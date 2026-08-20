package com.hilight.core;

import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.os.Binder;
import android.os.IBinder;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Thin wrapper over the hidden ILightsManager binder interface.
 *
 * Reflection rather than the public android.hardware.lights.LightsManager because that needs a
 * Context, and both callers here (the adb-launched helper and the Shizuku user service) are plain
 * processes. The binder route also exposes openSession(token, priority), which the public API does
 * not.
 *
 * Requires android.permission.CONTROL_DEVICE_LIGHTS, which is signature|privileged — this class only
 * works in a process running as the shell UID (2000) or root.
 */
public final class LightsBackend {

    private final IBinder token = new Binder();
    private Object service;
    private Method mGetLights, mOpenSession, mCloseSession, mSetLightStates;
    private int[] ids = new int[0];
    private boolean sessionOpen;
    private int sessionPriority = Integer.MIN_VALUE;

    public void connect() throws Exception {
        IBinder b = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "lights");
        if (b == null) throw new IllegalStateException("lights service missing");
        Class<?> iface = Class.forName("android.hardware.lights.ILightsManager");
        service = Class.forName("android.hardware.lights.ILightsManager$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, b);
        mGetLights = iface.getMethod("getLights");
        mOpenSession = iface.getMethod("openSession", IBinder.class, int.class);
        mCloseSession = iface.getMethod("closeSession", IBinder.class);
        mSetLightStates = iface.getMethod("setLightStates", IBinder.class, int[].class, LightState[].class);

        @SuppressWarnings("unchecked")
        List<Light> all = (List<Light>) mGetLights.invoke(service);
        int n = 0;
        for (Light l : all) if (l.getType() == Light.LIGHT_TYPE_APPLICATION) n++;
        ids = new int[n];
        int k = 0;
        for (Light l : all) if (l.getType() == Light.LIGHT_TYPE_APPLICATION) ids[k++] = l.getId();
        describe(all);
    }

    /**
     * Logs what the framework reports about every light it hands us.
     *
     * Worth the few lines: it is the first thing needed to tell "this device has no addressable
     * array" apart from "the renderer never got hold of it", which is otherwise guesswork from a bug
     * report, and it is the only place these capabilities are observable — `dumpsys lights` shows
     * ids and colours but nothing about control.
     */
    private void describe(List<Light> all) {
        for (Light l : all) {
            if (l.getType() != Light.LIGHT_TYPE_APPLICATION) continue;
            String period;
            try {
                period = l.getMinUpdatePeriodMillis() + "ms";
            } catch (Throwable t) {
                period = "unknown";
            }
            Log.i("light id=" + l.getId()
                    + " ordinal=" + l.getOrdinal()
                    + " type=" + l.getType()
                    + " rgb=" + l.hasRgbControl()
                    + " brightness=" + l.hasBrightnessControl()
                    + " animation=" + l.hasAnimationControl()
                    + " minUpdatePeriod=" + period);
        }
    }

    public int ledCount() { return ids.length; }

    public boolean isSessionOpen() { return sessionOpen; }

    public int sessionPriority() { return sessionPriority; }

    public void openSession(int priority) {
        try {
            mOpenSession.invoke(service, token, priority);
            sessionOpen = true;
            sessionPriority = priority;
        } catch (Exception e) {
            Log.w("openSession failed: " + e);
        }
    }

    public void closeSession() {
        if (!sessionOpen) return;
        try {
            mCloseSession.invoke(service, token);
        } catch (Exception e) {
            Log.w("closeSession failed: " + e);
        }
        sessionOpen = false;
    }

    /** Pushes one frame. [colors] is indexed per LED; a shorter array is repeated. */
    public void push(int[] colors) {
        if (!sessionOpen || ids.length == 0) return;
        LightState[] st = new LightState[ids.length];
        for (int i = 0; i < ids.length; i++) {
            st[i] = new LightState.Builder().setColor(colors[i % colors.length]).build();
        }
        try {
            mSetLightStates.invoke(service, token, ids, st);
        } catch (Exception e) {
            Log.w("setLightStates failed: " + e);
            // Tell the service the session is over before dropping the local flag. A transient
            // binder failure does not mean the far side closed anything, and simply forgetting the
            // session here left the caller reopening a token the service still held open.
            closeSession();
        }
    }
}

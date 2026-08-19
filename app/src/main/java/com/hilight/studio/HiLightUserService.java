package com.hilight.studio;

import com.hilight.core.Engine;
import com.hilight.core.IHiLightService;
import com.hilight.core.Log;

/**
 * Shizuku host: Shizuku starts this class in an app_process running as the shell UID (or root), so it
 * inherits CONTROL_DEVICE_LIGHTS and can drive HiLight directly.
 *
 * Unlike the adb host there is no file bridge — the app holds a binder to this object and pushes
 * state straight in.
 */
public final class HiLightUserService extends IHiLightService.Stub {

    private final Engine engine = new Engine();

    /** Shizuku instantiates the service with a no-arg constructor. */
    public HiLightUserService() {
        try {
            engine.start();
            Log.i("Shizuku user service up, " + engine.ledCount() + " LEDs, uid "
                    + android.os.Process.myUid());
        } catch (Throwable t) {
            Log.w("engine start failed: " + t);
        }
    }

    @Override
    public void setState(String json) {
        engine.setState(json);
    }

    @Override
    public String status() {
        return engine.status();
    }

    @Override
    public int ledCount() {
        return engine.ledCount();
    }

    @Override
    public void destroy() {
        Log.i("Shizuku user service going away");
        engine.stop();
        System.exit(0);
    }
}

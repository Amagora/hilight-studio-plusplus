package com.hilight.core;

import org.json.JSONObject;

/**
 * The render loop, shared by both privileged hosts.
 *
 * State is one JSON document: {enabled, priority, ambient:{...}, alert:{id, durationMs, ...}}.
 * The alert layer wins while it lasts; a durationMs of 0 holds until the alert is replaced or the
 * "alert" key disappears. Everything else falls through to ambient.
 *
 * Ticks at the hardware's minimum update period (33 ms, ~30 fps).
 *
 * Nothing here runs forever, and the protections live here rather than in the UI so that no bug or
 * hostile state document can bypass them:
 *
 * <ul>
 *   <li>the ambient look has a deadline ("ambientTimeoutMs", 30 s by default) and blanks itself when
 *       it passes; only a state document with "arm" set — a deliberate user action — starts a new
 *       window, so alerts and background pushes cannot extend it</li>
 *   <li>an alert with no duration is held only up to that same cap, and a finite alert is clamped to
 *       {@link #ALERT_MAX_MS}</li>
 *   <li>{@link #MAX_DUTY} caps how much of any {@link #DUTY_WINDOW_MS} window the array may be lit;
 *       past that it rests until the window rolls over, so repeated alerts cannot keep it on</li>
 *   <li>brightness tapers to {@link #TAPER_FLOOR} once the array has been continuously lit for
 *       {@link #TAPER_AFTER_MS}, which limits sustained current through the LEDs</li>
 * </ul>
 *
 * These figures are deliberately conservative: stock HiLight only flashes briefly, so there is no
 * published guidance on how long this array is meant to run.
 */
public final class Engine {

    public static final long FRAME_MS = SafetyGuard.FRAME_MS;

    /** Hard ceiling for a single alert, whatever the app asks for. */
    public static final long ALERT_MAX_MS = 60_000;
    public static final long DEFAULT_AMBIENT_TIMEOUT_MS = 30_000;

    /** Duty-cycle guard: at most half of any ten-minute window may be lit. */
    public static final long DUTY_WINDOW_MS = SafetyGuard.DUTY_WINDOW_MS;
    public static final double MAX_DUTY = SafetyGuard.MAX_DUTY;
    /** Sustained-current guard: taper brightness after this much unbroken light. */
    public static final long TAPER_AFTER_MS = SafetyGuard.TAPER_AFTER_MS;
    public static final long TAPER_RAMP_MS = SafetyGuard.TAPER_RAMP_MS;
    public static final double TAPER_FLOOR = SafetyGuard.TAPER_FLOOR;

    private final LightsBackend lights = new LightsBackend();
    private final Renderer renderer = new Renderer();
    private final SafetyGuard safety = new SafetyGuard();
    private final Object lock = new Object();

    private Thread thread;
    private volatile boolean running;

    private JSONObject state = new JSONObject();
    private JSONObject alert;
    private long alertId = -1;
    private long alertStart, alertEnd;

    private double dim = 1.0;
    private long ambientTimeoutMs = DEFAULT_AMBIENT_TIMEOUT_MS;
    private long ambientDeadline;
    private boolean ambientBlanked;

    public void start() throws Exception {
        lights.connect();
        Log.i("connected: " + lights.ledCount() + " HiLight LEDs");
        running = true;
        thread = new Thread(this::loop, "hilight-render");
        thread.setDaemon(false);
        thread.start();
    }

    public void stop() {
        running = false;
        synchronized (lock) {
            lights.push(new int[]{0});
            lights.closeSession();
        }
    }

    public int ledCount() { return lights.ledCount(); }

    /** Replaces the whole state document. Safe to call from any thread. */
    public void setState(String json) {
        JSONObject o;
        try {
            o = new JSONObject(json);
        } catch (Exception e) {
            Log.w("bad state json: " + e);
            return;
        }
        synchronized (lock) {
            state = o;
            ambientTimeoutMs = Math.max(1_000, o.optLong("ambientTimeoutMs", DEFAULT_AMBIENT_TIMEOUT_MS));
            dim = Math.max(0.02, Math.min(1.0, o.optDouble("dim", 1.0)));
            // Only a deliberate user action ("arm") may start a fresh window. Automatic pushes — an
            // alert firing, a foreground override, the app being backgrounded — must not, or the array
            // could be kept lit indefinitely in 30-second increments.
            if (o.optBoolean("arm", true)) armAmbient();
            JSONObject a = o.optJSONObject("alert");
            if (a == null) {
                if (alert != null) Log.i("alert cleared");
                alert = null;
                alertId = -1;
                renderer.reset();
            } else {
                long id = a.optLong("id", -1);
                if (id != alertId) {
                    alertId = id;
                    alert = a;
                    alertStart = System.currentTimeMillis();
                    long asked = a.optLong("durationMs", 4000);
                    // an open-ended alert (a "while this app is open" hold) still gets the global cap
                    long dur = asked <= 0 ? ambientTimeoutMs : Math.min(asked, ALERT_MAX_MS);
                    alertEnd = alertStart + dur;
                    renderer.reset();
                    Log.i("alert " + id + " " + a.optString("pattern", "pulse") + " for " + dur + "ms"
                            + (dur != asked ? " (asked " + asked + ", capped)" : ""));
                }
            }
        }
    }

    private void armAmbient() {
        ambientDeadline = System.currentTimeMillis() + ambientTimeoutMs;
        ambientBlanked = false;
    }

    public String status() {
        JSONObject o = new JSONObject();
        try {
            synchronized (lock) {
                o.put("pid", android.os.Process.myPid());
                o.put("uid", android.os.Process.myUid());
                o.put("ts", System.currentTimeMillis());
                o.put("ledCount", lights.ledCount());
                o.put("session", lights.isSessionOpen());
                o.put("priority", lights.sessionPriority());
                JSONObject amb = state.optJSONObject("ambient");
                o.put("mode", amb == null ? "off" : amb.optString("mode", "off"));
                o.put("alertId", alertId);
                o.put("timeoutMs", ambientTimeoutMs);
                o.put("dim", dim);
                o.put("ambientRemainingMs", Math.max(0, ambientDeadline - System.currentTimeMillis()));
                o.put("ambientHeld", ambientBlanked);
                o.put("resting", safety.isResting());
                o.put("dutyPct", safety.dutyPercent());
                o.put("version", 1);
            }
        } catch (Exception ignored) {
            // a status document is never worth crashing over
        }
        return o.toString();
    }

    private void loop() {
        while (running) {
            try {
                tick();
                Thread.sleep(FRAME_MS);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                Log.w("frame failed: " + t);
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private void tick() {
        synchronized (lock) {
            boolean enabled = state.optBoolean("enabled", false);
            int priority = state.optInt("priority", 0);

            if (!enabled) {
                if (lights.isSessionOpen()) {
                    lights.push(new int[]{0});          // blank before handing control back
                    lights.closeSession();
                    Log.i("released HiLight to the system");
                }
                return;
            }

            if (!lights.isSessionOpen() || priority != lights.sessionPriority()) {
                if (lights.isSessionOpen()) lights.closeSession();
                lights.openSession(priority);
            }

            long now = System.currentTimeMillis();
            JSONObject cfg;
            long t;
            if (alert != null && now < alertEnd) {
                cfg = alert;
                t = now - alertStart;
            } else {
                if (alert != null) {
                    alert = null;
                    renderer.reset();
                    // deliberately no re-arm here: an alert must not extend the ambient window
                }
                if (now > ambientDeadline) {
                    // the safety timeout: blank the array but keep the session, so app rules still work
                    if (!ambientBlanked) {
                        ambientBlanked = true;
                        lights.push(new int[]{0});
                        Log.i("ambient timed out after " + ambientTimeoutMs + "ms — array blanked");
                    }
                    return;
                }
                cfg = state.optJSONObject("ambient");
                t = now;
            }
            int[] frame = renderer.frame(cfg, t, Math.max(1, lights.ledCount()));
            lights.push(protect(frame, now));
        }
    }

    /**
     * Applies the hardware protections to a frame: rests the array when it has been lit for too much
     * of the current window, and tapers brightness under sustained light.
     */
    private int[] protect(int[] frame, long now) {
        return safety.apply(frame, now, dim);
    }
}

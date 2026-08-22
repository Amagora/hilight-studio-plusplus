package com.hilight.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The render loop, shared by all privileged hosts.
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
    private final OutputGate gate = new OutputGate();
    private final Object lock = new Object();
    private final PrivacyScheduler privacyScheduler = new PrivacyScheduler();
    private final Map<String, JSONObject> privacyConfigs = new HashMap<>();
    private final AppOpsWatcher privacyWatcher;

    private Thread thread;
    private volatile boolean running;

    private JSONObject state = new JSONObject();
    private JSONObject alert;
    private long alertId = -1;
    private boolean lastFrameWasAlert;
    private boolean renderingPrivacy;
    private String renderedPrivacyRule;
    private PrivacyScheduler.Phase privacyPhase = PrivacyScheduler.Phase.INACTIVE;
    private long appliedStateRevision;

    private double dim = 1.0;
    private long ambientTimeoutMs = DEFAULT_AMBIENT_TIMEOUT_MS;

    public Engine() {
        privacyWatcher = new AppOpsWatcher(active -> {
            synchronized (lock) {
                privacyScheduler.updateActive(active, android.os.SystemClock.elapsedRealtime());
            }
        });
    }

    public void start() throws Exception {
        lights.connect();
        Log.i("connected: " + lights.ledCount() + " HiLight LEDs");
        running = true;
        thread = new Thread(this::loop, "hilight-render");
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Blanks the array and hands it back.
     *
     * Clearing `running` inside the lock matters: outside it, a tick that had already passed the
     * `while (running)` check could run after the session was closed, reopen it and push a live
     * frame, leaving the array lit by the very call that was meant to darken it.
     */
    public void stop() {
        synchronized (lock) {
            running = false;
            privacyWatcher.stop();
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
            readPrivacyRules(o.optJSONArray("privacyRules"));
            if (o.optBoolean("privacyObserverEnabled", false)) {
                privacyWatcher.start();
            } else {
                privacyWatcher.stop();
                privacyScheduler.clearActive();
                privacyPhase = PrivacyScheduler.Phase.INACTIVE;
                renderingPrivacy = false;
                renderedPrivacyRule = null;
            }
            ambientTimeoutMs = Math.max(1_000, o.optLong("ambientTimeoutMs", DEFAULT_AMBIENT_TIMEOUT_MS));
            dim = Math.max(0.02, Math.min(1.0, o.optDouble("dim", 1.0)));
            // Only a deliberate user action ("arm") may start a fresh window. Automatic pushes — an
            // alert firing, a foreground override, the app being backgrounded — must not, or the array
            // could be kept lit indefinitely in 30-second increments.
            //
            // Defaulting to false matters: a document that omits the key must not arm. The app always
            // sends it, but the bootstrap file the app drops for a not-yet-running helper is just
            // {"enabled":false}, and defaulting to true let that open a window nobody asked for.
            if (o.optBoolean("arm", false)) gate.armAmbient(System.currentTimeMillis(), ambientTimeoutMs);
            JSONObject a = o.optJSONObject("alert");
            if (a == null) {
                if (alert != null) Log.i("alert cleared");
                alert = null;
                alertId = -1;
                // clears the blank latch too, so a cancelled alert cannot leave the array lit
                gate.clearAlert();
                renderer.reset();
            } else {
                long id = a.optLong("id", -1);
                if (id != alertId) {
                    alertId = id;
                    alert = a;
                    long asked = a.optLong("durationMs", 4000);
                    // an open-ended alert (a "while this app is open" hold) still gets the global cap
                    long dur = asked <= 0 ? ambientTimeoutMs : Math.min(asked, ALERT_MAX_MS);
                    gate.startAlert(System.currentTimeMillis(), dur);
                    renderer.reset();
                    Log.i("alert " + id + " " + a.optString("pattern", "pulse") + " for " + dur + "ms"
                            + (dur != asked ? " (asked " + asked + ", capped)" : ""));
                }
            }
            appliedStateRevision = o.optLong("stateRevision", appliedStateRevision);
        }
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
                o.put("ambientRemainingMs", gate.ambientRemainingMs(System.currentTimeMillis()));
                o.put("ambientHeld", gate.isAmbientHeld());
                o.put("resting", safety.isResting());
                o.put("dutyPct", safety.dutyPercent());
                o.put("appliedStateRevision", appliedStateRevision);
                o.put("privacyObserverEnabled", state.optBoolean("privacyObserverEnabled", false));
                o.put("privacyObserverState", privacyWatcher.state().name().toLowerCase());
                o.put("privacyPhase", privacyPhase.name().toLowerCase());
                o.put("version", 2);
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
            if (!running) return;                   // stop() may have closed the session already
            boolean enabled = state.optBoolean("enabled", false);
            boolean privacyOutputEnabled = state.optBoolean("privacyOutputEnabled", false);
            int priority = state.optInt("priority", 0);

            long now = System.currentTimeMillis();
            OutputGate.Layer layer = gate.next(now);
            PrivacyScheduler.Decision privacy =
                    privacyScheduler.decision(android.os.SystemClock.elapsedRealtime());
            privacyPhase = privacy.phase;

            boolean privacyOwnsOutput = privacyOutputEnabled &&
                    (privacy.phase == PrivacyScheduler.Phase.LIT ||
                            privacy.phase == PrivacyScheduler.Phase.COOLDOWN);
            if (!enabled && !privacyOwnsOutput) {
                leavePrivacyRenderer();
                release("released HiLight to the system");
                noteDark(now);
                return;
            }

            if (lastFrameWasAlert && layer != OutputGate.Layer.ALERT) {
                alert = null;
                renderer.reset();
                // deliberately no re-arm here: an alert must not extend the ambient window
            }
            lastFrameWasAlert = layer == OutputGate.Layer.ALERT;

            // Existing state documents had no source field, so they retain the old alert-first
            // behavior. New foreground holds identify themselves and yield to privacy activity.
            boolean finiteAlert = layer == OutputGate.Layer.ALERT &&
                    !"foreground".equals(alert == null ? "" : alert.optString("source", "legacy"));

            if (!finiteAlert && privacyOutputEnabled &&
                    privacy.phase == PrivacyScheduler.Phase.COOLDOWN) {
                renderingPrivacy = false;
                renderedPrivacyRule = null;
                renderer.reset();
                blankAndRelease(now, "privacy cooldown — released HiLight to the system");
                return;
            }

            JSONObject cfg;
            long t;
            if (!finiteAlert && privacyOutputEnabled && privacy.phase == PrivacyScheduler.Phase.LIT) {
                cfg = privacyConfigs.get(privacy.ruleId);
                if (cfg == null) {
                    renderingPrivacy = false;
                    renderedPrivacyRule = null;
                    return;
                }
                if (!renderingPrivacy || !privacy.ruleId.equals(renderedPrivacyRule)) renderer.reset();
                renderingPrivacy = true;
                renderedPrivacyRule = privacy.ruleId;
                t = privacy.phaseElapsedMs;
            } else switch (layer) {
                case ALERT:
                    leavePrivacyRenderer();
                    cfg = alert;
                    t = gate.alertElapsed(now);
                    break;
                case AMBIENT:
                    leavePrivacyRenderer();
                    cfg = state.optJSONObject("ambient");
                    t = now;
                    break;
                case BLANK:
                    // Blank, then let go. Holding an all-black session would keep winning over the
                    // system's own HiLight effects, so calls and Gemini would stay dark for as long
                    // as this process lived — and it outlives the app, so only a reboot fixed it.
                    leavePrivacyRenderer();
                    blankAndRelease(now, "nothing left to show — released HiLight to the system");
                    return;
                default:                                    // IDLE: dark, and already handed back
                    leavePrivacyRenderer();
                    noteDark(now);
                    return;
            }
            // The session is taken only while there is something to show, and reopened on demand: a
            // rule firing lands here and gets it back before the first frame.
            if (!lights.isSessionOpen() || priority != lights.sessionPriority()) {
                if (lights.isSessionOpen()) lights.closeSession();
                lights.openSession(priority);
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

    /**
     * Tells the guard the array is dark on a frame that is not pushed.
     *
     * The sustained-light taper only unwinds when the guard is handed a dark frame, and the frames
     * skipped while the array is already blank never reached it. Without this, a long ambient run
     * left the taper pinned, so the next look came back at the taper floor no matter how long the
     * array had actually been resting.
     */
    private void noteDark(long now) {
        safety.apply(BLANK, now, dim);
    }

    /** Hands the array back to Android, if we are holding it. */
    private void release(String why) {
        if (!lights.isSessionOpen()) return;
        lights.closeSession();
        Log.i(why);
    }

    private void leavePrivacyRenderer() {
        if (renderingPrivacy) renderer.reset();
        renderingPrivacy = false;
        renderedPrivacyRule = null;
    }

    private void blankAndRelease(long now, String why) {
        if (lights.isSessionOpen()) lights.push(protect(BLANK, now));
        release(why);
        noteDark(now);
    }

    private void readPrivacyRules(JSONArray array) {
        List<PrivacyScheduler.Rule> rules = new ArrayList<>();
        privacyConfigs.clear();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject cfg = array.optJSONObject(i);
                if (cfg == null) continue;
                String id = cfg.optString("id", "");
                PrivacyScheduler.Activity activity = privacyActivity(cfg.optString("activity", ""));
                String pkg = cfg.optString("pkg", PrivacyScheduler.ANY_APP);
                if (id.isEmpty() || activity == null) continue;
                long lightMs = Math.max(1_000, Math.min(60_000, cfg.optLong("lightMs", 10_000)));
                long cooldownMs = Math.max(1_000, Math.min(60_000, cfg.optLong("cooldownMs", 10_000)));
                rules.add(new PrivacyScheduler.Rule(id, activity, pkg, lightMs, cooldownMs));
                privacyConfigs.put(id, cfg);
            }
        }
        privacyScheduler.setRules(rules);
    }

    private static PrivacyScheduler.Activity privacyActivity(String key) {
        if ("microphone".equals(key)) return PrivacyScheduler.Activity.MICROPHONE;
        if ("camera".equals(key)) return PrivacyScheduler.Activity.CAMERA;
        return null;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static final int[] BLANK = {0};
}

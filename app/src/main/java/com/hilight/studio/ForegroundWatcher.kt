package com.hilight.studio

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Applies FOREGROUND rules (holding looks while an app is active) and tracks phone position
 * (face-down detection) via low-power sensor monitoring.
 * Also maintains the active background service status notification when enabled.
 */
class ForegroundWatcher : Service() {

    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var faceDownTracker: FaceDownSensorTracker
    private val main = Handler(Looper.getMainLooper())
    private val store by lazy { Store.get(this) }
    private var lastPkg: String? = null
    private val foreground = ForegroundAppTracker()
    @Volatile private var forceRefresh = true
    private var plan = ForegroundWatchPlan(trackForegroundApps = false, trackFaceDown = false)

    /**
     * Set on the main thread when the service is going away.
     *
     * The poll below queries UsageStats over binder, so a tick can still be in flight when the
     * service is destroyed — `removeCallbacksAndMessages` cannot recall one that already started.
     * Checking this on the main thread, where the override is also cleared, keeps a late tick from
     * re-applying an override with no watcher left alive to ever clear it.
     */
    private var stopped = false

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    forceRefresh = true
                    if (plan.trackForegroundApps) {
                        handler.removeCallbacks(tick)
                        handler.post(tick)
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    handler.removeCallbacks(tick)
                    lastPkg = null
                    main.post {
                        if (!stopped) {
                            store.setForegroundOverride(null, null)
                        }
                    }
                }
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (isPackageSuspended() || !store.enabled.value || !plan.trackForegroundApps) {
                main.post {
                    if (!stopped) {
                        store.setForegroundOverride(null, null)
                    }
                }
                if (!plan.shouldRun) {
                    runCatching {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
                    }
                    stopSelf()
                }
                return
            }

            val pm = getSystemService(android.os.PowerManager::class.java)
            val km = getSystemService(android.app.KeyguardManager::class.java)
            val isInteractive = pm?.isInteractive ?: true
            val isLocked = km?.isKeyguardLocked ?: false

            if (!isInteractive || isLocked) {
                if (lastPkg != null) {
                    lastPkg = null
                    main.post {
                        if (!stopped) {
                            store.setForegroundOverride(null, null)
                        }
                    }
                }
                // Screen is off or locked — do not schedule further polls until screen wakes up
                return
            }

            val pkg = currentForegroundPackage()
            val activePkg = pkg?.takeIf { !store.isPackagePausedOrStopped(it) }
            if (forceRefresh || activePkg != lastPkg) {
                forceRefresh = false
                lastPkg = activePkg
                // Store is a main-thread object: every other caller mutates it from there, and its
                // override field and file writes are not synchronized.
                main.post {
                    if (!stopped) {
                        val rule = activePkg?.let { store.ruleFor(it, Trigger.FOREGROUND) }
                        store.setForegroundOverride(activePkg?.takeIf { rule != null }, rule)
                    }
                }
            }
            if (plan.trackForegroundApps && store.enabled.value) handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        thread = HandlerThread("fg-watch").also { it.start() }
        handler = Handler(thread.looper)
        faceDownTracker = FaceDownSensorTracker(this, handler) { state, sampleElapsedMs ->
            main.post {
                if (!stopped) store.updateFaceDownSensorState(state, sampleElapsedMs)
            }
        }
        val initialPlan = store.foregroundWatchPlan()
        plan = initialPlan
        startForeground(NOTIF_ID, notification(initialPlan))
        runCatching {
            stopForeground(STOP_FOREGROUND_REMOVE)
            getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onDestroy() {
        stopped = true
        faceDownTracker.stop()
        runCatching { unregisterReceiver(screenReceiver) }
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
        runCatching {
            stopForeground(STOP_FOREGROUND_REMOVE)
            getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
        }
        main.post {
            store.setForegroundOverride(null, null)
            store.updateFaceDownSensorState(FaceDownState.INACTIVE, 0L)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isPackageSuspended()) {
            runCatching {
                stopForeground(STOP_FOREGROUND_REMOVE)
                getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        val nextPlan = intent?.let(::planFromIntent) ?: store.foregroundWatchPlan()
        if (!nextPlan.shouldRun) {
            runCatching {
                stopForeground(STOP_FOREGROUND_REMOVE)
                getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        applyPlan(nextPlan)
        return START_NOT_STICKY
    }

    private fun applyPlan(next: ForegroundWatchPlan) {
        val prior = plan
        plan = next
        startForeground(NOTIF_ID, notification(next))
        runCatching {
            stopForeground(STOP_FOREGROUND_REMOVE)
            getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
        }

        forceRefresh = true
        handler.removeCallbacks(tick)
        if (next.trackForegroundApps && store.enabled.value) {
            handler.post(tick)
        } else if (prior.trackForegroundApps) {
            foreground.clear()
            lastPkg = null
            lastEventTimeMs = Long.MIN_VALUE
            main.post { if (!stopped) store.setForegroundOverride(null, null) }
        }

        if (prior.trackFaceDown != next.trackFaceDown) {
            handler.post {
                if (stopped) return@post
                if (plan.trackFaceDown && store.enabled.value) {
                    faceDownTracker.start()
                    if (stopped) faceDownTracker.stop()
                } else {
                    faceDownTracker.stop()
                    main.post {
                        if (!stopped) {
                            store.updateFaceDownSensorState(FaceDownState.INACTIVE, 0L)
                        }
                    }
                }
            }
        }
    }

    private fun isPackageSuspended(): Boolean {
        return runCatching {
            packageManager.isPackageSuspended
        }.getOrDefault(false)
    }

    private var lastEventTimeMs = Long.MIN_VALUE

    private fun currentForegroundPackage(): String? {
        val pm = getSystemService(android.os.PowerManager::class.java)
        val km = getSystemService(android.app.KeyguardManager::class.java)
        val isInteractive = pm?.isInteractive ?: true
        val isLocked = km?.isKeyguardLocked ?: false
        if (!isInteractive || isLocked) {
            return null
        }

        if (!hasUsageAccess(this)) {
            foreground.clear()
            lastEventTimeMs = Long.MIN_VALUE
            return null
        }
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val bootWallTime = now - SystemClock.elapsedRealtime()
        val begin = if (lastEventTimeMs == Long.MIN_VALUE) {
            maxOf(bootWallTime, now - BOOTSTRAP_LOOKBACK_MS)
        } else {
            maxOf(bootWallTime, minOf(now, lastEventTimeMs + 1))
        }
        val events = usm.queryEvents(begin, now)
        val e = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.timeStamp > lastEventTimeMs) {
                lastEventTimeMs = e.timeStamp
            }
            when (e.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ->
                    foreground.accept(e.packageName, e.className, ForegroundLifecycle.RESUMED)
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED ->
                    foreground.accept(e.packageName, e.className, ForegroundLifecycle.PAUSED)
                android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED ->
                    foreground.accept(e.packageName, e.className, ForegroundLifecycle.STOPPED)
                android.app.usage.UsageEvents.Event.KEYGUARD_SHOWN,
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                    foreground.clear()
            }
        }
        return foreground.currentPackage()
    }

    private fun notification(currentPlan: ForegroundWatchPlan): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.service_watcher_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val text = when {
            currentPlan.trackForegroundApps && currentPlan.trackFaceDown ->
                getString(R.string.service_watcher_text_both)
            currentPlan.trackFaceDown ->
                getString(R.string.service_watcher_text_face_down)
            else ->
                getString(R.string.service_watcher_text)
        }

        return Notification.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.service_watcher_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.hilight_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .build()
    }

    companion object {
        const val EXTRA_PERSISTENT_NOTIF = "extra_persistent_notif"
        const val EXTRA_FOREGROUND = "trackForegroundApps"
        const val EXTRA_FACE_DOWN = "trackFaceDown"
        private const val NOTIF_ID = 1
        private const val CHANNEL = "fg_watch"
        private const val POLL_MS = 1000L
        private const val QUERY_OVERLAP_MS = 2_000L
        private const val BOOTSTRAP_LOOKBACK_MS = 24 * 60 * 60_000L

        /** Starts or stops the watcher to match one authoritative work plan. */
        fun syncRunning(ctx: Context, plan: ForegroundWatchPlan) {
            val intent = Intent(ctx, ForegroundWatcher::class.java)
                .putExtra(EXTRA_FOREGROUND, plan.trackForegroundApps)
                .putExtra(EXTRA_FACE_DOWN, plan.trackFaceDown)
            runCatching {
                if (plan.shouldRun) ctx.startForegroundService(intent) else ctx.stopService(intent)
            }.onFailure { Log.w("HiLightForeground", "could not update foreground watcher", it) }
        }

        fun hasFaceDownSensor(ctx: Context): Boolean = FaceDownSensorTracker.hasSensor(ctx)

        fun hasUsageAccess(ctx: Context): Boolean {
            val appOps = ctx.getSystemService(AppOpsManager::class.java) ?: return false
            return appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }

        private fun planFromIntent(intent: Intent): ForegroundWatchPlan = ForegroundWatchPlan(
            trackForegroundApps = intent.getBooleanExtra(EXTRA_FOREGROUND, false),
            trackFaceDown = intent.getBooleanExtra(EXTRA_FACE_DOWN, false),
        )
    }
}

package com.hilight.studio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AppOpsManager
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
 * Applies FOREGROUND rules: while a chosen app is on screen, HiLight holds that app's look.
 *
 * Uses UsageStatsManager event queries (needs Usage access, which the user grants in Settings)
 * because no non-privileged API reports the foreground package directly.
 */
class ForegroundWatcher : Service() {

    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private val main = Handler(Looper.getMainLooper())
    private val store by lazy { Store.get(this) }
    private var lastPkg: String? = null
    private val foreground = ForegroundAppTracker()
    @Volatile private var forceRefresh = true

    /**
     * Set on the main thread when the service is going away.
     *
     * The poll below queries UsageStats over binder, so a tick can still be in flight when the
     * service is destroyed — `removeCallbacksAndMessages` cannot recall one that already started.
     * Checking this on the main thread, where the override is also cleared, keeps a late tick from
     * re-applying an override with no watcher left alive to ever clear it.
     */
    private var stopped = false

    private val tick = object : Runnable {
        override fun run() {
            val pkg = currentForegroundPackage()
            if (forceRefresh || pkg != lastPkg) {
                forceRefresh = false
                lastPkg = pkg
                // Store is a main-thread object: every other caller mutates it from there, and its
                // override field and file writes are not synchronized.
                main.post {
                    if (!stopped) {
                        val rule = pkg?.let { store.ruleFor(it, Trigger.FOREGROUND) }
                        store.setForegroundOverride(pkg?.takeIf { rule != null }, rule)
                    }
                }
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        thread = HandlerThread("fg-watch").also { it.start() }
        handler = Handler(thread.looper)
        startForeground(1, notification())
        handler.post(tick)
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
        main.post { store.setForegroundOverride(null, null) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // syncRunning also calls start when the service already exists. Re-evaluate the same package
        // so enabling or editing its rule cannot be ignored just because the app did not change.
        forceRefresh = true
        return START_STICKY
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



    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        // A Service has no composition, so getString rather than stringResource. CHANNEL is the id
        // the notification is registered under and is never read by a person; the name beside it is
        // the label shown in the system notification settings, so that one is translated.
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.service_watcher_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            )
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.service_watcher_title))
            .setContentText(getString(R.string.service_watcher_text))
            .setSmallIcon(R.drawable.hilight_logo)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "fg_watch"
        private const val POLL_MS = 1000L
        private const val QUERY_OVERLAP_MS = 2_000L
        private const val BOOTSTRAP_LOOKBACK_MS = 24 * 60 * 60_000L

        /** Starts or stops the watcher to match the current rule set. */
        fun syncRunning(ctx: Context, rules: List<AppRule>, enabled: Boolean) {
            val needed = ForegroundWatchPolicy.shouldRun(enabled, rules)
            val intent = Intent(ctx, ForegroundWatcher::class.java)
            runCatching {
                if (needed) ctx.startForegroundService(intent) else ctx.stopService(intent)
            }.onFailure { Log.w("HiLightForeground", "could not update foreground watcher", it) }
        }

        fun hasUsageAccess(ctx: Context): Boolean {
            val appOps = ctx.getSystemService(AppOpsManager::class.java) ?: return false
            return appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }
    }
}

package com.hilight.studio

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for the UI and the triggers, and the only thing that pushes to a [Backend].
 *
 * Output layering, highest priority first:
 *   1. a finite notification alert
 *   2. an infinite foreground-app override
 *   3. the ambient look
 * When a notification alert finishes, any foreground override is re-pushed so it is not lost.
 */
class Store private constructor(private val app: Context) {

    private val prefs = app.getSharedPreferences("hilight", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())

    private val adb = AdbBackend(app)
    val shizuku = ShizukuBackend(app)

    private val _transport = MutableStateFlow(
        runCatching { Transport.valueOf(prefs.getString("transport", null) ?: "AUTO") }
            .getOrDefault(Transport.AUTO)
    )
    val transport: StateFlow<Transport> = _transport.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("dynamicColor", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _ambientTimeoutMs =
        MutableStateFlow(prefs.getInt("ambientTimeoutMs", Limits.AMBIENT_DEFAULT_MS))
    val ambientTimeoutMs: StateFlow<Int> = _ambientTimeoutMs.asStateFlow()

    private val _quietEnabled = MutableStateFlow(prefs.getBoolean("quietEnabled", false))
    val quietEnabled: StateFlow<Boolean> = _quietEnabled.asStateFlow()

    /** minutes since midnight */
    private val _quietStart = MutableStateFlow(prefs.getInt("quietStart", 23 * 60))
    val quietStart: StateFlow<Int> = _quietStart.asStateFlow()

    private val _quietEnd = MutableStateFlow(prefs.getInt("quietEnd", 7 * 60))
    val quietEnd: StateFlow<Int> = _quietEnd.asStateFlow()

    /** Dim through quiet hours instead of going fully dark. */
    private val _quietDim = MutableStateFlow(prefs.getBoolean("quietDim", false))
    val quietDim: StateFlow<Boolean> = _quietDim.asStateFlow()

    private val _quietDimPct = MutableStateFlow(prefs.getInt("quietDimPct", 12))
    val quietDimPct: StateFlow<Int> = _quietDimPct.asStateFlow()

    private val _screenOffOnly = MutableStateFlow(prefs.getBoolean("screenOffOnly", false))
    val screenOffOnly: StateFlow<Boolean> = _screenOffOnly.asStateFlow()

    private val _batteryGuard = MutableStateFlow(prefs.getBoolean("batteryGuard", true))
    val batteryGuard: StateFlow<Boolean> = _batteryGuard.asStateFlow()

    private val _batteryMinPct = MutableStateFlow(prefs.getInt("batteryMinPct", 20))
    val batteryMinPct: StateFlow<Int> = _batteryMinPct.asStateFlow()

    private val _respectDnd = MutableStateFlow(prefs.getBoolean("respectDnd", true))
    val respectDnd: StateFlow<Boolean> = _respectDnd.asStateFlow()

    private val _suppression = MutableStateFlow<Suppression?>(null)
    val suppression: StateFlow<Suppression?> = _suppression.asStateFlow()

    private val _priority = MutableStateFlow(prefs.getInt("priority", 0))
    val priority: StateFlow<Int> = _priority.asStateFlow()

    private val _ambient = MutableStateFlow(loadAmbient())
    val ambient: StateFlow<Ambient> = _ambient.asStateFlow()

    private val _presets = MutableStateFlow(loadPresets())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val _rules = MutableStateFlow(loadRules())
    val rules: StateFlow<List<AppRule>> = _rules.asStateFlow()

    private val _status = MutableStateFlow(HelperStatus(alive = false))
    val status: StateFlow<HelperStatus> = _status.asStateFlow()

    /** Which transport is actually carrying state right now. */
    private val _activeTransport = MutableStateFlow(Transport.ADB)
    val activeTransport: StateFlow<Transport> = _activeTransport.asStateFlow()

    /** Package of the app whose FOREGROUND rule is currently held, if any. */
    private var foregroundOverride: Pair<String, JSONObject>? = null

    /** True while a notification alert is still meant to be on the array. */
    private var alertHeld = false
    private var alertExpiry: Runnable? = null

    init {
        Bridge.ensureFiles(app)
        _suppression.value = suppressionNow()
        // cheap clock/battery watch: quiet hours start and battery drops must take effect on their own
        main.post(object : Runnable {
            override fun run() {
                refreshSuppression()
                main.postDelayed(this, 30_000)
            }
        })
        // screen state changes too fast for the 30s tick to be the only watcher
        app.registerReceiver(
            object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    if (i?.action == Intent.ACTION_SCREEN_OFF) {
                        refreshSuppression(armOnRelease = true)
                        return
                    }
                    // The screen coming on, or the phone being unlocked, means the notification has
                    // been seen — a rule's colour has no one left to tell, so drop it now instead of
                    // burning the rest of its window.
                    cancelAlert()
                    refreshSuppression()
                }
            },
            android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )
        // Whenever Shizuku appears or disappears, re-push: a new user service starts stateless, and
        // after a loss the ADB helper needs to be told to take over.
        shizuku.onAvailabilityChanged = {
            main.post {
                pushCurrent(arm = false)
                HiLightTile.refresh(app)
            }
        }
    }

    // ------------------------------------------------------------------ transport selection

    private fun backend(): Backend = when (_transport.value) {
        Transport.SHIZUKU -> shizuku
        Transport.ADB -> adb
        Transport.AUTO ->
            if (shizuku.state.value == ShizukuBackend.State.CONNECTED) shizuku else adb
    }

    fun setTransport(t: Transport) {
        _transport.value = t
        prefs.edit().putString("transport", t.name).apply()
        if (t == Transport.SHIZUKU || t == Transport.AUTO) shizuku.refresh()
        pushCurrent()
    }

    // ------------------------------------------------------------------ mutations

    fun setEnabled(v: Boolean) {
        _enabled.value = v
        prefs.edit().putBoolean("enabled", v).apply()
        pushCurrent()
        HiLightTile.refresh(app)
    }

    fun setDynamicColor(v: Boolean) {
        _dynamicColor.value = v
        prefs.edit().putBoolean("dynamicColor", v).apply()
    }

    fun setAmbientTimeoutMs(v: Int) {
        _ambientTimeoutMs.value = v.coerceIn(5_000, Limits.AMBIENT_MAX_MS)
        prefs.edit().putInt("ambientTimeoutMs", _ambientTimeoutMs.value).apply()
        pushCurrent()
    }

    fun setQuietHours(enabled: Boolean, startMin: Int = _quietStart.value, endMin: Int = _quietEnd.value) {
        _quietEnabled.value = enabled
        _quietStart.value = startMin
        _quietEnd.value = endMin
        prefs.edit()
            .putBoolean("quietEnabled", enabled)
            .putInt("quietStart", startMin)
            .putInt("quietEnd", endMin)
            .apply()
        pushCurrent()
    }

    fun setQuietDim(dim: Boolean, pct: Int = _quietDimPct.value) {
        _quietDim.value = dim
        _quietDimPct.value = pct.coerceIn(2, 40)
        prefs.edit()
            .putBoolean("quietDim", dim)
            .putInt("quietDimPct", _quietDimPct.value)
            .apply()
        pushCurrent()
    }

    fun setScreenOffOnly(v: Boolean) {
        _screenOffOnly.value = v
        prefs.edit().putBoolean("screenOffOnly", v).apply()
        pushCurrent()
    }

    fun setBatteryGuard(enabled: Boolean, minPct: Int = _batteryMinPct.value) {
        _batteryGuard.value = enabled
        _batteryMinPct.value = minPct.coerceIn(5, 50)
        prefs.edit()
            .putBoolean("batteryGuard", enabled)
            .putInt("batteryMinPct", _batteryMinPct.value)
            .apply()
        pushCurrent()
    }

    fun setRespectDnd(v: Boolean) {
        _respectDnd.value = v
        prefs.edit().putBoolean("respectDnd", v).apply()
    }

    fun setPriority(v: Int) {
        _priority.value = v
        prefs.edit().putInt("priority", v).apply()
        pushCurrent()
    }

    fun setAmbient(a: Ambient) {
        _ambient.value = a
        prefs.edit().putString("ambient", a.toPrefsJson().toString()).apply()
        pushCurrent()
    }

    fun upsertRule(rule: AppRule) {
        _rules.value = _rules.value.filterNot { it.pkg == rule.pkg && it.trigger == rule.trigger } + rule
        saveRules()
        ForegroundWatcher.syncRunning(app, _rules.value, _enabled.value)
    }

    fun removeRule(rule: AppRule) {
        _rules.value = _rules.value.filterNot { it.pkg == rule.pkg && it.trigger == rule.trigger }
        saveRules()
        ForegroundWatcher.syncRunning(app, _rules.value, _enabled.value)
    }

    fun savePreset(name: String) {
        val clean = name.trim().ifEmpty { "Preset ${_presets.value.size + 1}" }
        _presets.value = _presets.value.filterNot { it.name == clean } + Preset(clean, _ambient.value)
        persistPresets()
    }

    fun applyPreset(preset: Preset) = setAmbient(preset.ambient)

    fun deletePreset(preset: Preset) {
        _presets.value = _presets.value.filterNot { it.name == preset.name }
        persistPresets()
    }

    /** All presets as a JSON document, for sharing or backup. */
    fun exportPresets(): String = JSONObject().apply {
        put("v", 1)
        put("presets", JSONArray().also { a -> _presets.value.forEach { a.put(it.toJson()) } })
    }.toString(2)

    /** Merges presets from an exported document. Returns how many were added, or null if unreadable. */
    fun importPresets(raw: String): Int? = runCatching {
        val arr = JSONObject(raw).getJSONArray("presets")
        val incoming = (0 until arr.length()).mapNotNull {
            runCatching { Preset.fromJson(arr.getJSONObject(it)) }.getOrNull()
        }
        val byName = _presets.value.associateBy { it.name }.toMutableMap()
        incoming.forEach { byName[it.name] = it }
        _presets.value = byName.values.sortedBy { it.name.lowercase() }
        persistPresets()
        incoming.size
    }.getOrNull()

    private fun persistPresets() {
        val a = JSONArray()
        _presets.value.forEach { a.put(it.toJson()) }
        prefs.edit().putString("presets", a.toString()).apply()
    }

    private fun loadPresets(): List<Preset> =
        prefs.getString("presets", null)?.let { raw ->
            runCatching {
                val a = JSONArray(raw)
                (0 until a.length()).mapNotNull {
                    runCatching { Preset.fromJson(a.getJSONObject(it)) }.getOrNull()
                }
            }.getOrNull()
        } ?: emptyList()

    /** A rule for this package if there is one, otherwise the catch-all. */
    fun ruleFor(pkg: String, trigger: Trigger): AppRule? {
        val enabled = _rules.value.filter { it.enabled && it.trigger == trigger }
        return enabled.firstOrNull { it.pkg == pkg }
            ?: enabled.firstOrNull { it.isCatchAll }
    }

    // ------------------------------------------------------------------ light output

    /** Ambient (plus any held foreground override) — no transient alert. */
    fun pushCurrent(arm: Boolean = true) = send(_enabled.value, foregroundOverride?.second, arm)

    /** Fires a one-shot alert, then falls back to the override/ambient layer. */
    fun fireAlert(rule: AppRule) {
        if (!_enabled.value) return
        // screen-off-only must not block an alert that arrives while the screen happens to be on if
        // the user did not ask for that; only real suppressions block
        if (suppressionNow() != null) return
        val color = if (rule.randomColor) randomColor() else rule.color
        send(
            true,
            Bridge.alertJson(
                id = Bridge.nextAlertId(),
                pattern = rule.pattern,
                color = color,
                durationMs = rule.durationMs,
                speedMs = rule.speedMs,
                brightness = rule.brightness,
            ),
            arm = false,               // a notification must not extend the ambient window
        )
        // The renderer self-expires the alert, but the app tracks the window too: an unlock has to be
        // able to cut it short, and a held foreground override has to be restored underneath it.
        alertHeld = true
        alertExpiry?.let { main.removeCallbacks(it) }
        val r = Runnable {
            alertHeld = false
            alertExpiry = null
            pushCurrent(arm = false)
        }
        alertExpiry = r
        main.postDelayed(r, rule.durationMs.toLong() + 150)
    }

    /**
     * Drops a notification alert that is still running, restoring whatever sits underneath it.
     *
     * Called when the user turns the screen on or unlocks: the alert exists to be noticed, so once it
     * has been there is nothing to keep lit. No-op when no alert is in flight.
     */
    fun cancelAlert() {
        alertExpiry?.let { main.removeCallbacks(it) }
        alertExpiry = null
        if (!alertHeld) return
        alertHeld = false
        pushCurrent(arm = false)       // seeing a notification must not extend the ambient window
    }

    /** Holds a look for as long as [pkg] is in the foreground. */
    fun setForegroundOverride(pkg: String?, rule: AppRule?) {
        if (pkg == null || rule == null) {
            if (foregroundOverride == null) return
            foregroundOverride = null
            pushCurrent(arm = false)
            return
        }
        if (foregroundOverride?.first == pkg) return
        val color = if (rule.randomColor) randomColor() else rule.color
        foregroundOverride = pkg to Bridge.alertJson(
            id = Bridge.nextAlertId(),
            pattern = rule.pattern,
            color = color,
            durationMs = 0,                 // hold until cleared
            speedMs = rule.speedMs,
            brightness = rule.brightness,
        )
        pushCurrent(arm = false)       // opening an app must not extend the ambient window either
    }

    /** True while a Test-button preview is on the array. */
    private var previewAlertId: Long? = null

    /** The look currently being tested, so the hero can show it instead of the ambient look. */
    private val _previewLook = MutableStateFlow<Ambient?>(null)
    val previewLook: StateFlow<Ambient?> = _previewLook.asStateFlow()
    private var previewClear: Runnable? = null

    /** One-off preview used by the Test buttons. */
    fun preview(pattern: Pattern, color: Int, speedMs: Int, brightness: Float, durationMs: Int = 4000) {
        val id = Bridge.nextAlertId()
        previewAlertId = id
        send(true, Bridge.alertJson(id, pattern, color, durationMs, speedMs, brightness), arm = true)

        _previewLook.value = Ambient(
            pattern = pattern, color = color, speedMs = speedMs, brightness = brightness,
        )
        previewClear?.let { main.removeCallbacks(it) }
        val clear = Runnable { _previewLook.value = null }
        previewClear = clear
        main.postDelayed(clear, durationMs.toLong())
    }

    /**
     * Kills a running preview. Called when the app leaves the foreground: a test the user started by
     * hand must never outlive the screen they started it from.
     */
    fun stopPreview() {
        previewClear?.let { main.removeCallbacks(it) }
        _previewLook.value = null
        if (previewAlertId == null) return
        previewAlertId = null
        // clears the test immediately, and does not hand ambient a fresh window on the way out
        pushCurrent(arm = false)
    }

    /** Battery level from the sticky broadcast — no receiver to keep alive. */
    private fun batteryPct(): Int {
        val i = app.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return 100
        val level = i.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
        val charging = i.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (level < 0 || scale <= 0) return 100
        // on the charger there is no reason to hold the array back
        return if (charging) 100 else level * 100 / scale
    }

    private fun inQuietWindow(nowMin: Int): Boolean {
        val start = _quietStart.value
        val end = _quietEnd.value
        if (start == end) return false
        return if (start < end) nowMin in start until end
        else nowMin >= start || nowMin < end       // window crosses midnight
    }

    private fun nowMinutes(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    }

    private fun screenOn(): Boolean =
        app.getSystemService(android.os.PowerManager::class.java)?.isInteractive ?: true

    /** Why the array must stay dark right now, or null. */
    private fun suppressionNow(): Suppression? {
        if (_screenOffOnly.value && screenOn()) return Suppression.SCREEN_ON
        // a dimmed quiet window is not a suppression: it still lights, just far lower
        if (_quietEnabled.value && !_quietDim.value && inQuietWindow(nowMinutes())) {
            return Suppression.QUIET_HOURS
        }
        if (_batteryGuard.value && batteryPct() <= _batteryMinPct.value) return Suppression.LOW_BATTERY
        return null
    }

    /** Scale applied to every frame — below 1 only inside a dimmed quiet window. */
    private fun dimFactor(): Float =
        if (_quietEnabled.value && _quietDim.value && inQuietWindow(nowMinutes())) {
            _quietDimPct.value / 100f
        } else {
            1f
        }

    /** True while a dimmed quiet window is in effect, for the UI to explain itself. */
    fun inDimmedWindow(): Boolean = dimFactor() < 1f

    /**
     * Re-checks screen, clock and battery, and re-pushes if the answer changed.
     *
     * [armOnRelease] is for the screen going off: that is the moment the user wants the array back, so
     * it starts a fresh auto-off window. The duty-cycle guard still bounds the total.
     */
    fun refreshSuppression(armOnRelease: Boolean = false) {
        val was = _suppression.value
        val now = suppressionNow()
        if (was != now) {
            _suppression.value = now
            pushCurrent(arm = armOnRelease && now == null)
            HiLightTile.refresh(app)
        }
    }

    private fun send(enabled: Boolean, alert: JSONObject?, arm: Boolean = true) {
        if (alert == null) previewAlertId = null
        // quiet hours and the battery guard override the master switch, and hand the array back to
        // the system rather than merely blanking it, so the system's own alerts still work
        val suppressed = suppressionNow()
        _suppression.value = suppressed
        val json = Bridge.stateJson(
            enabled && suppressed == null,
            _priority.value, _ambient.value, alert, _ambientTimeoutMs.value, arm, dimFactor(),
        )
        val active = backend()
        active.push(json)
        // If Shizuku is driving, make sure a helper left running from an adb session lets go of the
        // light array instead of fighting us for it.
        if (active.transport == Transport.SHIZUKU) {
            Bridge.writeState(
                app,
                Bridge.stateJson(
                    false, _priority.value, _ambient.value, null, _ambientTimeoutMs.value, arm,
                    dimFactor(),
                ),
            )
        }
        _activeTransport.value = active.transport
        refreshStatus()
    }

    fun refreshStatus() {
        if (_transport.value != Transport.ADB) shizuku.refresh()
        _status.value = backend().status()
        _activeTransport.value = backend().transport
    }

    private fun randomColor(): Int {
        val hsv = floatArrayOf((0..359).random().toFloat(), 1f, 1f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    // ------------------------------------------------------------------ persistence

    private fun loadAmbient(): Ambient =
        prefs.getString("ambient", null)?.let { runCatching { Ambient.fromJson(JSONObject(it)) }.getOrNull() }
            ?: Ambient()

    private fun loadRules(): List<AppRule> =
        prefs.getString("rules", null)?.let { raw ->
            runCatching {
                val a = JSONArray(raw)
                (0 until a.length()).mapNotNull { i ->
                    runCatching { AppRule.fromJson(a.getJSONObject(i)) }.getOrNull()
                }
            }.getOrNull()
        } ?: emptyList()

    private fun saveRules() {
        val a = JSONArray()
        _rules.value.forEach { a.put(it.toPrefsJson()) }
        prefs.edit().putString("rules", a.toString()).apply()
    }

    companion object {
        @Volatile
        private var instance: Store? = null

        fun get(ctx: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(ctx.applicationContext).also { instance = it }
        }
    }
}

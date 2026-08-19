package com.hilight.studio

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.hilight.core.IHiLightService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import rikka.shizuku.Shizuku

/** How the privileged renderer is reached. */
enum class Transport(val label: String) {
    /** Prefer Shizuku, fall back to an adb-started helper. */
    AUTO("Auto"),
    SHIZUKU("Shizuku"),
    ADB("ADB helper"),
}

/** A privileged renderer the app can push state to. */
interface Backend {
    val transport: Transport
    fun push(json: String)
    fun status(): HelperStatus
}

/**
 * Talks to the adb-started [com.hilight.core.AdbHelper] through the two JSON files.
 *
 * The app must own the directory and both files: on external storage a file keeps its creator's UID,
 * and a file the shell created is unreadable here.
 */
class AdbBackend(private val ctx: Context) : Backend {

    override val transport = Transport.ADB

    override fun push(json: String) = Bridge.writeState(ctx, json)

    override fun status(): HelperStatus = Bridge.readStatus(ctx)
}

/**
 * Talks to [HiLightUserService], which Shizuku launches into a shell-UID process.
 *
 * Shizuku itself is started once per boot by the user, either from on-device wireless debugging (no
 * computer needed) or from adb.
 */
class ShizukuBackend(private val ctx: Context) : Backend {

    enum class State { NOT_INSTALLED, NOT_RUNNING, NEEDS_PERMISSION, CONNECTING, CONNECTED, FAILED }

    override val transport = Transport.SHIZUKU

    private val _state = MutableStateFlow(State.NOT_RUNNING)
    val state: StateFlow<State> = _state.asStateFlow()

    private var service: IHiLightService? = null
    private var lastError: String? = null

    /** Latest state document, replayed when the service (re)connects. */
    private var pending: String? = null

    /**
     * Called whenever availability changes in either direction.
     *
     * On connect: a fresh user service holds no state, so the current look has to be pushed at once
     * or the LEDs stay dark until the user touches a control. On loss: the store needs to re-push
     * through whatever transport is left, for the same reason.
     */
    var onAvailabilityChanged: (() -> Unit)? = null

    private val args = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, HiLightUserService::class.java.name)
    )
        .daemon(true)                       // survive the app process going away
        .processNameSuffix("hilight")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null || !binder.pingBinder()) {
                _state.value = State.FAILED
                lastError = "service returned a dead binder"
                return
            }
            service = IHiLightService.Stub.asInterface(binder)
            _state.value = State.CONNECTED
            Log.i(TAG, "user service connected, ${runCatching { service?.ledCount() }.getOrNull()} LEDs")
            pending?.let { push(it) }       // replay whatever the UI last asked for
            onAvailabilityChanged?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            if (_state.value == State.CONNECTED) _state.value = State.NOT_RUNNING
            onAvailabilityChanged?.invoke()
        }
    }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) bind() else {
                _state.value = State.NEEDS_PERMISSION
            }
        }

    init {
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            // Shizuku itself went away (stopped, or the device rebooted its adb session)
            service = null
            _state.value = State.NOT_RUNNING
            onAvailabilityChanged?.invoke()
        }
        refresh()
    }

    fun isInstalled(): Boolean = runCatching {
        ctx.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
        true
    }.getOrDefault(false)

    /** Re-evaluates availability, and connects when it can be done without user interaction. */
    fun refresh() {
        if (_state.value == State.CONNECTED && service?.asBinder()?.pingBinder() == true) return
        if (!Shizuku.pingBinder()) {
            if (!isInstalled()) {
                _state.value = State.NOT_INSTALLED
                return
            }
            // Shizuku hands its binder to an app when that app's process starts. A Shizuku started
            // *after* this process therefore stays invisible until the app is reopened — verified on
            // device, and the reason the Setup card says so. There is no app-side pull for this:
            // ShizukuProvider.requestBinderForNonProviderProcess() only talks to the app's own
            // provider, not to the manager.
            _state.value = State.NOT_RUNNING
            return
        }
        if (Shizuku.isPreV11()) {
            _state.value = State.FAILED
            lastError = "Shizuku is too old; v11 or newer is required"
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            _state.value = State.NEEDS_PERMISSION
            return
        }
        bind()
    }

    /** Asks Shizuku for access; the user confirms in Shizuku's own dialog. */
    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            refresh()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) bind()
        else Shizuku.requestPermission(PERMISSION_REQUEST)
    }

    private fun bind() {
        if (_state.value == State.CONNECTING || _state.value == State.CONNECTED) return
        _state.value = State.CONNECTING
        runCatching { Shizuku.bindUserService(args, connection) }
            .onFailure {
                _state.value = State.FAILED
                lastError = it.message
                Log.w(TAG, "bindUserService failed", it)
            }
    }

    fun unbind() {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        service = null
        _state.value = State.NOT_RUNNING
    }

    fun errorText(): String? = lastError

    override fun push(json: String) {
        pending = json
        val s = service ?: return
        runCatching { s.setState(json) }.onFailure {
            Log.w(TAG, "setState failed", it)
            service = null
            _state.value = State.NOT_RUNNING
            onAvailabilityChanged?.invoke()
        }
    }

    override fun status(): HelperStatus {
        val s = service ?: return HelperStatus(alive = false)
        val raw = runCatching { s.status() }.getOrNull() ?: return HelperStatus(alive = false)
        return runCatching {
            val o = JSONObject(raw)
            HelperStatus(
                alive = true,
                ageMs = 0,
                pid = o.optInt("pid", -1),
                ledCount = o.optInt("ledCount", 0),
                sessionOpen = o.optBoolean("session", false),
                mode = o.optString("mode", "-"),
                ambientRemainingMs = o.optLong("ambientRemainingMs", 0),
                ambientHeld = o.optBoolean("ambientHeld", false),
                resting = o.optBoolean("resting", false),
                dutyPct = o.optInt("dutyPct", 0),
            )
        }.getOrDefault(HelperStatus(alive = false))
    }

    companion object {
        const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
        const val PERMISSION_REQUEST = 4242
        private const val TAG = "HiLightShizuku"
    }
}

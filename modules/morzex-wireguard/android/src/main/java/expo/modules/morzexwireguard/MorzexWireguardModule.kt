package expo.modules.morzexwireguard

import android.app.Activity
import android.content.Context
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.ByteArrayInputStream
import kotlinx.coroutines.launch

private object WireGuardRuntime {
  private var backend: GoBackend? = null

  private val tunnel = object : Tunnel {
    override fun getName(): String = "MORZEX"

    override fun onStateChange(newState: Tunnel.State) {
      // وضعیت واقعی از Backend خوانده می‌شود.
    }
  }

  @Synchronized
  fun connect(context: Context, config: Config): String {
    val activeBackend = backend
      ?: GoBackend(context.applicationContext).also { backend = it }

    return activeBackend
      .setState(tunnel, Tunnel.State.UP, config)
      .name
  }

  @Synchronized
  fun disconnect(): String {
    val activeBackend = backend ?: return Tunnel.State.DOWN.name

    return activeBackend
      .setState(tunnel, Tunnel.State.DOWN, null)
      .name
  }

  @Synchronized
  fun status(): String {
    return backend?.getState(tunnel)?.name ?: Tunnel.State.DOWN.name
  }
}

class MorzexWireguardModule : Module() {
  private var pendingPromise: Promise? = null
  private var pendingConfig: Config? = null

  override fun definition() = ModuleDefinition {
    Name("MorzexWireguard")

    AsyncFunction("connect") { configText: String, promise: Promise ->
      if (pendingPromise != null) {
        promise.reject(
          "VPN_REQUEST_PENDING",
          "درخواست مجوز قبلی هنوز تمام نشده است.",
          null
        )
        return@AsyncFunction
      }

      val config = try {
        Config.parse(
          ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8))
        )
      } catch (e: Exception) {
        promise.reject(
          "INVALID_WIREGUARD_CONFIG",
          "کانفیگ WireGuard قابل خواندن نیست.",
          e
        )
        return@AsyncFunction
      }

      val activity = appContext.currentActivity
      if (activity == null) {
        promise.reject(
          "NO_ACTIVITY",
          "فعالیت اندروید برای درخواست مجوز در دسترس نیست.",
          null
        )
        return@AsyncFunction
      }

      val permissionIntent = try {
        VpnService.prepare(appContext.applicationContext)
      } catch (e: Exception) {
        promise.reject("VPN_PERMISSION_ERROR", e.message, e)
        return@AsyncFunction
      }

      if (permissionIntent != null) {
        pendingPromise = promise
        pendingConfig = config

        appContext.mainQueue.launch {
          try {
            activity.startActivityForResult(
              permissionIntent,
              VPN_REQUEST_CODE
            )
          } catch (e: Exception) {
            pendingPromise = null
            pendingConfig = null
            promise.reject("VPN_PERMISSION_ERROR", e.message, e)
          }
        }
      } else {
        appContext.backgroundCoroutineScope.launch {
          try {
            val state = WireGuardRuntime.connect(
              appContext.applicationContext,
              config
            )
            promise.resolve(
              mapOf(
                "state" to state,
                "connected" to (state == Tunnel.State.UP.name)
              )
            )
          } catch (e: Exception) {
            promise.reject("VPN_CONNECT_FAILED", e.message, e)
          }
        }
      }
    }

    AsyncFunction("disconnect") { promise: Promise ->
      appContext.backgroundCoroutineScope.launch {
        try {
          val state = WireGuardRuntime.disconnect()
          promise.resolve(
            mapOf(
              "state" to state,
              "connected" to (state == Tunnel.State.UP.name)
            )
          )
        } catch (e: Exception) {
          promise.reject("VPN_DISCONNECT_FAILED", e.message, e)
        }
      }
    }

    Function("getStatus") {
      val state = WireGuardRuntime.status()
      mapOf(
        "state" to state,
        "connected" to (state == Tunnel.State.UP.name)
      )
    }

    OnActivityResult { _, payload ->
      if (payload.requestCode == VPN_REQUEST_CODE) {
        val promise = pendingPromise
        val config = pendingConfig

        pendingPromise = null
        pendingConfig = null

        if (promise != null && config != null) {
          if (payload.resultCode != Activity.RESULT_OK) {
            promise.reject(
              "VPN_PERMISSION_DENIED",
              "مجوز اتصال VPN داده نشد.",
              null
            )
          } else {
            appContext.backgroundCoroutineScope.launch {
              try {
                val state = WireGuardRuntime.connect(
                  appContext.applicationContext,
                  config
                )
                promise.resolve(
                  mapOf(
                    "state" to state,
                    "connected" to (state == Tunnel.State.UP.name)
                  )
                )
              } catch (e: Exception) {
                promise.reject("VPN_CONNECT_FAILED", e.message, e)
              }
            }
          }
        }
      }
    }
  }

  private companion object {
    const val VPN_REQUEST_CODE = 7402
  }
}

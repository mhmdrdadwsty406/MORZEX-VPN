package expo.modules.morzexvless

import android.app.Activity
import android.net.VpnService
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import libXray.LibXray
import org.json.JSONObject

class MorzexVlessModule : Module() {

    private var pendingPromise: Promise? = null
    private var pendingConfig: String? = null

    private fun invokeXray(method: String): JSONObject {
        val request = JSONObject()
            .put("apiVersion", 3)
            .put("method", method)

        return JSONObject(LibXray.invoke(request.toString())).also {
            check(it.optBoolean("success")) {
                it.optString("error", "libXray request failed")
                    .ifBlank { "libXray request failed: $method" }
            }
        }
    }

    private fun startVless(config: String, promise: Promise) {
        MorzexVpnService.start(
            appContext.applicationContext,
            config
        ) { success, message ->
            if (success) {
                promise.resolve(
                    mapOf(
                        "started" to true,
                        "connected" to true,
                        "message" to message
                    )
                )
            } else {
                promise.reject(
                    "VLESS_START_FAILED",
                    message,
                    null
                )
            }
        }
    }

    override fun definition() = ModuleDefinition {
        Name("MorzexVless")

        AsyncFunction("start") { xrayJson: String, promise: Promise ->
            if (xrayJson.isBlank()) {
                promise.reject("EMPTY_CONFIG", "Xray configuration is empty", null)
                return@AsyncFunction
            }

            if (pendingPromise != null) {
                promise.reject(
                    "VPN_REQUEST_PENDING",
                    "A VPN permission request is already pending.",
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
                val activity = appContext.currentActivity
                if (activity == null) {
                    promise.reject(
                        "NO_ACTIVITY",
                        "Android activity is unavailable.",
                        null
                    )
                    return@AsyncFunction
                }

                pendingPromise = promise
                pendingConfig = xrayJson

                appContext.mainQueue.launch {
                    try {
                        activity.startActivityForResult(
                            permissionIntent,
                            VPN_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        pendingPromise = null
                        pendingConfig = null
                        promise.reject(
                            "VPN_PERMISSION_ERROR",
                            e.message,
                            e
                        )
                    }
                }
            } else {
                startVless(xrayJson, promise)
            }
        }

        AsyncFunction("stop") {
            MorzexVpnService.stop(appContext.applicationContext)
            mapOf(
                "stopping" to true,
                "message" to "VLESS stop requested"
            )
        }

        Function("getStatus") {
            val response = invokeXray("getXrayState")
            val running = response
                .optJSONObject("data")
                ?.optBoolean("running", false) ?: false

            mapOf(
                "available" to true,
                "connected" to running,
                "message" to if (running) "Xray is running" else "Xray is stopped"
            )
        }

        AsyncFunction("testConfig") { xrayJson: String ->
            require(xrayJson.isNotBlank()) {
                "Xray configuration is empty"
            }

            val request = JSONObject()
                .put("apiVersion", 3)
                .put("method", "testXray")
                .put(
                    "payload",
                    JSONObject().put("xrayJson", xrayJson)
                )

            val response = JSONObject(LibXray.invoke(request.toString()))
            check(response.optBoolean("success")) {
                response.optString("error", "Xray configuration test failed")
            }

            mapOf(
                "valid" to true,
                "message" to "Xray configuration accepted"
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
                            "VPN permission was denied.",
                            null
                        )
                    } else {
                        startVless(config, promise)
                    }
                }
            }
        }
    }

    private companion object {
        const val VPN_REQUEST_CODE = 7404
    }
}

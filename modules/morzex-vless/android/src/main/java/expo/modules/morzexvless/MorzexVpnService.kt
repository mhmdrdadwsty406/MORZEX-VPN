package expo.modules.morzexvless

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import libXray.DialerController
import libXray.LibXray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class MorzexVpnService : VpnService() {

    companion object {
        const val ACTION_START = "morzex.vless.START"
        const val ACTION_STOP = "morzex.vless.STOP"
        const val EXTRA_CONFIG = "xray_config"

        private const val CHANNEL_ID = "morzex_vless_vpn"
        private const val NOTIFICATION_ID = 7403

        @Volatile
        var onStartResult: ((Boolean, String) -> Unit)? = null

        @Volatile
        var running = false

        fun start(
            context: Context,
            config: String,
            callback: (Boolean, String) -> Unit
        ) {
            onStartResult = callback

            val intent = Intent(context, MorzexVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CONFIG, config)
            }

            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                onStartResult = null
                callback(false, e.message ?: "Cannot start VPN service")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MorzexVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunDescriptor: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                startAsForeground()

                val config = intent.getStringExtra(EXTRA_CONFIG)
                if (config.isNullOrBlank()) {
                    reportStart(false, "Xray configuration is empty")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }

                scope.launch {
                    try {
                        startXray(config)
                        running = true
                        reportStart(true, "VLESS started")
                    } catch (e: Exception) {
                        closeTun()
                        running = false
                        reportStart(false, e.message ?: "VLESS start failed")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    }
                }
            }

            ACTION_STOP -> {
                scope.launch {
                    try {
                        stopXray()
                    } finally {
                        running = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("MORZEX VPN")
                .setContentText("VLESS is starting")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("MORZEX VPN")
                .setContentText("VLESS is starting")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MORZEX VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun startXray(configText: String) {
        if (tunDescriptor != null) {
            stopXray()
        }

        val config = JSONObject(configText)
        val inbounds = config.optJSONArray("inbounds")
            ?: error("Xray config has no inbounds")

        var tunSettings: JSONObject? = null

        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("protocol") == "tun") {
                tunSettings = inbound.optJSONObject("settings")
                break
            }
        }

        val settings = tunSettings
            ?: error("A TUN inbound with settings is required")

        val gateway = settings.optJSONArray("gateway")
            ?: error("TUN settings.gateway is required")

        val mtu = settings.optInt("mtu", 1500)
        require(mtu in 576..65535) { "Invalid TUN MTU" }

        val builder = Builder()
            .setSession("MORZEX VLESS")
            .setMtu(mtu)
            .setBlocking(false)

        var hasIPv4 = false
        var hasIPv6 = false

        for (i in 0 until gateway.length()) {
            val cidr = gateway.optString(i)
            val parts = cidr.split("/")
            require(parts.size == 2) { "Invalid TUN gateway: $cidr" }

            val address = InetAddress.getByName(parts[0])
            val prefix = parts[1].toInt()

            when (address) {
                is Inet4Address -> {
                    require(prefix in 0..32)
                    hasIPv4 = true
                }
                is Inet6Address -> {
                    require(prefix in 0..128)
                    hasIPv6 = true
                }
                else -> error("Unsupported TUN gateway address")
            }

            builder.addAddress(parts[0], prefix)
        }

        require(hasIPv4) {
            "TUN gateway must include an IPv4 address"
        }

        builder.addRoute("0.0.0.0", 0)

        if (hasIPv6) {
            builder.addRoute("::", 0)
        }

        val dns = settings.optJSONArray("dns")
        if (dns != null && dns.length() > 0) {
            for (i in 0 until dns.length()) {
                val server = dns.optString(i)
                require(server.isNotBlank())
                builder.addDnsServer(server)
            }
        } else {
            builder.addDnsServer("1.1.1.1")
        }

        val descriptor = builder.establish()
            ?: error("Android could not establish the TUN interface")

        tunDescriptor = descriptor

        val dialerController = object : DialerController {
            override fun protectFd(fd: Long): Boolean {
                if (fd < 0 || fd > Int.MAX_VALUE) return false
                return protect(fd.toInt())
            }
        }

        LibXray.registerDialerController(dialerController)

        val env = config.optJSONObject("env") ?: JSONObject()
        env.put("xray.tun.fd", descriptor.fd.toString())
        config.put("env", env)

        val request = JSONObject()
            .put("apiVersion", 3)
            .put("method", "runXray")
            .put(
                "payload",
                JSONObject().put("xrayJson", config.toString())
            )

        val response = JSONObject(
            LibXray.invoke(request.toString())
        )

        check(response.optBoolean("success")) {
            response.optString("error", "libXray runXray failed")
                .ifBlank { "libXray runXray failed" }
        }
    }

    private fun stopXray() {
        try {
            val request = JSONObject()
                .put("apiVersion", 3)
                .put("method", "stopXray")

            LibXray.invoke(request.toString())
        } finally {
            closeTun()
            running = false
        }
    }

    private fun closeTun() {
        try {
            tunDescriptor?.close()
        } catch (_: Exception) {
        }
        tunDescriptor = null
    }

    private fun reportStart(success: Boolean, message: String) {
        val callback = onStartResult
        onStartResult = null
        callback?.invoke(success, message)
    }

    override fun onRevoke() {
        scope.launch {
            try {
                stopXray()
            } finally {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        try {
            runBlocking(Dispatchers.IO) {
                stopXray()
            }
        } catch (_: Exception) {
            closeTun()
        }

        scope.cancel()
        super.onDestroy()
    }
}

package one.mixin.android.net

import android.content.Context
import android.os.Build
import androidx.core.net.toUri
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.API.Mixin_URL
import one.mixin.android.Constants.API.URL
import one.mixin.android.R
import one.mixin.android.di.HostSelectionInterceptor.Companion.CURRENT_URL
import one.mixin.android.extension.getNetworkOperatorName
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.networkType
import one.mixin.android.extension.timeFormat
import org.threeten.bp.Instant
import timber.log.Timber
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Enumeration

fun diagnosis(context: Context, diagnosisCallback: (String) -> Unit) {
    val result = StringBuilder()

    result.append("${context.getString(R.string.App_version)}: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})").appendLine()
        .append("${context.getString(R.string.Manufacturer)}: ${Build.MANUFACTURER}").appendLine()
        .append("${context.getString(R.string.Model)}: ${Build.MODEL}").appendLine()
        .append("${context.getString(R.string.System_version)}: ${Build.VERSION.RELEASE}").appendLine()
        .append("${context.getString(R.string.Time)}: ${Instant.now().toString().timeFormat()}").appendLine()
        .appendLine()
    diagnosisCallback(result.toString())
    result.clear()

    result.append("${context.getString(R.string.is_network_available)}: ${context.networkConnected()}").appendLine()
    result.append("${context.getString(R.string.Network_Type)}: ${context.networkType()}").appendLine()
        .appendLine()
    diagnosisCallback(result.toString())
    result.clear()

    getExportIp(result, context)
    val ipAddress = getIpAddress()
    result.append("${context.getString(R.string.Local_IP)}: $ipAddress").appendLine()
        .append("${context.getString(R.string.Network_Operator)}: ${context.getNetworkOperatorName()}").appendLine()
        .appendLine()
    diagnosisCallback(result.toString())
    result.clear()

    val hosts = arrayOf(CURRENT_URL.toUri().host, (if (CURRENT_URL == URL) Mixin_URL.toUri() else URL.toUri()).host)
    val dnsList = arrayListOf(
        CustomDns("8.8.8.8"),
        CustomDns("1.1.1.1"),
        CustomDns("2001:4860:4860::8888"),
        Dns.SYSTEM
    )
    val prefix = context.getString(R.string.parse_dns_result)
    hosts.forEach host@{ host ->
        requireNotNull(host)
        result.append("Nslookup for $host").appendLine()
        dnsList.forEach { dns ->
            val dnsHost = if (dns is CustomDns) "dns ${dns.dnsHostname}" else "System DNS"
            result.append("Use $dnsHost").appendLine()
            val addresses = try {
                dns.lookup(host)
            } catch (e: UnknownHostException) {
                null
            }
            if (addresses.isNullOrEmpty()) {
                result.append("Nslookup for $host use dns $dns failed").appendLine()
                return@forEach
            }
            addresses.forEach addr@{ addr ->
                val ipAddr = addr.hostAddress ?: return@addr
                val pingResult = ping(ipAddr)
                Timber.i("Ping $ipAddr result: $pingResult")
                result.append("$prefix Ping: [$ipAddr] [${if (pingResult.isNullOrEmpty()) "FAILURE" else "SUCCESS"}]").appendLine()
            }
        }
        diagnosisCallback(result.appendLine().toString())
        result.clear()
    }
    diagnosisCallback(context.getString(R.string.Diagnosis_Complete))
}

fun ping(domain: String, count: Int = 1, timeout: Int = 10): String? {
    val command = "/system/bin/ping -c $count -w $timeout $domain"
    var process: Process? = null
    try {
        process = Runtime.getRuntime().exec(command)
        val input = process.inputStream
        input.bufferedReader().use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (null != reader.readLine().also { line = it }) {
                sb.append(line).appendLine()
            }
            return sb.toString()
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        process?.destroy()
    }
    return null
}

private const val EXPORT_IP_PRIMARY = "https://nstool.netease.com/"
private const val EXPORT_IP_SECONDARY = "http://api.ipify.org/"

private fun getExportIp(result: StringBuilder, context: Context) {
    val client = OkHttpClient()
    var ipRequest = Request.Builder().url(EXPORT_IP_PRIMARY).build()
    try {
        var data = client.newCall(ipRequest).execute().body?.string()
            ?: throw IOException("EXPORT_IP_PRIMARY no data")
        val url = data.substring(data.indexOf("src=") + 4, data.lastIndexOf("frameborder")).replace("'".toRegex(), "").replace(" ".toRegex(), "")
        ipRequest = Request.Builder().url(url).build()
        data = client.newCall(ipRequest).execute().body?.string()
            ?: throw IOException("EXPORT_IP_PRIMARY no data")
        val dataIp = data.substring(data.indexOf("您的IP地址信息") + 10)
        val dataAddress = dataIp.substring(0, dataIp.indexOf("<br>"))
        val ips = dataAddress.split(" ").toTypedArray()
        result.append("${context.getString(R.string.export_ip)}: ${ips[0]}").appendLine()
            .append("${context.getString(R.string.Operator)}: ${ips[1]}").appendLine()
    } catch (e: Exception) {
        Timber.i("Get export ip from $EXPORT_IP_PRIMARY meet ${e.localizedMessage}")
        try {
            ipRequest = Request.Builder().url(EXPORT_IP_SECONDARY).build()
            val exportIp = client.newCall(ipRequest).execute().body?.string()
            result.append("${context.getString(R.string.export_ip)}: $exportIp")
        } catch (e: Exception) {
            Timber.i("Get export ip from $EXPORT_IP_SECONDARY meet ${e.localizedMessage}")
        }
    }
    result.appendLine()
}

private fun getIpAddress(): String? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf: NetworkInterface = en.nextElement()
            val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress: InetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.getHostAddress()
                }
            }
        }
    } catch (ex: SocketException) {
        ex.printStackTrace()
    }
    return null
}

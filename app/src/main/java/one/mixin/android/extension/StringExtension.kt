@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.graphics.Bitmap
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import okio.Buffer
import okio.ByteString
import okio.GzipSink
import okio.GzipSource
import okio.Okio
import okio.Source
import one.mixin.android.ui.contacts.QRFragment
import one.mixin.android.util.GzipException
import org.threeten.bp.Instant
import java.io.IOException
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

fun String.generateQRCode(size: Int): Bitmap? {
    val result: BitMatrix
    try {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        result = MultiFormatWriter().encode(this, BarcodeFormat.QR_CODE, size, size, hints)
    } catch (iae: IllegalArgumentException) {
        // Unsupported format
        return null
    }

    val width = result.width
    val height = result.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (result.get(x, y)) QRFragment.BLACK else QRFragment.WHITE
        }
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

fun String.getEpochNano(): Long {
    val inst = Instant.parse(this)
    var time = inst.epochSecond
    time *= 1000000000L
    time += inst.nano
    return time
}

@Throws(IOException::class)
fun String.gzip(): ByteString {
    val result = Buffer()
    val sink = Okio.buffer(GzipSink(result))
    sink.use {
        sink.write(toByteArray())
    }
    return result.readByteString()
}

@Throws(GzipException::class)
fun ByteString.ungzip(): String {
    val buffer = Buffer().write(this)
    val gzip = GzipSource(buffer as Source)
    return Okio.buffer(gzip).readUtf8()
}

inline fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digested = md.digest(toByteArray())
    return digested.joinToString("") {
        String.format("%02x", it)
    }
}

inline fun String.sha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA256")
    return md.digest(toByteArray())
}

inline fun String.isWebUrl(): Boolean {
    return startsWith("http://", true) || startsWith("https://", true)
}

inline fun <reified T> Gson.fromJson(json: JsonElement) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)!!

private val HEX_CHARS by lazy { "0123456789abcdef".toCharArray() }
fun ByteArray.toHex(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}

inline fun Long.toLeByteArray(): ByteArray {
    var num = this
    val result = ByteArray(8)
    for (i in (0..7)) {
        result[i] = (num and 0xffL).toByte()
        num = num shr 8
    }
    return result
}

fun String.formatPublicKey(): String {
    require(length > 10)
    return substring(0, 6) + "..." + substring(length - 4, length)
}

fun String.numberFormat(): String {
    if (this.isEmpty()) return this

    return try {
        DecimalFormat(getPattern(64)).format(BigDecimal(this))
    } catch (e: NumberFormatException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun String.numberFormat8(): String {
    if (this.isEmpty()) return this

    return try {
        DecimalFormat(this.getPattern()).format(BigDecimal(this))
    } catch (e: NumberFormatException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun String.numberFormat2(): String {
    if (this.isEmpty()) return this

    return try {
        DecimalFormat(this.getPattern(2)).format(BigDecimal(this))
    } catch (e: NumberFormatException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun BigDecimal.numberFormat(): String {
    return try {
        DecimalFormat(this.toPlainString().getPattern(64)).format(this)
    } catch (e: NumberFormatException) {
        this.toPlainString()
    } catch (e: IllegalArgumentException) {
        this.toPlainString()
    }
}

fun BigDecimal.numberFormat8(): String {
    return try {
        DecimalFormat(this.toPlainString().getPattern()).format(this)
    } catch (e: NumberFormatException) {
        this.toPlainString()
    } catch (e: IllegalArgumentException) {
        this.toPlainString()
    }
}

fun BigDecimal.numberFormat2(): String {
    return try {
        DecimalFormat(this.toPlainString().getPattern(2)).format(this)
    } catch (e: NumberFormatException) {
        this.toPlainString()
    } catch (e: IllegalArgumentException) {
        this.toPlainString()
    }
}

fun String.getPattern(count: Int = 8): String {
    if (this.isEmpty()) return ""

    val index = this.indexOf('.')
    if (index == -1) return ",###"
    if (index >= count) return ",###"

    val bit = if (index == 1 && this[0] == '0') count + 1 else count

    val sb = StringBuilder(",###.")
    for (i in 0 until (bit - index)) {
        sb.append('#')
    }
    return sb.toString()
}

fun Long.formatMillis(): String {
    val formatBuilder = StringBuilder()
    val formatter = Formatter(formatBuilder, Locale.getDefault())
    Util.getStringForTime(formatBuilder, formatter, this)
    return formatBuilder.toString()
}
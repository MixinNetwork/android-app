@file:Suppress("NOTHING_TO_INLINE")

package one.mixin.android.extension

import android.graphics.Bitmap
import android.text.Editable
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.Formatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.collections.set
import kotlin.math.abs
import okio.Buffer
import okio.ByteString
import okio.GzipSink
import okio.GzipSource
import okio.Okio
import okio.Source
import one.mixin.android.util.GzipException
import org.threeten.bp.Instant

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
            pixels[offset + x] = if (result.get(x, y)) {
                -0x1000000 // black
            } else {
                -0x1 // white
            }
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

private val HEX_CHARS = "0123456789abcdef"
fun ByteArray.toHex(): String {
    val hex = HEX_CHARS.toCharArray()
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(hex[firstIndex])
        result.append(hex[secondIndex])
    }

    return result.toString()
}

fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i])
        val secondIndex = HEX_CHARS.indexOf(this[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result[i.shr(1)] = octet.toByte()
    }
    return result
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
    if (this.length <= 10) return this
    return substring(0, 6) + "..." + substring(length - 4, length)
}

fun String.numberFormat(): String {
    if (this.isEmpty()) return this

    return try {
        DecimalFormat(getPattern(32)).format(BigDecimal(this))
    } catch (e: NumberFormatException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun String.numberFormat8(): String {
    if (this.isEmpty()) return this

    return try {
        DecimalFormat(getPattern()).format(BigDecimal(this))
    } catch (e: NumberFormatException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun String.numberFormat2(): String {
    if (this.isEmpty()) return this

    return try {
        DecimalFormat(",###.##").format(BigDecimal(this))
    } catch (e: NumberFormatException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun BigDecimal.numberFormat(): String {
    return try {
        DecimalFormat(this.toPlainString().getPattern(32)).format(this)
    } catch (e: NumberFormatException) {
        this.toPlainString()
    } catch (e: IllegalArgumentException) {
        this.toPlainString()
    }
}

fun BigDecimal.priceFormat(): String {
    return if (this.compareTo(BigDecimal.ONE) == 1 || this.compareTo(BigDecimal.ONE) == 0) {
        priceFormat2()
    } else {
        numberFormat8()
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

fun BigDecimal.priceFormat2(): String {
    return try {
        DecimalFormat(",##0.00").format(this)
    } catch (e: NumberFormatException) {
        this.toPlainString()
    } catch (e: IllegalArgumentException) {
        this.toPlainString()
    }
}

fun BigDecimal.numberFormat2(): String {
    return try {
        DecimalFormat(",###.##").format(this)
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

    val bit = if (index == 1 && this[0] == '0')
        count + 1
    else if (index == 2 && this[0] == '-' && this[1] == '0')
        count + 2
    else count

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

fun Editable.maxDecimal(bit: Int = 8) {
    val index = this.indexOf('.')
    if (index > -1) {
        val max = if (index == 1 && this[0] == '0')
            bit
        else if (index == 2 && this[0] == '-' && this[1] == '0')
            bit + 1
        else bit - 1
        if (this.length - 1 - index > max) {
            this.delete(this.length - 1, this.length)
        }
    }
}

fun String.checkNumber(): Boolean {
    return try {
        BigDecimal(this)
        true
    } catch (e: NumberFormatException) {
        false
    }
}

val idAvatarCodeMap = ConcurrentHashMap<String, Int>()
val idNameCodeMap = ConcurrentHashMap<String, Int>()

sealed class CodeType(val count: Int) {
    class Name(count: Int) : CodeType(count)
    object Avatar : CodeType(24)
}

fun String.getColorCode(codeType: CodeType): Int {
    val cacheMap = when (codeType) {
        is CodeType.Name -> idNameCodeMap
        is CodeType.Avatar -> idAvatarCodeMap
    }
    var code = cacheMap[this]
    if (code != null) return code

    val hashcode = try {
        UUID.fromString(this).hashCode()
    } catch (e: IllegalArgumentException) {
        hashCode()
    }
    code = abs(hashcode).rem(codeType.count)
    cacheMap[this] = code
    return code
}

inline fun <reified T : Serializable> String.deserialize(): T? {
    if (isNullOrEmpty()) return null

    try {
        ObjectInputStream(ByteArrayInputStream(this.toByteArray(charset("ISO-8859-1")))).use {
            return it.readObject() as T
        }
    } catch (e: Exception) {
        throw IOException("Deserialization error: ${e.message}, $e")
    }
}

inline fun <T : Serializable> T.serialize(): String? {
    try {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use {
            it.writeObject(this)
        }
        return baos.toString("ISO-8859-1")
    } catch (e: Exception) {
        throw IOException("Serialization error: ${e.message}, $e")
    }
}

private val escapeSqlChars = charArrayOf('\\', '%', '_', '[', ']')

fun String.escapeSql(): String {
    var result = this
    escapeSqlChars.forEach { c ->
        result = result.replace(c.toString(), "\\$c")
    }
    return result
}

fun String.getBotNumber(): String? {
    if (this.startsWith("@7000")) {
        val pattern = Pattern.compile("^@7000\\d* ")
        val matcher = pattern.matcher(this)
        if (matcher.find()) {
            return matcher.group().substring(1, matcher.end() - 1)
        }
    }
    return null
}

inline fun String?.getDeviceId(): Int {
    return if (this == null || this.isEmpty()) {
        1
    } else {
        UUID.fromString(this).hashCode()
    }
}

fun String.filterNonAscii() = replace("[^\\p{ASCII}]".toRegex(), "")

fun String.postOptimize(): String {
    return split("\n").take(20).joinToString("\n").postLengthOptimize()
}

fun String.postLengthOptimize(): String {
    return if (length > 1024) {
        substring(0, 1024)
    } else {
        this
    }
}

fun String.maxLimit(maxSize: Int = 64 * 1024): String {
    if (length < 32 * 1024) return this
    val bytes = toByteArray()
    return String(bytes.take(maxSize).toByteArray())
}

fun String?.joinWhiteSpace(): String {
    if (this == null) return ""

    return joinWithCharacter(' ')
}

fun String.joinStar() = joinWithCharacter('*')

fun String.joinWithCharacter(char: Char): String {
    val result = StringBuilder()
    this.trim().forEachIndexed { i, c ->
        val lookAhead = try {
            this[i + 1]
        } catch (ignored: IndexOutOfBoundsException) {
            char
        }
        val isSameType = if (c.isAlphabet() && lookAhead.isAlphabet()) {
            true
        } else {
            c.isDigit() && lookAhead.isDigit()
        }

        val needWhiteSpace = !isSameType && !c.isWhitespace()
        result.append(c)
        if (needWhiteSpace) {
            result.append(char)
        }
    }
    return result.toString().trim()
}

private fun Char.isAlphabet() = this in 'a'..'z' || this in 'A'..'Z'

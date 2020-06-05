package one.mixin.android.util

import com.bumptech.glide.load.Key
import java.io.UnsupportedEncodingException
import java.security.MessageDigest

class StringSignature(private val signature: String?) : Key {

    init {
        if (signature == null) {
            throw NullPointerException("Signature cannot be null!")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val that = other as StringSignature?

        return signature == that!!.signature
    }

    override fun hashCode(): Int {
        return signature!!.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        try {
            messageDigest.update(signature!!.toByteArray(charset(Key.STRING_CHARSET_NAME)))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        return (
            "StringSignature{" +
                "signature='" + signature + '\''.toString() +
                '}'.toString()
            )
    }
}

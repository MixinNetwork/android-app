package one.mixin.android.crypto

import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.toLeByteArray
import one.mixin.android.session.Session

abstract class BasePinCipher {
    fun encryptPinInternal(
        pinToken: ByteArray,
        code: ByteArray,
    ): String {
        val iterator = Session.getPinIterator()
        val pinByte = code + (currentTimeSeconds()).toLeByteArray() + iterator.toLeByteArray()
        val based = aesEncrypt(pinToken, pinByte).base64Encode()
        Session.storePinIterator(iterator + 1)
        return based
    }

    fun encryptTipPinInternal(
        pinToken: ByteArray,
        tipPriv: ByteArray,
        signTarget: ByteArray,
    ): String {
        val sig = initFromSeedAndSign(tipPriv, signTarget)
        val iterator = Session.getPinIterator()
        val pinByte = sig + (currentTimeSeconds()).toLeByteArray() + iterator.toLeByteArray()
        val based = aesEncrypt(pinToken, pinByte).base64Encode()
        Session.storePinIterator(iterator + 1)
        return based
    }
}

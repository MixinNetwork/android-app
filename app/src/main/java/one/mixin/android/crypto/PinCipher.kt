package one.mixin.android.crypto

import one.mixin.android.MixinApplication
import one.mixin.android.extension.decodeBase64
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import javax.inject.Inject

class PinCipher
    @Inject
    internal constructor(val tip: Tip) : BasePinCipher() {
        suspend fun encryptPin(
            pin: String,
            signTarget: ByteArray,
        ): String {
            val pinToken = Session.getPinToken()?.decodeBase64() ?: throw NullPointerException("No pin token")
            return if (Session.getTipPub().isNullOrBlank()) {
                encryptPinInternal(pinToken, pin.toByteArray())
            } else {
                val tipPrivateKey = tip.getOrRecoverTipPriv(MixinApplication.appContext, pin).getOrThrow()
                encryptTipPinInternal(pinToken, tipPrivateKey, signTarget)
            }
        }
    }

package one.mixin.android.ui.transfer

import one.mixin.android.crypto.Util
import org.whispersystems.libsignal.kdf.HKDFv3

object TransferCipher {
    private val INFO = "Mixin Device Transfer".toByteArray()

    fun generateKey(): ByteArray {
        return HKDFv3().deriveSecrets(
            Util.getSecretBytes(32),
            INFO,
            64,
        )
    }
}

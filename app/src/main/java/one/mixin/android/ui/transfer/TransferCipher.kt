package one.mixin.android.ui.transfer

import one.mixin.android.crypto.Util
import org.whispersystems.libsignal.kdf.HKDFv3

object TransferCipher {
    fun generateKey(): ByteArray {
        return HKDFv3().deriveSecrets(
            Util.getSecretBytes(32),
            "Mixin  Device Transfer".toByteArray(),
            64
        )
    }
}
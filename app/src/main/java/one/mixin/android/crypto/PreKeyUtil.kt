package one.mixin.android.crypto

import android.content.Context
import one.mixin.android.Constants.BATCH_SIZE
import one.mixin.android.crypto.storage.MixinPreKeyStore
import one.mixin.android.crypto.vo.PreKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import org.whispersystems.libsignal.util.Medium

object PreKeyUtil {
    @Synchronized
    fun generatePreKeys(context: Context): List<PreKeyRecord> {
        val preKeyStore = MixinPreKeyStore(context)
        val preKeyIdOffset = CryptoPreference.getNextPreKeyId(context)
        val records = KeyHelper.generatePreKeys(preKeyIdOffset, BATCH_SIZE)
        val preKeys = arrayListOf<PreKey>()
        for (record in records) {
            preKeys.add(PreKey(record.id, record.serialize()))
        }
        preKeyStore.storePreKeyList(preKeys)
        CryptoPreference.setNextPreKeyId(context, (preKeyIdOffset + BATCH_SIZE + 1) % Medium.MAX_VALUE)
        return records
    }

    @Synchronized
    fun generateSignedPreKey(
        context: Context,
        identityKeyPair: IdentityKeyPair,
        active: Boolean,
    ): SignedPreKeyRecord {
        val signedPreKeyStore = MixinPreKeyStore(context)
        val signedPreKeyId = CryptoPreference.getNextSignedPreKeyId(context)
        val record = KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId)
        signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record)
        CryptoPreference.setNextSignedPreKeyId(context, (signedPreKeyId + 1) % Medium.MAX_VALUE)

        if (active) {
            setActiveSignedPreKeyId(context, signedPreKeyId)
        }
        return record
    }

    @Synchronized
    fun setActiveSignedPreKeyId(
        context: Context,
        id: Int,
    ) {
        CryptoPreference.setActiveSignedPreKeyId(context, id)
    }

    @Synchronized
    fun getActiveSignedPreKeyId(context: Context): Int {
        return CryptoPreference.getActiveSignedPreKeyId(context)
    }
}

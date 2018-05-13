package one.mixin.android.crypto

import android.content.Context
import one.mixin.android.AppExecutors
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.crypto.vo.Identity
import org.whispersystems.libsignal.util.KeyHelper

open class IdentityKeyUtil {

    companion object {

        fun generateIdentityKeys(ctx: Context) {
            val registrationId = KeyHelper.generateRegistrationId(false)
            CryptoPreference.setLocalRegistrationId(ctx, registrationId)
            val identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val identity = Identity("-1",
                registrationId,
                identityKeyPair.publicKey.serialize(),
                identityKeyPair.privateKey.serialize(),
                0,
                System.currentTimeMillis())
            AppExecutors().diskIO().execute {
                SignalDatabase.getDatabase(ctx).identityDao().insert(identity)
            }
        }

        fun getIdentityKeyPair(context: Context) =
                SignalDatabase.getDatabase(context).identityDao().getLocalIdentity().getIdentityKeyPair()

        fun getIdentityKey(context: Context) =
                SignalDatabase.getDatabase(context).identityDao().getLocalIdentity().getIdentityKey()
    }
}
package one.mixin.android.crypto

import android.content.Context

open class ProfileKeyUtil {
    companion object {
        @Synchronized
        fun hasProfileKey(ctx: Context): Boolean {
            return CryptoPreference.getProfileKey(ctx) != null
        }

        @Synchronized
        fun getProfileKey(ctx: Context): ByteArray {
            var encodedProfileKey = CryptoPreference.getProfileKey(ctx)
            if (encodedProfileKey == null) {
                encodedProfileKey = Util.getSecret(32)
                CryptoPreference.setProfileKey(ctx, encodedProfileKey)
            }
            return Base64.decode(encodedProfileKey)
        }

        @Synchronized
        fun rotateProfileKey(ctx: Context): ByteArray {
            CryptoPreference.setProfileKey(ctx, null)
            return getProfileKey(ctx)
        }
    }
}
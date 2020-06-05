package one.mixin.android.crypto

import android.content.Context
import org.whispersystems.libsignal.util.Medium
import java.security.SecureRandom

object CryptoPreference {

    private const val CRYPTO_PREF_NAME = "crypto_pref_name"
    private const val LOCAL_REGISTRATION_ID_PREF = "pref_local_registration_id"
    private const val NEXT_PRE_KEY_ID = "pref_next_pre_key_id"
    private const val NEXT_PRE_SIGNED_PRE_KEY_ID = "pref_next_signed_pre_key_id"
    private const val ACTIVE_SIGNED_PRE_KEY_ID = "active_signed_pre_key_id"

    fun getLocalRegistrationId(context: Context): Int {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        return pref.getInt(LOCAL_REGISTRATION_ID_PREF, 0)
    }

    fun setLocalRegistrationId(context: Context, registrationId: Int) {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putInt(LOCAL_REGISTRATION_ID_PREF, registrationId).apply()
    }

    fun getNextPreKeyId(context: Context): Int {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        return pref.getInt(NEXT_PRE_KEY_ID, SecureRandom().nextInt(Medium.MAX_VALUE))
    }

    fun setNextPreKeyId(context: Context, nextPreKeyId: Int) {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putInt(NEXT_PRE_KEY_ID, nextPreKeyId).apply()
    }

    fun getNextSignedPreKeyId(context: Context): Int {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        return pref.getInt(NEXT_PRE_SIGNED_PRE_KEY_ID, SecureRandom().nextInt(Medium.MAX_VALUE))
    }

    fun setNextSignedPreKeyId(context: Context, nextSignedPreKeyId: Int) {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putInt(NEXT_PRE_SIGNED_PRE_KEY_ID, nextSignedPreKeyId).apply()
    }

    fun setActiveSignedPreKeyId(context: Context, signedPreKeyId: Int) {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putInt(ACTIVE_SIGNED_PRE_KEY_ID, signedPreKeyId).apply()
    }

    fun getActiveSignedPreKeyId(context: Context): Int {
        val pref = context.getSharedPreferences(CRYPTO_PREF_NAME, Context.MODE_PRIVATE)
        return pref.getInt(ACTIVE_SIGNED_PRE_KEY_ID, -1)
    }
}

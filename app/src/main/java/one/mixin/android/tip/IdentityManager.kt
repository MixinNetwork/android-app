package one.mixin.android.tip

import android.content.Context
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.TipService
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import javax.inject.Inject

class IdentityManager @Inject internal constructor(private val tipService: TipService) {

    fun checkIdentityExists(context: Context): Boolean {
        val pub = Session.getIdentityPub()
        val priv = context.defaultSharedPreferences.getString(Constants.Tip.Identity_Priv, null)
        return pub != null && priv != null
    }

    suspend fun getIdentityPriv(context: Context, pin: String): String? {
        val priv = context.defaultSharedPreferences.getString(Constants.Tip.Identity_Priv, null)
        if (!priv.isNullOrBlank()) {
            return priv
        }

        val tipIdentity = handleMixinResponse(
            invokeNetwork = {
                tipService.tipIdentity()
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                requireNotNull(it.data) { "Required tipIdentity response data was null." }
            }
        ) ?: return null

        val argon2Kt = withContext(Dispatchers.Main) {
            Argon2Kt()
        }
        val hashResult: Argon2KtResult = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_I,
            password = tipIdentity.seedBase64.decodeBase64() + pin.toByteArray(),
            salt = "somesalt".toByteArray(),
            tCostInIterations = 1,
            mCostInKibibyte = 1024,
            hashLengthInBytes = 32
        )
        val privString = hashResult.rawHashAsByteArray().base64Encode()
        context.defaultSharedPreferences.putString(Constants.Tip.Identity_Priv, privString)
        return privString
    }
}

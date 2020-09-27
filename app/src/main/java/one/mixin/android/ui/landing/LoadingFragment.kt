package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.crypto.PrivacyPreference.putIsSyncSession
import one.mixin.android.crypto.calculateAgreement
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.reportException

@AndroidEntryPoint
class LoadingFragment : BaseFragment() {

    companion object {
        const val TAG: String = "LoadingFragment"

        fun newInstance() = LoadingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_loading, container, false)

    private val loadingViewModel by viewModels<LoadingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().onlining.set(true)
        lifecycleScope.launch {
            if (Session.shouldUpdateKey()) {
                updateRsa2EdDsa()
            }

            if (!getIsLoaded(requireContext(), false)) {
                load()
            }

            if (!getIsSyncSession(requireContext(), false)) {
                syncSession()
            }

            context?.let {
                MainActivity.show(it)
            }
            activity?.finish()
        }
    }

    private suspend fun updateRsa2EdDsa() {
        val sessionKey = generateEd25519KeyPair()
        val publicKey = sessionKey.public as EdDSAPublicKey
        val privateKey = sessionKey.private as EdDSAPrivateKey
        val sessionSecret = publicKey.abyte.base64Encode()

        while (true) {
            try {
                val response = loadingViewModel.modifySessionSecret(SessionSecretRequest(sessionSecret))
                if (response.isSuccess) {
                    response.data?.let { r ->
                        val account = Session.getAccount()
                        account?.let { acc ->
                            acc.pinToken = r.pinToken
                            val key = calculateAgreement(r.pinToken.decodeBase64(), privateKey) ?: return

                            Session.storeEd25519PrivateKey(privateKey.seed.base64Encode())
                            Session.storePinToken(key.base64Encode())
                            Session.storeAccount(acc)
                        }
                        return
                    }
                } else {
                    val code = response.errorCode
                    reportException("Update EdDSA key", IllegalStateException("errorCode: $code, errorDescription: ${response.errorDescription}"))
                    ErrorHandler.handleMixinError(code, response.errorDescription)

                    if (code == ErrorHandler.AUTHENTICATION || code == FORBIDDEN) {
                        defaultSharedPreferences.putBoolean(PREF_TRIED_UPDATE_KEY, true)
                        return
                    }
                }
            } catch (t: Throwable) {
                reportException("Update EdDSA key", t)
                ErrorHandler.handleError(t)
            }

            delay(2000)
        }
    }

    private suspend fun syncSession() {
        try {
            Session.deleteExtensionSessionId()
            loadingViewModel.updateSignalSession()
            putIsSyncSession(requireContext(), true)
        } catch (e: Exception) {
            ErrorHandler.handleError(e)
        }
    }

    private suspend fun load() {
        if (count > 0) {
            count--
            try {
                val response = loadingViewModel.pushAsyncSignalKeys()
                when {
                    response.isSuccess -> {
                        putIsLoaded(requireContext(), true)
                    }
                    response.errorCode == ErrorHandler.AUTHENTICATION -> {
                        withContext(Dispatchers.IO) {
                            MixinApplication.get().closeAndClear()
                        }
                        activity?.finish()
                    }
                    else -> load()
                }
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
                load()
            }
        }
    }

    private var count = 2
}

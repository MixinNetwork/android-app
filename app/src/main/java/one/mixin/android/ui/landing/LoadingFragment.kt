package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.crypto.PrivacyPreference.putIsSyncSession
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import one.mixin.android.Constants.Load.IS_UPDATE_KEY
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import org.whispersystems.curve25519.Curve25519

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
            if (Session.shouldUpdateKey() &&
                !defaultSharedPreferences.getBoolean(IS_UPDATE_KEY, false)
            ) {
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

        handleMixinResponse(
            invokeNetwork = {
                loadingViewModel.modifySessionSecret(SessionSecretRequest(sessionSecret))
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                it.data?.let { r ->
                    val account = Session.getAccount()
                    account?.let { acc ->
                        acc.pinToken = r.serverPublicKey
                        Session.storeAccount(acc)
                    }
                    Session.storeEd25519PrivateKey(privateKey.seed.base64Encode())
                    val key = Curve25519.getInstance(Curve25519.BEST).calculateAgreement(r.serverPublicKey.decodeBase64(), privateKeyToCurve25519(privateKey.seed))
                    Session.storePinToken(key.base64Encode())
                    requireContext().defaultSharedPreferences.putBoolean(IS_UPDATE_KEY, true)
                }
            }
        )
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

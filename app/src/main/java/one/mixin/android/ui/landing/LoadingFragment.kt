package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_LOGIN_VERIFY
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.Constants.TEAM_BOT_ID
import one.mixin.android.Constants.TEAM_BOT_NAME
import one.mixin.android.Constants.TEAM_MIXIN_USER_ID
import one.mixin.android.Constants.TEAM_MIXIN_USER_NAME
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.crypto.PrivacyPreference.putIsSyncSession
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.databinding.FragmentLoadingBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.InitializeJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.session.decryptPinToken
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipBundle
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.tip.TryConnecting
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class LoadingFragment : BaseFragment(R.layout.fragment_loading) {
    companion object {
        const val TAG: String = "LoadingFragment"

        fun newInstance() = LoadingFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val loadingViewModel by viewModels<LoadingViewModel>()
    private val binding by viewBinding(FragmentLoadingBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().isOnline.set(true)
        checkAndLoad()
    }

    private fun checkAndLoad() =
        lifecycleScope.launch {
            showLoading()

            if (Session.shouldUpdateKey()) {
                updateRsa2EdDsa()

                if (Session.shouldUpdateKey()) {
                    showRetry()
                    return@launch
                }
            }

            if (!getIsLoaded(requireContext(), false)) {
                load()

                if (!getIsLoaded(requireContext(), false)) {
                    showRetry()
                    return@launch
                }
            }

            if (!getIsSyncSession(requireContext(), false)) {
                syncSession()

                if (!getIsSyncSession(requireContext(), false)) {
                    showRetry()
                    return@launch
                }
            }

            jobManager.addJobInBackground(InitializeJob(TEAM_MIXIN_USER_ID, TEAM_MIXIN_USER_NAME))
            if (TEAM_BOT_ID.isNotEmpty()) {
                jobManager.addJobInBackground(InitializeJob(TEAM_BOT_ID, TEAM_BOT_NAME))
            }

            if (Session.hasSafe()) {
                defaultSharedPreferences.putBoolean(PREF_LOGIN_VERIFY, true)
                MainActivity.show(requireContext())
            } else {
                var deviceId = defaultSharedPreferences.getString(DEVICE_ID, null)
                if (deviceId == null) {
                    deviceId = requireActivity().getStringDeviceId()
                }
                val tipType = if (Session.getAccount()?.hasPin == true) TipType.Upgrade else TipType.Create
                TipActivity.show(requireActivity(), tipType, shouldWatch = true)
            }
            activity?.finish()
        }

    private suspend fun updateRsa2EdDsa() {
        val sessionKey = generateEd25519KeyPair()
        val publicKey = sessionKey.publicKey
        val privateKey = sessionKey.privateKey
        val sessionSecret = publicKey.base64Encode()

        while (true) {
            try {
                val response = loadingViewModel.modifySessionSecret(SessionSecretRequest(sessionSecret))
                if (response.isSuccess) {
                    response.data?.let { r ->
                        val account = Session.getAccount()
                        account?.let { acc ->
                            acc.pinToken = r.pinToken
                            val pinToken = decryptPinToken(r.pinToken.decodeBase64(), privateKey)
                            Session.storeEd25519Seed(privateKey.base64Encode())
                            Session.storePinToken(pinToken.base64Encode())
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
                reportException("$TAG Update EdDSA key", t)
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
            reportException("$TAG syncSession", e)
        }
    }

    private fun showRetry() {
        if (viewDestroyed()) return

        count = 2
        binding.apply {
            subTitle.isVisible = false
            pb.isVisible = false
            retryTv.isVisible = true
            retryTv.setOnClickListener {
                checkAndLoad()
            }
        }
    }

    private fun showLoading() {
        if (viewDestroyed()) return

        binding.apply {
            subTitle.isVisible = true
            pb.isVisible = true
            retryTv.isVisible = false
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
                reportException("$TAG pushAsyncSignalKeys", e)
                load()
            }
        }
    }

    private var count = 2
}

package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AnonymousMessage
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.doAnonymousPOW
import one.mixin.android.crypto.CryptoPreference
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clear
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.hexString
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putString
import one.mixin.android.extension.toHex
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.session.decryptPinToken
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhrasePage
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.database.clearDatabase
import one.mixin.android.util.database.clearJobsAndRawTransaction
import one.mixin.android.util.database.getLastUserId
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.toUser
import one.mixin.android.widget.CaptchaView
import timber.log.Timber

@AndroidEntryPoint
class MnemonicPhraseFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): MnemonicPhraseFragment =
            MnemonicPhraseFragment().apply {

            }
    }

    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentComposeBinding::bind)
    private var errorInfo: String? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.compose.setContent {
            MnemonicPhrasePage(errorInfo) {
                anonymousRequest()
            }
        }
    }

    private fun anonymousRequest() {
        lifecycleScope.launch {
            mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
            val sessionKey = generateEd25519KeyPair()
            val publicKey = sessionKey.publicKey
            val message = AnonymousMessage("", nowInUtc()).doAnonymousPOW()
            val messageHex = GsonHelper.customGson.toJson(message).toHex()
            val signature = initFromSeedAndSign(sessionKey.privateKey.toTypedArray().toByteArray(), GsonHelper.customGson.toJson(message).toByteArray())
            val r = handleMixinResponse(
                invokeNetwork = {
                    mobileViewModel.anonymousRequest(publicKey.hexString(), messageHex, signature.hexString())
                },

                successBlock = { r ->
                    r
                },

                exceptionBlock = { t ->
                    mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    errorInfo = t.message
                    Timber.e(t)
                    true
                },

                failureBlock = { r ->
                    if (r.errorCode == NEED_CAPTCHA) {
                        initAndLoadCaptcha(sessionKey, publicKey.hexString(), messageHex, signature.hexString())
                    } else {
                        errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                        mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    }
                    true
                }
            )

            if (r?.isSuccess == true) {
                createAccount(sessionKey, r.data!!.id)
            }
        }
    }

    private var captchaView: CaptchaView? = null
    private fun initAndLoadCaptcha(key: EdKeyPair, publicKeyHex: String, messageHex: String, signatureHex: String) =
        lifecycleScope.launch {
            if (captchaView == null) {
                captchaView =
                    CaptchaView(
                        requireContext(),
                        object : CaptchaView.Callback {
                            override fun onStop() {
                                if (viewDestroyed()) return
                                binding.mobileCover.isVisible = false
                            }

                            override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                                val t = value.second
                                reSend(key, publicKeyHex, messageHex, signatureHex, if (!value.first.isG()) t else null, if (value.first.isG()) t else null)
                            }
                        },
                    )
                (view as ViewGroup).addView(captchaView?.webView, MATCH_PARENT, MATCH_PARENT)
            }
            captchaView?.loadCaptcha(CaptchaView.CaptchaType.GCaptcha)
        }

    private fun reSend(key: EdKeyPair, publicKeyHex: String, messageHex: String, signatureHex: String, hCaptchaResponse: String? = null, gRecaptchaResponse: String? = null) {
        lifecycleScope.launch {
            val r = handleMixinResponse(
                invokeNetwork = {
                    mobileViewModel.anonymousRequest(publicKeyHex, messageHex, signatureHex, hCaptchaResponse, gRecaptchaResponse)
                },

                successBlock = { r ->
                    r
                },

                exceptionBlock = { t ->
                    mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    errorInfo = t.message
                    Timber.e(t)
                    true
                },

                failureBlock = { r ->
                    errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                    mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    true
                }
            )

            if (r?.isSuccess == true) {
                createAccount(key, r.data!!.id)
            } else {
                mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Initial)
            }
        }
    }

    private fun createAccount(key: EdKeyPair, verificationId: String) {
        lifecycleScope.launch {
            SignalProtocol.initSignal(requireContext().applicationContext)
            val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
            val sessionSecret = key.publicKey.base64Encode()
            val r = handleMixinResponse(
                invokeNetwork = {
                    mobileViewModel.create(
                        verificationId,
                        AccountRequest(
                            purpose = VerificationPurpose.ANONYMOUS_SESSION.name,
                            session_secret = sessionSecret,
                            signature_hex = initFromSeedAndSign(key.privateKey.toTypedArray().toByteArray(), verificationId.toByteArray()).toHex(),
                            registration_id = registrationId,
                        )
                    )
                },

                successBlock = { r ->
                    r
                },

                exceptionBlock = { t ->
                    mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    errorInfo = t.message
                    Timber.e(t)
                    true
                },

                failureBlock = { r ->
                    errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                    mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    true
                }
            )
            if (r?.isSuccess == true) {
                val account = r.data!!
                val lastUserId = getLastUserId(requireContext())
                val sameUser = lastUserId != null && lastUserId == account.userId
                if (sameUser) {
                    clearJobsAndRawTransaction(requireContext())
                } else {
                    clearDatabase(requireContext())
                    defaultSharedPreferences.clear()
                }
                val privateKey = key.privateKey
                val pinToken = decryptPinToken(account.pinToken.decodeBase64(), privateKey)
                Session.storeEd25519Seed(privateKey.base64Encode())
                Session.storePinToken(pinToken.base64Encode())
                Session.storeAccount(account)
                defaultSharedPreferences.putString(DEVICE_ID, requireContext().getStringDeviceId())

                MixinApplication.get().isOnline.set(true)

                when {
                    account.fullName.isNullOrBlank() -> {
                        mobileViewModel.upsertUser(account.toUser())
                        InitializeActivity.showSetupName(requireContext())
                    }
                    else -> {
                        RestoreActivity.show(requireContext())
                    }
                }
                mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Success)
                activity?.finish()
            } else {
                mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Initial)
            }
        }
    }
}
package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AnonymousMessage
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.doAnonymousPOW
import one.mixin.android.crypto.CryptoPreference
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.getValueFromEncryptedPreferences
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromMnemonic
import one.mixin.android.crypto.storeValueInEncryptedPreferences
import one.mixin.android.crypto.toEntropy
import one.mixin.android.crypto.toMnemonic
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clear
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.hexString
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putString
import one.mixin.android.extension.toHex
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.session.decryptPinToken
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhrasePage
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.database.clearDatabase
import one.mixin.android.util.database.clearJobsAndRawTransaction
import one.mixin.android.util.database.getLastUserId
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.toUser
import one.mixin.android.widget.CaptchaView
import one.mixin.android.widget.CaptchaView.Companion.gtCAPTCHA
import one.mixin.android.widget.CaptchaView.Companion.hCAPTCHA
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MnemonicPhraseFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"
        const val ARGS_MNEMONIC_PHRASE = "mnemonic_phrase"

        fun newInstance(
            words: ArrayList<String>? = null
        ): MnemonicPhraseFragment =
            MnemonicPhraseFragment().apply {
                withArgs {
                    putStringArrayList(ARGS_MNEMONIC_PHRASE, words)
                }
            }
    }

    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentComposeBinding::bind)
    private var errorInfo by mutableStateOf<String?>(null)

    @Inject
    lateinit var tip: Tip

    private val words by lazy {
        requireArguments().getStringArrayList(ARGS_MNEMONIC_PHRASE)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        Timber.e("MnemonicPhraseFragment onViewCreated")
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.compose.setContent {
            MnemonicPhrasePage(!words.isNullOrEmpty(), errorInfo) {
                anonymousRequest(words)
            }
        }
        anonymousRequest(words)
    }

    private fun applySafeTopPadding(rootView: View) {
        val originalPaddingTop: Int = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, insets: WindowInsetsCompat ->
            val topInset: Int = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            v.setPadding(v.paddingLeft, originalPaddingTop + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun anonymousRequest(words: List<String>? = null) {
        lifecycleScope.launch {
            mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
            val sessionKey = generateEd25519KeyPair()
            val edKey = if (!words.isNullOrEmpty()) {
                val w = words.let {
                    when (words.size) {
                        13 -> {
                            words.subList(0, 12)
                        }

                        25 -> {
                            words.subList(0, 24)
                        }

                        else -> {
                            errorInfo = getString(R.string.invalid_mnemonic_phrase)
                            throw IllegalArgumentException("Invalid mnemonic")
                        }
                    }
                }
                val mnemonic = w.joinToString(" ")
                val entropy  = runCatching { toEntropy(w)}.onFailure { errorInfo = getString(R.string.invalid_mnemonic_phrase) }.getOrNull() ?: return@launch
                storeValueInEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC, entropy)
                if (getValueFromEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC).contentEquals(entropy).not()) {
                    errorInfo = getString(R.string.Save_failure) // Save entropy failure
                    return@launch
                }
                newKeyPairFromMnemonic(mnemonic)
            } else {
                val mnemonic = toMnemonic(tip.generateEntropyAndStore(requireContext()))
                val entropy  = runCatching { toEntropy(mnemonic.split(" "))}.getOrNull() ?: return@launch
                if (getValueFromEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC).contentEquals(entropy).not()) {
                    errorInfo = getString(R.string.Save_failure) // Save entropy failure
                    return@launch
                }
                newKeyPairFromMnemonic(mnemonic)
            }
            Timber.e("PublicKey:${edKey.publicKey.hexString()}")
            val message = withContext(Dispatchers.IO) {
                AnonymousMessage(createdAt = nowInUtc(), masterPublicHex = edKey.publicKey.hexString()).doAnonymousPOW()
            }
            val messageHex = GsonHelper.customGson.toJson(message).toHex()
            val signature = initFromSeedAndSign(edKey.privateKey.toTypedArray().toByteArray(), GsonHelper.customGson.toJson(message).toByteArray())
            val r = handleMixinResponse(
                invokeNetwork = {
                    mobileViewModel.anonymousRequest(edKey.publicKey.hexString(), messageHex, signature.hexString())
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
                        if (words.isNullOrEmpty()) {
                            AnalyticsTracker.trackSignUpCaptcha("mnemonic_phrase")
                        } else {
                            AnalyticsTracker.trackLoginCaptcha("mnemonic_phrase")
                        }
                        initAndLoadCaptcha(sessionKey, edKey, messageHex, signature.hexString(), r.errorDescription)
                    } else {
                        errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                        mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    }
                    true
                }
            )

            if (r?.isSuccess == true) {
                if (!r.data?.deactivationEffectiveAt.isNullOrBlank() && !words.isNullOrEmpty()) {
                    LandingDeleteAccountFragment.newInstance(r.data?.deactivationRequestedAt, r.data?.deactivationEffectiveAt)
                        .setContinueCallback {
                            createAccount(sessionKey, edKey, r.data!!.id)
                        }.showNow(parentFragmentManager, LandingDeleteAccountFragment.TAG)
                } else {
                    createAccount(sessionKey, edKey, r.data!!.id)
                }
            } else if (r != null) {
                errorInfo = requireActivity().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
            }
        }
    }

    private var captchaView: CaptchaView? = null
    private fun initAndLoadCaptcha(sessionKey: EdKeyPair, edKey: EdKeyPair, messageHex: String, signatureHex: String, errorDescription: String) =
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
                                reSend(sessionKey, edKey, messageHex, signatureHex, if (value.first.isH()) t else null, if (value.first.isG()) t else null, if (value.first.isGT()) t else null)
                            }
                        },
                    )
                (view as ViewGroup).addView(captchaView?.webView, MATCH_PARENT, MATCH_PARENT)
            }
            captchaView?.loadCaptcha(
                if (errorDescription.containsIgnoreCase(gtCAPTCHA)) CaptchaView.CaptchaType.GTCaptcha
                else if (errorDescription.containsIgnoreCase(hCAPTCHA)) CaptchaView.CaptchaType.HCaptcha
                else CaptchaView.CaptchaType.GCaptcha
            )
        }

    private fun reSend(sessionKey: EdKeyPair, edKey: EdKeyPair, messageHex: String, signatureHex: String, hCaptchaResponse: String? = null, gRecaptchaResponse: String? = null, gtRecaptchaResponse: String? = null) {
        lifecycleScope.launch {
            val r = handleMixinResponse(
                invokeNetwork = {
                    mobileViewModel.anonymousRequest(edKey.publicKey.hexString(), messageHex, signatureHex, hCaptchaResponse, gRecaptchaResponse, gtRecaptchaResponse)
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
                        mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
                        initAndLoadCaptcha(sessionKey, edKey, messageHex, signatureHex, r.errorDescription)
                    } else {
                        errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                        mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    }
                    true
                }
            )

            if (r?.isSuccess == true) {
                if (!r.data?.deactivationEffectiveAt.isNullOrBlank() && !words.isNullOrEmpty()) {
                    LandingDeleteAccountFragment.newInstance(r.data?.deactivationRequestedAt, r.data?.deactivationEffectiveAt)
                        .setContinueCallback {
                            createAccount(sessionKey, edKey, r.data!!.id)
                        }.showNow(parentFragmentManager, LandingDeleteAccountFragment.TAG)
                } else {
                    createAccount(sessionKey, edKey, r.data!!.id)
                }
            } else {
                if (r != null) {
                    if (r.errorCode == NEED_CAPTCHA) {
                        mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
                        initAndLoadCaptcha(sessionKey, edKey, messageHex, signatureHex, r.errorDescription)
                        return@launch
                    }
                    errorInfo = requireActivity().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                }
                mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
            }
        }
    }

    private fun createAccount(sessionKey: EdKeyPair, edKey: EdKeyPair,verificationId: String) {
        lifecycleScope.launch {
            SignalProtocol.initSignal(requireContext().applicationContext)
            val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
            val sessionSecret = sessionKey.publicKey.base64Encode()
            val r = handleMixinResponse(
                invokeNetwork = {
                    mobileViewModel.create(
                        verificationId,
                        AccountRequest(
                            purpose = VerificationPurpose.ANONYMOUS_SESSION.name,
                            sessionSecret = sessionSecret,
                            masterSignatureHex = initFromSeedAndSign(edKey.privateKey.toTypedArray().toByteArray(), verificationId.toByteArray()).toHex(),
                            registrationId = registrationId,
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
                    withContext(Dispatchers.IO) {
                        clearJobsAndRawTransaction(requireContext())
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        clearDatabase(requireContext())
                    }
                    CryptoWalletHelper.clear(requireContext())
                    defaultSharedPreferences.clear()
                }
                val privateKey = sessionKey.privateKey
                val pinToken = decryptPinToken(account.pinToken.decodeBase64(), privateKey)
                Session.storeEd25519Seed(privateKey.base64Encode())
                Session.storePinToken(pinToken.base64Encode())
                Session.storeAccount(account)
                defaultSharedPreferences.putString(DEVICE_ID, requireContext().getStringDeviceId())
                when {
                    account.fullName.isNullOrBlank() -> {
                        mobileViewModel.insertUser(account.toUser())
                        InitializeActivity.showSetupName(requireContext())
                    }

                    else -> {
                        RestoreActivity.show(requireContext())
                    }
                }
                mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Success)
                MixinApplication.get().reject()
                activity?.finish()
            } else {
                if (r != null) {
                    errorInfo = requireActivity().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                }
                mobileViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
            }
        }
    }
}
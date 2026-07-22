package one.mixin.android.ui.landing

import android.content.ClipData
import android.os.Bundle
import android.os.Build
import android.view.View
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
import one.mixin.android.crypto.clearPendingImportMnemonic
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.getValueFromEncryptedPreferences
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.markPendingImportMnemonic
import one.mixin.android.crypto.newKeyPairFromMnemonic
import one.mixin.android.crypto.preparePendingImportMnemonic
import one.mixin.android.crypto.storeValueInEncryptedPreferences
import one.mixin.android.crypto.toEntropy
import one.mixin.android.crypto.toMnemonic
import one.mixin.android.crypto.toMnemonicWithChecksum
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hexString
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toHex
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.session.initializeAccountSession
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhrasePage
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.toUser
import one.mixin.android.widget.CaptchaView
import one.mixin.android.widget.CaptchaView.Companion.gtCAPTCHA
import one.mixin.android.widget.CaptchaView.Companion.hCAPTCHA
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class MnemonicPhraseFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"
        const val ARGS_MNEMONIC_PHRASE = "mnemonic_phrase"
        private const val ARGS_PENDING_IMPORT_MNEMONIC = "pending_import_mnemonic"
        private const val ARGS_PASTED_MNEMONIC = "pasted_mnemonic"
        private const val STATE_ERROR_INFO = "error_info"

        fun newInstance(
            words: ArrayList<String>? = null,
            pendingImportWords: ArrayList<String>? = null,
            pastedMnemonicWords: ArrayList<String>? = null,
        ): MnemonicPhraseFragment =
            MnemonicPhraseFragment().apply {
                withArgs {
                    putStringArrayList(ARGS_MNEMONIC_PHRASE, words)
                    putStringArrayList(ARGS_PENDING_IMPORT_MNEMONIC, pendingImportWords)
                    putStringArrayList(ARGS_PASTED_MNEMONIC, pastedMnemonicWords)
                }
            }
    }

    private val landingViewModel by viewModels<LandingViewModel>()
    private val binding by viewBinding(FragmentComposeBinding::bind)
    private var errorInfo by mutableStateOf<String?>(null)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var userRepositoryProvider: Provider<UserRepository>

    private val words by lazy {
        requireArguments().getStringArrayList(ARGS_MNEMONIC_PHRASE)
    }
    private val pendingImportWords by lazy {
        requireArguments().getStringArrayList(ARGS_PENDING_IMPORT_MNEMONIC)
    }
    private val pastedMnemonicWords by lazy {
        requireArguments().getStringArrayList(ARGS_PASTED_MNEMONIC)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        Timber.i("LoginFlow account_creation_open source=${if (words.isNullOrEmpty()) "signup" else "mnemonic_login"} pending_import=${!pendingImportWords.isNullOrEmpty()}")
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.titleView.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        errorInfo = savedInstanceState?.getString(STATE_ERROR_INFO)
        binding.compose.setContent {
            MnemonicPhrasePage(!words.isNullOrEmpty(), errorInfo) {
                anonymousRequest(words)
            }
        }
        if (shouldRequestAnonymousLogin(errorInfo)) {
            anonymousRequest(words)
        } else {
            landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        errorInfo?.let { outState.putString(STATE_ERROR_INFO, it) }
    }

    private fun applySafeTopPadding(rootView: View) {
        val originalPaddingTop: Int = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, insets: WindowInsetsCompat ->
            val topInset: Int = maxOf(
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top,
            )
            v.setPadding(v.paddingLeft, originalPaddingTop + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun anonymousRequest(words: List<String>? = null) {
        lifecycleScope.launch {
            errorInfo = null
            landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
            val sessionKey = generateEd25519KeyPair()
            val edKey = if (!words.isNullOrEmpty()) {
                val completedWords = runCatching {
                    completeMnemonicForLogin(words) { sourceWords ->
                        toMnemonicWithChecksum(sourceWords)
                    }
                }.onFailure {
                    errorInfo = getString(R.string.invalid_mnemonic_phrase)
                }.getOrNull() ?: return@launch
                val w = completedWords.let {
                    when (completedWords.size) {
                        13 -> {
                            completedWords.subList(0, 12)
                        }

                        25 -> {
                            completedWords.subList(0, 24)
                        }

                        else -> {
                            errorInfo = getString(R.string.invalid_mnemonic_phrase)
                            throw IllegalArgumentException("Invalid mnemonic")
                        }
                    }
                }
                val mnemonic = w.joinToString(" ")
                runCatching { toEntropy(w) }.onFailure { errorInfo = getString(R.string.invalid_mnemonic_phrase) }.getOrNull() ?: return@launch
                newKeyPairFromMnemonic(mnemonic)
            } else {
                val entropy = tip.getMnemonicFromEncryptedPreferences(requireContext()) ?: tip.generateEntropyAndStore(requireContext())
                val mnemonic = runCatching {
                    toMnemonic(entropy)
                }.onFailure {
                    errorInfo = getString(R.string.invalid_mnemonic_phrase)
                }.getOrNull() ?: return@launch
                val verifiedEntropy  = runCatching { toEntropy(mnemonic.split(" "))}.getOrNull() ?: return@launch
                if (getValueFromEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC).contentEquals(verifiedEntropy).not()) {
                    errorInfo = getString(R.string.Save_failure) // Save entropy failure
                    return@launch
                }
                newKeyPairFromMnemonic(mnemonic)
            }
            val (messageHex, signatureHex) = buildAnonymousRequestPayload(edKey)
            var needCaptcha = false
            val r = handleMixinResponse(
                invokeNetwork = {
                    landingViewModel.anonymousRequest(edKey.publicKey.hexString(), messageHex, signatureHex)
                },

                successBlock = { r ->
                    r
                },

                exceptionBlock = { t ->
                    landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    errorInfo = ErrorHandler.getErrorMessage(t)
                    Timber.e(t)
                    true
                },

                failureBlock = { r ->
                    if (r.errorCode == NEED_CAPTCHA) {
                        needCaptcha = true
                        errorInfo = null
                        landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
                        if (words.isNullOrEmpty()) {
                            AnalyticsTracker.trackSignUpCaptcha()
                        } else {
                            AnalyticsTracker.trackLoginCaptcha("mnemonic_phrase")
                        }
                        initAndLoadCaptcha(sessionKey, edKey, r.errorDescription)
                    } else {
                        errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                        landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
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
            } else if (needCaptcha) {
                return@launch
            }
        }
    }

    private var captchaView: CaptchaView? = null

    override fun onDestroyView() {
        captchaView?.release()
        captchaView = null
        super.onDestroyView()
    }

    private fun initAndLoadCaptcha(sessionKey: EdKeyPair, edKey: EdKeyPair, errorDescription: String) =
        lifecycleScope.launch {
            errorInfo = null
            landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
            if (captchaView == null) {
                captchaView =
                    CaptchaView(
                        requireContext(),
                        object : CaptchaView.Callback {
                            override fun onStop() {
                                if (viewDestroyed()) return
                                binding.mobileCover.isVisible = false
                                landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                            }

                            override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                                val t = value.second
                                reSend(sessionKey, edKey, if (value.first.isH()) t else null, if (value.first.isG()) t else null, if (value.first.isGT()) t else null)
                            }
                        },
                    )
            }
            captchaView?.loadCaptcha(
                if (errorDescription.containsIgnoreCase(gtCAPTCHA)) CaptchaView.CaptchaType.GTCaptcha
                else if (errorDescription.containsIgnoreCase(hCAPTCHA)) CaptchaView.CaptchaType.HCaptcha
                else CaptchaView.CaptchaType.GCaptcha
            )
        }

    private suspend fun buildAnonymousRequestPayload(edKey: EdKeyPair): Pair<String, String> =
        withContext(Dispatchers.Default) {
            val message = AnonymousMessage(
                createdAt = nowInUtc(),
                masterPublicHex = edKey.publicKey.hexString(),
            ).doAnonymousPOW()
            val messageJson = GsonHelper.customGson.toJson(message)
            val messageHex = messageJson.toHex()
            val signatureHex = initFromSeedAndSign(
                edKey.privateKey.toTypedArray().toByteArray(),
                messageJson.toByteArray(),
            ).hexString()
            messageHex to signatureHex
        }

    private fun reSend(sessionKey: EdKeyPair, edKey: EdKeyPair, hCaptchaResponse: String? = null, gRecaptchaResponse: String? = null, gtRecaptchaResponse: String? = null) {
        lifecycleScope.launch {
            errorInfo = null
            landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
            val (messageHex, signatureHex) = buildAnonymousRequestPayload(edKey)
            var needCaptcha = false
            val r = handleMixinResponse(
                invokeNetwork = {
                    landingViewModel.anonymousRequest(edKey.publicKey.hexString(), messageHex, signatureHex, hCaptchaResponse, gRecaptchaResponse, gtRecaptchaResponse)
                },

                successBlock = { r ->
                    r
                },

                exceptionBlock = { t ->
                    landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    errorInfo = ErrorHandler.getErrorMessage(t)
                    Timber.e(t)
                    true
                },

                failureBlock = { r ->
                    if (r.errorCode == NEED_CAPTCHA) {
                        needCaptcha = true
                        errorInfo = null
                        landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Creating)
                        initAndLoadCaptcha(sessionKey, edKey, r.errorDescription)
                    } else {
                        errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                        landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
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
            } else if (needCaptcha) {
                return@launch
            } else {
                landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
            }
        }
    }

    private fun clearPastedMnemonicFromClipboard() {
        val pastedWords = pastedMnemonicWords ?: return
        val clipboard = requireContext().getClipboardManager()
        val clipboardWords = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter(String::isNotBlank)
            ?: return
        if (clipboardWords != pastedWords) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun createAccount(sessionKey: EdKeyPair, edKey: EdKeyPair, verificationId: String) {
        lifecycleScope.launch {
            SignalProtocol.initSignal(requireContext().applicationContext)
            val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
            val sessionSecret = sessionKey.publicKey.base64Encode()
            val r = handleMixinResponse(
                invokeNetwork = {
                    landingViewModel.create(
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
                    landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    errorInfo = ErrorHandler.getErrorMessage(t)
                    Timber.e(t)
                    true
                },

                failureBlock = { r ->
                    errorInfo = requireContext().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                    landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                    true
                }
            )
            if (r?.isSuccess == true) {
                val account = r.data!!
                Timber.i(
                    "LoginFlow anonymous_account_success source=${if (words.isNullOrEmpty()) "signup" else "mnemonic_login"} has_full_name=${!account.fullName.isNullOrBlank()} pending_import=${!pendingImportWords.isNullOrEmpty()}"
                )
                if (words.isNullOrEmpty() || account.fullName.isNullOrBlank()) {
                    AnalyticsTracker.trackSignUpAccountCreated()
                }
                initializeAccountSession(requireContext(), account, sessionKey)
                val importWords = pendingImportWords
                val hasPendingWalletImport = !importWords.isNullOrEmpty()
                if (shouldStoreLoginMnemonicForSafe(account.hasSafe, Session.hasPhone(), hasPendingWalletImport) && !words.isNullOrEmpty()) {
                    val loginWords = words.orEmpty().dropLast(1)
                    val entropy = runCatching { toEntropy(loginWords) }.getOrNull()
                    if (entropy == null) {
                        errorInfo = getString(R.string.invalid_mnemonic_phrase)
                        landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                        return@launch
                    }
                    storeValueInEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC, entropy)
                    if (!getValueFromEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC).contentEquals(entropy)) {
                        errorInfo = getString(R.string.Save_failure)
                        landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
                        return@launch
                    }
                }
                if (hasPendingWalletImport) {
                    preparePendingImportMnemonic(importWords.orEmpty())
                    markPendingImportMnemonic(requireContext())
                } else {
                    clearPendingImportMnemonic(requireContext())
                }
                clearPastedMnemonicFromClipboard()
                val hasFullName = !account.fullName.isNullOrBlank()
                val localAccountDatabaseExists = hasLocalAccountDatabase(requireContext(), account.identityNumber)
                val loginAccountRoute = routeLoginAccount(hasFullName, localAccountDatabaseExists)
                Timber.i(
                    "LoginFlow account_route source=mnemonic route=$loginAccountRoute has_full_name=$hasFullName has_local_database=$localAccountDatabaseExists pending_import=${!importWords.isNullOrEmpty()}"
                )
                when (loginAccountRoute) {
                    LoginAccountRoute.SetupName -> {
                        withContext(Dispatchers.IO) {
                            userRepositoryProvider.get().insertUser(account.toUser())
                        }
                        InitializeActivity.showSetupName(requireContext())
                    }

                    LoginAccountRoute.UseLocalDatabase -> {
                        defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                        InitializeActivity.showLoading(requireContext(), source = InitializeActivity.SOURCE_LOGIN)
                    }

                    LoginAccountRoute.Restore -> {
                        RestoreActivity.show(requireContext())
                    }
                }
                landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Success)
                MixinApplication.get().reject()
                activity?.finish()
            } else {
                if (r != null) {
                    errorInfo = requireActivity().getMixinErrorStringByCode(r.errorCode, r.errorDescription)
                }
                landingViewModel.updateMnemonicPhraseState(MnemonicPhraseState.Failure)
            }
        }
    }
}

internal fun shouldRequestAnonymousLogin(restoredErrorInfo: String?) = restoredErrorInfo == null

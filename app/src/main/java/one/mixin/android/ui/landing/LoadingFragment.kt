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
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Account.PREF_LOGIN_OR_SIGN_UP
import one.mixin.android.Constants.Account.PREF_LOGIN_VERIFY
import one.mixin.android.Constants.Account.PREF_TRIED_UPDATE_KEY
import one.mixin.android.Constants.DEFAULT_BOTS
import one.mixin.android.Constants.DEFAULT_CN_BOTS
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.crypto.PrivacyPreference.putIsSyncSession
import one.mixin.android.crypto.clearPendingImportMnemonic
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.crypto.hasPendingImportMnemonic
import one.mixin.android.databinding.FragmentLoadingBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.InitializeJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.session.decryptPinToken
import one.mixin.android.tip.Tip
import one.mixin.android.tip.getSpendKeyFromPin
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.LoginVerifyBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.tip.PendingMnemonicResolution
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.tip.resolvePendingMnemonicAfterWalletFetch
import one.mixin.android.ui.wallet.WalletSecurityActivity
import one.mixin.android.ui.wallet.components.walletDestinationForWallet
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.isSimplifiedChineseLocale
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WalletCategory
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoadingFragment : BaseFragment(R.layout.fragment_loading) {
    companion object {
        const val TAG: String = "LoadingFragment"
        private const val ARGS_SOURCE = "args_source"

        fun newInstance(source: String? = null) = LoadingFragment().apply {
            arguments = Bundle().apply {
                source?.let { putString(ARGS_SOURCE, it) }
            }
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var web3Repository: Web3Repository

    @Inject
    lateinit var tip: Tip

    private val loadingViewModel by viewModels<LoadingViewModel>()
    private val binding by viewBinding(FragmentLoadingBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadingTitle.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        MixinApplication.get().isOnline.set(true)
        when (arguments?.getString(ARGS_SOURCE)) {
            InitializeActivity.SOURCE_SIGN_UP -> AnalyticsTracker.trackSignUpSignalInit()
            InitializeActivity.SOURCE_LOGIN -> AnalyticsTracker.trackLoginSignalInit()
        }
        findLoginPinGate()?.let(::bindLoginPinGate)
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
            initializeBots()
            val hasSafe = Session.hasSafe()
            val hasPin = Session.getAccount()?.hasPin == true
            val loginPostGateRoute = routeLoginPostGate(hasSafe, hasPin)
            Timber.i(
                "LoginFlow post_login_gate route=$loginPostGateRoute has_safe=$hasSafe has_pin=$hasPin pending_import=${hasPendingImportMnemonic(requireContext())}"
            )
            when (loginPostGateRoute) {
                LoginPostGateRoute.VerifyPin -> {
                    showLoginPinGate()
                    return@launch
                }
                LoginPostGateRoute.SetupPin -> {
                    ensureDeviceId()
                    InitializeActivity.showSetupPin(requireActivity())
                }
                LoginPostGateRoute.UpgradeTip -> {
                    ensureDeviceId()
                    TipActivity.show(requireActivity(), TipType.Upgrade, shouldWatch = true)
                }
            }
            activity?.finish()
        }

    private fun showLoginPinGate() {
        defaultSharedPreferences.putBoolean(PREF_LOGIN_OR_SIGN_UP, true)
        defaultSharedPreferences.putBoolean(PREF_LOGIN_VERIFY, false)
        reuseOrCreateLoginPinGate(
            existing = findLoginPinGate(),
            create = LoginVerifyBottomSheetDialogFragment::newInstance,
            bind = ::bindLoginPinGate,
            show = { dialog ->
                dialog.showNow(parentFragmentManager, LoginVerifyBottomSheetDialogFragment.TAG)
            },
        )
    }

    private fun findLoginPinGate(): LoginVerifyBottomSheetDialogFragment? =
        parentFragmentManager.findFragmentByTag(LoginVerifyBottomSheetDialogFragment.TAG) as? LoginVerifyBottomSheetDialogFragment

    private fun bindLoginPinGate(dialog: LoginVerifyBottomSheetDialogFragment) {
        dialog.onDismissCallback = loginPinGateDismissCallback(
            ownerScope = this@LoadingFragment.lifecycleScope,
            openNext = { pin ->
                Timber.i("LoginFlow login_pin_gate_result success=true pending_import=${hasPendingImportMnemonic(requireContext())}")
                openNextAfterPin(pin)
            },
            finish = { activity?.finish() },
        )
    }

    private suspend fun openNextAfterPin(pin: String?): Boolean {
        return if (hasPendingImportMnemonic(requireContext())) {
            openPendingMnemonicNext(pin)
        } else {
            MainActivity.show(requireContext())
            true
        }
    }

    private suspend fun openPendingMnemonicNext(pin: String?): Boolean {
        val context = requireContext()
        val wallets = web3Repository.syncWalletsFromRoute()
        val pendingWords = if (pin == null) {
            null
        } else {
            runCatching { tip.getPendingImportMnemonic(context, pin) }
                .onFailure { Timber.e(it, "Failed to restore pending mnemonic from Safe") }
                .getOrNull()
                ?: return false
        }
        val resolution = resolvePendingMnemonicAfterWalletFetch(
            wallets = wallets,
            pin = pin,
            pendingWords = pendingWords,
            walletAddresses = { wallet -> web3Repository.getAddresses(wallet.id) },
            save = { walletId, verifiedPin, words ->
                CryptoWalletHelper.saveMnemonicWithSpendKey(
                    context,
                    tip.getSpendKeyFromPin(context, verifiedPin),
                    walletId,
                    words,
                )
            },
            savePrivateKey = { walletId, verifiedPin, privateKey ->
                CryptoWalletHelper.savePrivateKeyWithSpendKey(
                    context,
                    tip.getSpendKeyFromPin(context, verifiedPin),
                    walletId,
                    privateKey,
                )
            },
            clear = {
                clearPendingImportMnemonic(context)
                Timber.i("LoginFlow pending_import_cleared source=loading")
            },
        )
        Timber.i(
            "LoginFlow pending_import_wallet_sync_result source=loading resolution=$resolution wallet_count=${wallets?.size ?: -1}"
        )
        return when (resolution) {
            is PendingMnemonicResolution.WalletHome -> {
                val walletDestination = walletDestinationForWallet(
                    resolution.walletId,
                    resolution.walletCategory,
                )
                MainActivity.showWallet(context, walletDestination = walletDestination)
                true
            }
            PendingMnemonicResolution.ImportMnemonic -> {
                Timber.i("LoginFlow pending_import_fetch_open source=loading pin_reused=${pin != null}")
                WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC, pin = pin)
                true
            }
            PendingMnemonicResolution.NeedPin -> {
                showRetry { showLoginPinGate() }
                false
            }
            PendingMnemonicResolution.LocalSaveFailed -> {
                toast(R.string.Save_failure)
                showRetry {
                    lifecycleScope.launch {
                        showLoading()
                        if (openPendingMnemonicNext(pin)) {
                            activity?.finish()
                        }
                    }
                }
                false
            }
        }
    }

    private fun ensureDeviceId() {
        if (defaultSharedPreferences.getString(DEVICE_ID, null) == null) {
            defaultSharedPreferences.putString(DEVICE_ID, requireActivity().getStringDeviceId())
        }
    }

    private fun initializeBots() {
        val phone = Session.getAccount()?.phone.orEmpty()
        val testAccountPrefix = BuildConfig.TEST_ACCOUNT_PREFIX
        if (testAccountPrefix.isNotBlank() && phone.startsWith(testAccountPrefix)) {
            return
        }

        val bots = if (isSimplifiedChineseLocale()) DEFAULT_CN_BOTS else DEFAULT_BOTS
        bots.forEach { botId ->
            jobManager.addJobInBackground(InitializeJob(botId))
        }
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
                    Timber.e("errorCode: $code, errorDescription: ${response.errorDescription}")
                    ErrorHandler.handleMixinError(code, response.errorDescription)

                    if (code == ErrorHandler.AUTHENTICATION || code == FORBIDDEN) {
                        defaultSharedPreferences.putBoolean(PREF_TRIED_UPDATE_KEY, true)
                        return
                    }
                }
            } catch (t: Throwable) {
                reportException("$TAG Update EdDSA key", t)
                Timber.e(t)
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
            Timber.e(e)
        }
    }

    private fun showRetry(onRetry: () -> Unit = { checkAndLoad() }) {
        if (viewDestroyed()) return

        count = 2
        binding.apply {
            subTitle.isVisible = false
            pb.isVisible = false
            retryTv.isVisible = true
            retryTv.setOnClickListener {
                onRetry()
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

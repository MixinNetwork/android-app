package one.mixin.android.ui.wallet

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.crypto.hasPendingImportMnemonic
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.WalletCategory
import timber.log.Timber

@AndroidEntryPoint
class WalletSecurityActivity : BlazeBaseActivity() {
    private val mode: Mode by lazy {
        val modeOrdinal = intent.getIntExtra(EXTRA_MODE, Mode.IMPORT_MNEMONIC.ordinal)
        Mode.entries[modeOrdinal]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mode.requiresSecureWindow()) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContentView(R.layout.activity_add_wallet)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (shouldBlockWalletSecurityBack(mode, isImportStep = isPendingMnemonicImportStep())) {
                        Timber.i("LoginFlow wallet_security_back_ignored mode=$mode")
                    } else {
                        finish()
                    }
                }
            },
        )
        if (savedInstanceState == null) {
            val chainId = intent.getStringExtra(EXTRA_CHAIN_ID)
            val walletId = intent.getStringExtra(EXTRA_WALLET_ID)
            val pin = intent.getStringExtra(EXTRA_PIN)
            Timber.i(
                "LoginFlow wallet_security_open mode=$mode pending_import=${hasPendingImportMnemonic(this)} pin_reused=${pin != null}"
            )

            val fragment = when (mode) {
                Mode.CREATE_WALLET -> WalletNoticeFragment.newInstance(Mode.CREATE_WALLET)
                Mode.IMPORT_MNEMONIC -> WalletNoticeFragment.newInstance(Mode.IMPORT_MNEMONIC)
                Mode.IMPORT_PRIVATE_KEY -> WalletNoticeFragment.newInstance(Mode.IMPORT_PRIVATE_KEY)
                Mode.ADD_WATCH_ADDRESS -> WalletNoticeFragment.newInstance(Mode.ADD_WATCH_ADDRESS)
                Mode.VIEW_MNEMONIC -> ViewWalletSecurityFragment.newInstance(mode, walletId = walletId)
                Mode.VIEW_PRIVATE_KEY -> ViewWalletSecurityFragment.newInstance(mode, chainId = chainId, walletId = walletId)
                Mode.RE_IMPORT_MNEMONIC -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.RE_IMPORT_MNEMONIC, walletId = walletId)
                Mode.RE_IMPORT_PRIVATE_KEY -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.RE_IMPORT_PRIVATE_KEY, walletId = walletId, chainId = chainId)
                Mode.VIEW_ADDRESS -> ViewWalletAddressFragment.newInstance(walletId)
                Mode.LOGIN_IMPORT_MNEMONIC -> {
                    if (pin.isNullOrBlank()) {
                        VerifyPinBeforeImportWalletFragment.newInstance(Mode.LOGIN_IMPORT_MNEMONIC)
                    } else {
                        FetchingWalletFragment.newInstance(
                            mnemonic = null,
                            pin = pin,
                            importCategory = WalletCategory.IMPORTED_MNEMONIC.value,
                            fetchCustomerServiceSource = AnalyticsTracker.CustomerServiceSource.LOGIN_WALLET_FETCHING,
                            importCustomerServiceSource = AnalyticsTracker.CustomerServiceSource.LOGIN_WALLET_IMPORT,
                            hideCloseButton = true,
                        )
                    }
                }
                Mode.REGISTER_IMPORT_MNEMONIC -> FetchingWalletFragment.newInstance(
                    mnemonic = null,
                    pin = pin,
                    importCategory = WalletCategory.IMPORTED_MNEMONIC.value,
                    fetchCustomerServiceSource = AnalyticsTracker.CustomerServiceSource.LOGIN_WALLET_FETCHING,
                    importCustomerServiceSource = AnalyticsTracker.CustomerServiceSource.LOGIN_WALLET_IMPORT,
                    hideCloseButton = true,
                )
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
        }
    }

    override fun onDestroy() {
        if (mode.requiresSecureWindow()) {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_CHAIN_ID = "extra_chain_id"
        const val EXTRA_WALLET_ID = "extra_wallet_id"
        const val EXTRA_PIN = "extra_pin"

        fun show(activity: Activity, mode: Mode, chainId: String? = null, walletId: String? = null, pin: String? = null) {
            val intent = android.content.Intent(activity, WalletSecurityActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode.ordinal)
            chainId?.let { intent.putExtra(EXTRA_CHAIN_ID, it) }
            walletId?.let { intent.putExtra(EXTRA_WALLET_ID, it) }
            pin?.let { intent.putExtra(EXTRA_PIN, it) }
            activity.startActivity(intent)
        }
    }

    enum class Mode {
        IMPORT_MNEMONIC,
        VIEW_MNEMONIC,
        VIEW_PRIVATE_KEY,
        IMPORT_PRIVATE_KEY,
        ADD_WATCH_ADDRESS,
        RE_IMPORT_MNEMONIC,
        RE_IMPORT_PRIVATE_KEY,
        CREATE_WALLET,
        VIEW_ADDRESS,
        LOGIN_IMPORT_MNEMONIC,
        REGISTER_IMPORT_MNEMONIC,
    }

    private fun Mode.requiresSecureWindow(): Boolean =
        this == Mode.VIEW_MNEMONIC || this == Mode.VIEW_PRIVATE_KEY

    private fun isPendingMnemonicImportStep(): Boolean =
        when (supportFragmentManager.findFragmentById(R.id.container)) {
            is VerifyPinBeforeImportWalletFragment,
            is FetchingWalletFragment,
            is SelectWalletFragment,
            is ImportingWalletFragment,
            -> true
            else -> false
        }

}

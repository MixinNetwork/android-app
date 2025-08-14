package one.mixin.android.ui.wallet

import android.app.Activity
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class WalletSecurityActivity : BlazeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_wallet)
        if (savedInstanceState == null) {
            val modeOrdinal = intent.getIntExtra(EXTRA_MODE, Mode.IMPORT_MNEMONIC.ordinal)
            val mode = Mode.entries[modeOrdinal]
            val chainId = intent.getStringExtra(EXTRA_CHAIN_ID)
            val walletId = intent.getStringExtra(EXTRA_WALLET_ID)

            val fragment = when (mode) {
                Mode.CREATE_WALLET -> WalletNoticeFragment.newInstance(Mode.CREATE_WALLET)
                Mode.IMPORT_MNEMONIC -> WalletNoticeFragment.newInstance(Mode.IMPORT_MNEMONIC)
                Mode.IMPORT_PRIVATE_KEY -> WalletNoticeFragment.newInstance(Mode.IMPORT_PRIVATE_KEY)
                Mode.ADD_WATCH_ADDRESS -> WalletNoticeFragment.newInstance(Mode.ADD_WATCH_ADDRESS)
                Mode.VIEW_MNEMONIC -> ViewWalletSecurityFragment.newInstance(mode, walletId = walletId)
                Mode.VIEW_PRIVATE_KEY -> ViewWalletSecurityFragment.newInstance(mode, chainId = chainId, walletId = walletId)
                Mode.RE_IMPORT_MNEMONIC -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.RE_IMPORT_MNEMONIC, walletId = walletId)
                Mode.RE_IMPORT_PRIVATE_KEY -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.RE_IMPORT_PRIVATE_KEY, walletId = walletId, chainId = chainId)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_CHAIN_ID = "extra_chain_id"
        const val EXTRA_WALLET_ID = "extra_wallet_id"

        fun show(activity: Activity, mode: Mode, chainId: String? = null, walletId: String? = null) {
            val intent = android.content.Intent(activity, WalletSecurityActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode.ordinal)
            chainId?.let { intent.putExtra(EXTRA_CHAIN_ID, it) }
            walletId?.let { intent.putExtra(EXTRA_WALLET_ID, it) }
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
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

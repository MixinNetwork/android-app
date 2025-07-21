package one.mixin.android.ui.wallet

import android.app.Activity
import android.os.Bundle
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.AddWalletSuccessEvent
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class WalletSecurityActivity : BlazeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_wallet)
        if (savedInstanceState == null) {
            val modeOrdinal = intent.getIntExtra(EXTRA_MODE, Mode.IMPORT.ordinal)
            val mode = Mode.values()[modeOrdinal]
            val chainId = intent.getStringExtra(EXTRA_CHAIN_ID)

            val fragment = when (mode) {
                Mode.IMPORT -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.IMPORT)
                Mode.VIEW_MNEMONIC -> ViewWalletSecurityFragment.newInstance(Mode.VIEW_MNEMONIC)
                Mode.VIEW_PRIVATE_KEY -> ViewWalletSecurityFragment.newInstance(Mode.VIEW_PRIVATE_KEY, chainId)
                Mode.IMPORT_PRIVATE_KEY -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.IMPORT_PRIVATE_KEY)
                Mode.ADD_WATCH_ADDRESS -> VerifyPinBeforeImportWalletFragment.newInstance(Mode.ADD_WATCH_ADDRESS)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
        }
        RxBus.listen(AddWalletSuccessEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(destroyScope).subscribe {
            finish()
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_CHAIN_ID = "extra_chain_id"

        fun show(activity: Activity, mode: Mode, chainId: String? = null) {
            val intent = android.content.Intent(activity, WalletSecurityActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode.ordinal)
            chainId?.let { intent.putExtra(EXTRA_CHAIN_ID, it) }
            activity.startActivity(intent)
        }
    }

    enum class Mode {
        IMPORT,
        VIEW_MNEMONIC,
        VIEW_PRIVATE_KEY,
        IMPORT_PRIVATE_KEY,
        ADD_WATCH_ADDRESS,
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}


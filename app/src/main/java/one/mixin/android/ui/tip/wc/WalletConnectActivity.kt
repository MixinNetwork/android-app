package one.mixin.android.ui.tip.wc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.tip.Tip
import one.mixin.android.tip.wc.WCError
import one.mixin.android.tip.wc.WCEvent
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnect.RequestType
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WalletConnectActivity : BaseActivity() {
    @Inject
    lateinit var tip: Tip

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Transparent

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Transparent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
    }

    private fun handleIntent(intent: Intent) {
        val event = intent.getParcelableExtraCompat(ARGS_WC_EVENT, WCEvent::class.java)
        if (event != null) {
            val wcBottom = supportFragmentManager.findFragmentByTag(WalletConnectBottomSheetDialogFragment.TAG) as? WalletConnectBottomSheetDialogFragment
            if (wcBottom == null) {
                handleWCEvent(event)
            } else {
                if (wcBottom.step == WalletConnectBottomSheetDialogFragment.Step.Connecting || wcBottom.step == WalletConnectBottomSheetDialogFragment.Step.Done) {
                    wcBottom.dismiss()
                    handleWCEvent(event)
                } else {
                    Timber.e("$TAG wcBottom step is ${wcBottom.step}, not done or connecting, skip this $event")
                }
            }
        } else {
            val wcBottom = supportFragmentManager.findFragmentByTag(WalletConnectBottomSheetDialogFragment.TAG) as? WalletConnectBottomSheetDialogFragment
            if (wcBottom != null) {
                if (wcBottom.step != WalletConnectBottomSheetDialogFragment.Step.Connecting) {
                    return
                }
                wcBottom.dismiss()
            }

            val qrBottom = supportFragmentManager.findFragmentByTag(QrScanBottomSheetDialogFragment.TAG)
            if (qrBottom != null) return

            val error = intent.getParcelableExtraCompat(ARGS_WC_ERROR, WCError::class.java)
            if (error != null) {
                QrScanBottomSheetDialogFragment.newInstance(error.throwable.toString()).apply {
                    enableFinishOnDetach = true
                }.showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
            } else {
                Timber.e("$TAG ARGS_WC_EVENT ARGS_WC_ERROR all null")
                finish()
            }
        }
    }

    private fun handleWCEvent(event: WCEvent) {
        when (event.version) {
            WalletConnect.Version.V2 -> {
                event as WCEvent.V2
                when (event.requestType) {
                    RequestType.Connect -> {
                        showWalletConnectBottomSheet(
                            RequestType.Connect,
                            WalletConnect.Version.V2,
                            event.topic,
                        )
                    }
                    RequestType.SessionProposal -> {
                        showWalletConnectBottomSheet(
                            RequestType.SessionProposal,
                            WalletConnect.Version.V2,
                            event.topic,
                        )
                    }
                    RequestType.SessionRequest -> {
                        showWalletConnectBottomSheet(
                            RequestType.SessionRequest,
                            WalletConnect.Version.V2,
                            event.topic,
                        )
                    }
                }
            }
            WalletConnect.Version.TIP -> {
                Timber.e("$TAG invalid event $event")
            }
            WalletConnect.Version.BROWSER -> {

            }
        }
    }

    private fun showWalletConnectBottomSheet(
        requestType: RequestType,
        @Suppress("SameParameterValue") version: WalletConnect.Version,
        topic: String?,
    ) {
        showWalletConnectBottomSheetDialogFragment(
            tip,
            this,
            requestType,
            version,
            topic,
        )
    }

    companion object {
        const val TAG = "WalletConnectActivity"
        const val ARGS_WC_EVENT = "args_wc_event"
        const val ARGS_WC_ERROR = "args_wc_error"

        fun show(
            context: Context,
            event: WCEvent,
        ) {
            context.startActivity(
                Intent(context, WalletConnectActivity::class.java).apply {
                    putExtra(ARGS_WC_EVENT, event)
                },
            )
        }

        fun show(
            context: Context,
            error: WCError,
        ) {
            context.startActivity(
                Intent(context, WalletConnectActivity::class.java).apply {
                    putExtra(ARGS_WC_ERROR, error)
                },
            )
        }
    }
}

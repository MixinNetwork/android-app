@file:Suppress("DEPRECATION")

package one.mixin.android.ui.tip.wc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import one.mixin.android.ui.tip.wc.pay.WalletConnectPayBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WalletConnectActivity : BaseActivity() {
    @Inject
    lateinit var tip: Tip

    private var handlingConnectError = false

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Transparent

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Transparent

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        SystemUIManager.setSafePadding(window, Color.TRANSPARENT)
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
        val error = intent.getParcelableExtraCompat(ARGS_WC_ERROR, WCError::class.java)
        if (event != null) {
            if (handlingConnectError && event.requestType == RequestType.Connect) {
                return
            }
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
            if (error != null) {
                handlingConnectError = true
            }
            val wcBottom = supportFragmentManager.findFragmentByTag(WalletConnectBottomSheetDialogFragment.TAG) as? WalletConnectBottomSheetDialogFragment
            if (wcBottom != null) {
                if (wcBottom.step != WalletConnectBottomSheetDialogFragment.Step.Connecting) {
                    return
                }
                wcBottom.dismiss()
            }

            val qrBottom = supportFragmentManager.findFragmentByTag(QrScanBottomSheetDialogFragment.TAG)
            if (qrBottom != null) return

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
                when (event.requestType) {
                    RequestType.Pay -> {
                        event as WCEvent.Pay
                        val existing = supportFragmentManager.findFragmentByTag(WalletConnectPayBottomSheetDialogFragment.TAG)
                        if (existing == null) {
                            WalletConnectPayBottomSheetDialogFragment.newInstance(event.paymentLink)
                                .showNow(supportFragmentManager, WalletConnectPayBottomSheetDialogFragment.TAG)
                        }
                    }
                    else -> {
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
                            else -> {}
                        }
                    }
                }
            }
            WalletConnect.Version.TIP -> {
                Timber.e("$TAG invalid event $event")
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

        private fun baseIntent(context: Context) =
            Intent(context, WalletConnectActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

        fun show(
            context: Context,
            event: WCEvent,
        ) {
            context.startActivity(
                baseIntent(context).apply {
                    putExtra(ARGS_WC_EVENT, event)
                },
            )
            (context as? Activity)?.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        }

        fun show(
            context: Context,
            error: WCError,
        ) {
            context.startActivity(
                baseIntent(context).apply {
                    putExtra(ARGS_WC_ERROR, error)
                },
            )
            (context as? Activity)?.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        }
    }
}

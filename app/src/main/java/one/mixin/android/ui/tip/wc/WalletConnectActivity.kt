package one.mixin.android.ui.tip.wc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.event.WCEvent
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnect.RequestType
import one.mixin.android.tip.wc.WalletConnectV1
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.ui.common.BaseActivity
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

        val event = requireNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(ARGS_WC_EVENT, WCEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(ARGS_WC_EVENT)
            },
        ) { "required WCEvent is null" }
        handleWCEvent(event)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun handleWCEvent(event: WCEvent) {
        when (event.version) {
            WalletConnect.Version.V1 -> {
                val wc = WalletConnectV1
                event as WCEvent.V1
                when (event.requestType) {
                    RequestType.SessionProposal -> {
                        showWalletConnectBottomSheet(
                            RequestType.SessionProposal,
                            WalletConnect.Version.V1,
                            { wc.rejectSession() },
                        ) { priv ->
                            wc.approveSession(priv)
                        }
                    }
                    RequestType.SwitchNetwork -> {
                        showWalletConnectBottomSheet(
                            RequestType.SwitchNetwork,
                            WalletConnect.Version.V1,
                            { wc.rejectRequest(event.id) },
                        ) { priv ->
                            wc.walletChangeNetwork(priv, event.id)
                        }
                    }
                    RequestType.SessionRequest -> {
                        showWalletConnectBottomSheet(
                            RequestType.SessionRequest,
                            WalletConnect.Version.V1,
                            { wc.rejectRequest(event.id) },
                        ) { priv ->
                            wc.approveRequest(priv, event.id)
                        }
                    }
                }
            }
            WalletConnect.Version.V2 -> {
                when (event.requestType) {
                    RequestType.SessionProposal -> {
                        showWalletConnectBottomSheet(
                            RequestType.SessionProposal,
                            WalletConnect.Version.V2,
                            { WalletConnectV2.rejectSession() },
                        ) { priv ->
                            WalletConnectV2.approveSession(priv)
                        }
                    }
                    RequestType.SwitchNetwork -> {
                    }
                    RequestType.SessionRequest -> {
                        showWalletConnectBottomSheet(
                            RequestType.SessionRequest,
                            WalletConnect.Version.V2,
                            { WalletConnectV2.rejectRequest() },
                            { priv ->
                                WalletConnectV2.approveRequest(priv)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun showWalletConnectBottomSheet(
        requestType: RequestType,
        version: WalletConnect.Version,
        onReject: () -> Unit,
        callback: suspend (ByteArray) -> Unit,
    ) {
        val wcBottomSheet = WalletConnectBottomSheetDialogFragment.newInstance(requestType, version)
        wcBottomSheet.setOnPinComplete { pin ->
            val result = tip.getOrRecoverTipPriv(this, pin)
            if (result.isSuccess) {
                withContext(Dispatchers.IO) {
                    callback(tipPrivToPrivateKey(result.getOrThrow()))
                }
                return@setOnPinComplete null
            } else {
                val e = result.exceptionOrNull()
                val errorInfo = e?.stackTraceToString()
                Timber.d("${if (version == WalletConnect.Version.V2) WalletConnectV2.TAG else WalletConnectV1.TAG} $errorInfo")
                return@setOnPinComplete if (e is TipNetworkException) {
                    "code: ${e.error.code}, message: ${e.error.description}"
                } else {
                    errorInfo
                }
            }
        }.setOnReject { onReject() }
            .showNow(supportFragmentManager, WalletConnectBottomSheetDialogFragment.TAG)
    }

    companion object {
        const val TAG = "WalletConnectActivity"
        const val ARGS_WC_EVENT = "args_wc_event"

        fun show(context: Context, event: WCEvent) {
            context.startActivity(
                Intent(context, WalletConnectActivity::class.java).apply {
                    putExtra(ARGS_WC_EVENT, event)
                },
            )
        }
    }
}

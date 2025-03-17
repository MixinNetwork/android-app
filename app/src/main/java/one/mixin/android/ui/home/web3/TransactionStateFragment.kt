package one.mixin.android.ui.home.web3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.api.response.web3.TxState
import one.mixin.android.api.response.web3.isFinalTxState
import one.mixin.android.api.response.web3.isTxSuccess
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.event.SolanaRefreshEvent
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.tickerFlow
import one.mixin.android.web3.Rpc
import org.sol4k.VersionedTransaction
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class TransactionStateFragment : BaseFragment() {
    companion object {
        const val TAG = "TransactionStateFragment"

        const val ARGS_TX = "args_tx"
        const val ARGS_TOKEN_SYMBOL = "args_token_symbol"

        fun newInstance(
            tx: String,
            tokenSymbol: String?,
        ) =
            TransactionStateFragment().withArgs {
                putString(ARGS_TX, tx)
                tokenSymbol?.let { putString(ARGS_TOKEN_SYMBOL, it) }
            }
    }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val tx: VersionedTransaction by lazy {
        val serializedTx = requireArguments().getString(ARGS_TX)!!
        VersionedTransaction.from(serializedTx)
    }
    private val symbol: String? by lazy { requireArguments().getString(ARGS_TOKEN_SYMBOL) }

    private var txState: Tx? by mutableStateOf(null)

    @Inject
    lateinit var rpc: Rpc

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    TransactionStatePage(
                        tx = txState ?: Tx(TxState.NotFound.name),
                        symbol = symbol,
                        viewTx = {
                            WebActivity.show(context, "https://solscan.io/tx/${tx.signatures[0]}", null)
                        },
                    ) {
                        val action = closeAction
                        if (action == null) {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        } else {
                            closeAction?.invoke()
                        }
                    }
                }
            }
            refreshTx()
        }
    }

    private var refreshTxJob: Job? = null

    private fun refreshTx() {
        val txhash = tx.signatures[0]
        val blockhash = tx.message.recentBlockhash
        Timber.e("$TAG txhash: $txhash, blockhash: $blockhash")
        refreshTxJob?.cancel()
        refreshTxJob =
            tickerFlow(2.seconds)
                .onEach {
                    try {
                        handleMixinResponse(
                            invokeNetwork = { web3ViewModel.getWeb3Tx(txhash) },
                            successBlock = {
                                txState = it.data
                            },
                            failureBlock = {
                                if (it.errorCode == 401) {
                                    web3ViewModel.getBotPublicKey(ROUTE_BOT_USER_ID, true)
                                    refreshTx()
                                }
                                return@handleMixinResponse true
                            },
                        )
                        if (txState?.state?.isFinalTxState() == true) {
                            if (txState?.state?.isTxSuccess() == true) {
                                AnalyticsTracker.trackSwapSend()
                                RxBus.publish(SolanaRefreshEvent())
                            }
                            refreshTxJob?.cancel()
                        } else {
                            val isBlockhashValid =
                                withContext(Dispatchers.IO) {
                                    rpc.isBlockhashValid(blockhash) ?: false
                                }
                            if (!isBlockhashValid) {
                                Timber.e("$TAG blockhash $blockhash invalid")
                                val ts =
                                    handleMixinResponse(
                                        invokeNetwork = { web3ViewModel.getWeb3Tx(txhash) },
                                        successBlock = { it.data },
                                    )
                                refreshTxJob?.cancel()
                                txState =
                                    if (ts?.state?.isFinalTxState() == true) {
                                        if (ts.state.isTxSuccess()) {
                                            RxBus.publish(SolanaRefreshEvent())
                                        }
                                        ts
                                    } else {
                                        Tx(TxState.Failed.name)
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }.launchIn(lifecycleScope)
    }

    private var closeAction: (() -> Unit)? = null

    fun setCloseAction(action: () -> Unit): TransactionStateFragment {
        this.closeAction = action
        return this
    }
}

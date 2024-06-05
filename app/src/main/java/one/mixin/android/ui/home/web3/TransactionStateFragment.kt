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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.api.response.web3.TxState
import one.mixin.android.api.response.web3.isFinalTxState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.swap.SwapViewModel
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.tickerFlow
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class TransactionStateFragment : BaseFragment() {
    companion object {
        const val TAG = "TransactionStateFragment"

        const val ARGS_TXHASH = "args_txhash"
        const val ARGS_TOKEN_SYMBOL = "args_token_symbol"
        const val ARGS_BLOCKHASH = "args_blockhash"

        fun newInstance(txhash: String, blockhash: String, tokenSymbol: String) = TransactionStateFragment().withArgs {
            putString(ARGS_TXHASH, txhash)
            putString(ARGS_BLOCKHASH, blockhash)
            putString(ARGS_TOKEN_SYMBOL, tokenSymbol)
        }
    }
    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val txhash: String by lazy { requireArguments().getString(ARGS_TXHASH)!! }
    private val symbol: String by lazy { requireArguments().getString(ARGS_TOKEN_SYMBOL)!! }
    private val blockhash: String by lazy { requireArguments().getString(ARGS_BLOCKHASH)!! }

    private var tx: Tx? by mutableStateOf(null)

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
                        tx = tx ?: Tx(TxState.NotFound.name),
                        symbol = symbol,
                        viewTx = {
                            WebActivity.show(context, "https://solscan.io/tx/$txhash", null)
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
        Timber.e("$TAG txhash: $txhash, blockhash: $blockhash")
        refreshTxJob?.cancel()
        refreshTxJob =
            tickerFlow(2.seconds)
                .onEach {
                    handleMixinResponse(
                        invokeNetwork = { web3ViewModel.getWeb3Tx(txhash) },
                        successBlock = {
                            tx = it.data
                        },
                        failureBlock = {
                            if (it.errorCode == 401) {
                                web3ViewModel.getBotPublicKey(ROUTE_BOT_USER_ID)
                                refreshTx()
                            }
                            return@handleMixinResponse true
                        },
                    )
                    if (tx?.state?.isFinalTxState() == true) {
                        refreshTxJob?.cancel()
                    } else {
                        if (!web3ViewModel.isBlockhashValid(blockhash)) {
                            Timber.e("$TAG blockhash $blockhash valid")
                            refreshTxJob?.cancel()
                            tx = Tx(TxState.Failed.name)
                        }
                    }
                }.launchIn(lifecycleScope)
    }

    private var closeAction: (() -> Unit)? = null
    fun setCloseAction(action: () -> Unit): TransactionStateFragment {
        this.closeAction = action
        return this
    }
}
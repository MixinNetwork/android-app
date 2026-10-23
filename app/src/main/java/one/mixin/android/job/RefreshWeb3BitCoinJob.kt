package one.mixin.android.job

import one.mixin.android.Constants

class RefreshWeb3BitCoinJob(walletId: String) : RefreshWeb3UtxoJob(walletId, Constants.ChainId.BITCOIN_CHAIN_ID) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3BitCoinJob"
    }
}

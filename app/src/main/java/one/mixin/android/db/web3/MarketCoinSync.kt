package one.mixin.android.db.web3

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import one.mixin.android.db.MarketCoinDao
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.withRoomTransaction
import timber.log.Timber

fun CoroutineScope.syncMarketCoins(source: MarketCoinDao, wallet: WalletDatabase) = launch {
    source.observeAll().distinctUntilChanged().onEach { coins ->
        wallet.withRoomTransaction {
            // ponytail: copy the mapping snapshot; use deltas if mapping volume makes this costly.
            wallet.web3MarketCoinDao().deleteAll()
            wallet.web3MarketCoinDao().insertListSuspend(coins)
        }
    }.retryWhen { error, _ ->
        Timber.e(error, "Failed to sync Web3 market coins")
        delay(1_000)
        true
    }.collect {}
}

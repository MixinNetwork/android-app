package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.RxBus
import one.mixin.android.api.response.SafeAsset
import one.mixin.android.api.response.UserSafe
import one.mixin.android.db.web3.vo.SafeChain
import one.mixin.android.db.web3.vo.SafeWallets
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.event.WalletOperationType
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshSafeAccountsJob : BaseJob(
    Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshUserAccountsJob"
    }

    override fun onRun(): Unit = runBlocking {
        try {
            fetchUserAccounts()
            Timber.d("Successfully refreshed user accounts")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh user accounts")
        }
    }

    private suspend fun fetchUserAccounts() {
        requestRouteAPI(
            invokeNetwork = {
                accountService.getUserAccounts()
            },
            successBlock = { response ->
                val userAccounts = response.data
                if (userAccounts != null && userAccounts.isNotEmpty()) {
                    Timber.d("Fetched ${userAccounts.size} user accounts")
                    userAccounts.forEach { account ->
                        Timber.d("Account: accountId=${account.accountId}, name=${account.name}, chainId=${account.chainId}, address=${account.address}")
                        saveUserAccount(account)
                    }
                    userAccounts.map { it.accountId }.let { ids ->
                        val localIds = safeWalletsDao.getAllSafeWallets().map { it.id }
                        safeWalletsDao.deleteSafeWalletNotIn(ids)
                        (localIds - ids.toSet()).let {
                            if (it.isNotEmpty()) {
                                web3TokenDao.deleteInByWalletIds(it)
                            }
                        }
                    }

                } else {
                    Timber.d("No user accounts found")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch user accounts: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }

    private suspend fun saveUserAccount(account: UserSafe) {
        val walletId = account.accountId
        val currentTime = nowInUtc()

        val safeWallet = SafeWallets(
            id = walletId,
            name = account.name,
            createdAt = account.createdAt,
            updatedAt = currentTime,
            safeRole = account.role,
            safeChainId = SafeChain.fromValue(account.chainId)?.chainId ?: "",
            safeAddress = account.address,
            safeUrl = account.uri,
        )
        safeWalletsDao.insert(safeWallet)
        Timber.d("Saved wallet: walletId=$walletId, name=${account.name}, address=${account.address}")

        if (account.assets.isNotEmpty()) {
            val tokens = account.assets.mapNotNull { asset ->
                convertSafeAssetToWeb3Token(walletId, asset)
            }
            val tokensToInsert = applyBitcoinTokenBalanceBeforeInsert(walletId, tokens)
            web3TokenDao.insertList(tokensToInsert)
            web3TokenDao.deleteNotIn(account.accountId, tokens.map { it.assetId })
            Timber.d("Saved ${tokens.size} tokens for wallet $walletId")
        } else {
            web3TokenDao.deleteByWalletId(account.accountId)
        }

        RxBus.publish(WalletRefreshedEvent(walletId, WalletOperationType.OTHER))
    }

    private suspend fun convertSafeAssetToWeb3Token(walletId: String, asset: SafeAsset): Web3Token? {
        val local = web3TokenDao.findAnyTokenById(asset.mixinAssetId)
        return local?.copy(
            walletId = walletId,
            balance = asset.balance,
            priceUsd = asset.priceUsd,
            precision = asset.decimal,
            iconUrl = asset.iconUrl,
        ) ?: assetService.getAssetByIdSuspend(asset.mixinAssetId).data?.let {
            Web3Token(
                walletId = walletId,
                assetId = asset.mixinAssetId,
                chainId = it.chainId,
                name = it.name,
                assetKey = it.assetKey ?: asset.address,
                symbol = it.symbol,
                iconUrl = asset.iconUrl,
                precision = asset.decimal,
                balance = asset.balance,
                priceUsd = asset.priceUsd,
                changeUsd = it.changeUsd,
            )
        }
    }
}

package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.response.SafeAsset
import one.mixin.android.api.response.UserSafe
import one.mixin.android.db.web3.vo.SafeChain
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.assetIdToAsset
import timber.log.Timber

class RefreshUserAccountsJob : BaseJob(
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
        
        val wallet = Web3Wallet(
            id = walletId,
            category = WalletCategory.MIXIN_SAFE.value,
            name = account.name,
            createdAt = account.createdAt,
            updatedAt = currentTime,
            owners = account.owners,
            safeChainId = SafeChain.fromValue(account.chainId)?.chainId,
            safeAddress = account.address,
        )
        
        web3WalletDao.insert(wallet)
        Timber.d("Saved wallet: walletId=$walletId, name=${account.name}, address=${account.address}")
        
        if (account.assets.isNotEmpty()) {
            val tokens = account.assets.mapNotNull { asset ->
                convertSafeAssetToWeb3Token(walletId, asset)
            }
            web3TokenDao.insertList(tokens)
            Timber.d("Saved ${tokens.size} tokens for wallet $walletId")
        }
    }

    private suspend fun convertSafeAssetToWeb3Token(walletId: String, asset: SafeAsset): Web3Token? {
        val local = web3TokenDao.findAnyTokenById(asset.mixinAssetId)
        return local?.copy(
            walletId = walletId,
            balance = asset.balance,
        ) ?: assetService.getAssetByIdSuspend(asset.assetId).data?.let {
            Web3Token(
                walletId = walletId,
                assetId = asset.assetId,
                chainId = it.chainId,
                name = it.name,
                assetKey = asset.address,
                symbol = it.symbol,
                iconUrl = it.iconUrl,
                precision = asset.decimal,
                balance = asset.balance,
                priceUsd = it.priceUsd,
                changeUsd = it.changeUsd,
            )
        }
    }
}

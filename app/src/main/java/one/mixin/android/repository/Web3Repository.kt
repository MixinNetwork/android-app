package one.mixin.android.repository

import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.AddressSearchRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokensExtra
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Web3Repository
@Inject
constructor(
    val routeService: RouteService,
    val web3TokenDao: Web3TokenDao,
    val web3TransactionDao: Web3TransactionDao,
    val web3TokensExtraDao: Web3TokensExtraDao,
    val web3AddressDao: Web3AddressDao,
    val web3WalletDao: Web3WalletDao
) {
    suspend fun estimateFee(request: EstimateFeeRequest) = routeService.estimateFee(request)

    suspend fun insertWeb3Tokens(list: List<Web3Token>) = web3TokenDao.insertListSuspend(list)

    suspend fun web3TokenItemByAddress(address: String) = web3TokenDao.web3TokenItemByAddress(address)

    suspend fun web3TokenItemById(assetId: String) = web3TokenDao.web3TokenItemById(assetId)
    
    suspend fun findWeb3TokenItemsByIds(assetIds: List<String>) = web3TokenDao.findWeb3TokenItemsByIds(assetIds)

    fun web3Tokens(walletId: String) = web3TokenDao.web3TokenItems(walletId)
    
    fun web3TokensExcludeHidden(walletId: String) = web3TokenDao.web3TokenItemsExcludeHidden(walletId)
    
    fun hiddenAssetItems(walletId: String) = web3TokenDao.hiddenAssetItems(walletId)
    
    suspend fun updateTokenHidden(tokenId: String, walletId: String, hidden: Boolean) {
        val tokensExtra = web3TokensExtraDao.findByAssetId(tokenId,  walletId)
        if (tokensExtra != null) {
            web3TokensExtraDao.updateHidden(tokenId, walletId, hidden)
        } else {
            web3TokensExtraDao.insertSuspend(Web3TokensExtra(walletId, tokenId, hidden))
        }
    }

    fun web3Transactions(walletId:String, assetId: String) = web3TransactionDao.web3Transactions(walletId, assetId)
    
    suspend fun getAddressesByChainId(walletId: String): Web3Address? {
        return web3AddressDao.getAddressesByChainId(walletId)
    }

    suspend fun getClassicWalletId(): String? = web3WalletDao.getClassicWalletId()

    suspend fun searchAssetsByAddresses(addresses: List<String>) = routeService.searchAssetsByAddresses(
        AddressSearchRequest(addresses)
    )

    suspend fun createWallet(request: WalletRequest) = routeService.createWallet(request)

    suspend fun createAddress(request: Web3AddressRequest) = routeService.createAddress(request)
}

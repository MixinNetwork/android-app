package one.mixin.android.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.AddressSearchRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.property.Web3PropertyHelper
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.updateWithLocalKeyInfo
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Wallet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Web3Repository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val routeService: RouteService,
    val web3TokenDao: Web3TokenDao,
    val web3TransactionDao: Web3TransactionDao,
    val web3TokensExtraDao: Web3TokensExtraDao,
    val web3AddressDao: Web3AddressDao,
    val web3WalletDao: Web3WalletDao,
    val userRepository: UserRepository
) {
    suspend fun estimateFee(request: EstimateFeeRequest) = routeService.estimateFee(request)

    suspend fun insertWeb3Tokens(list: List<Web3Token>) = web3TokenDao.insertListSuspend(list)

    suspend fun web3TokenItemByAddress(address: String) = web3TokenDao.web3TokenItemByAddress(address)

    suspend fun web3TokenItemById(walletId: String, assetId: String) = web3TokenDao.web3TokenItemById(walletId, assetId)
    
    suspend fun findWeb3TokenItemsByIds(walletId: String, assetIds: List<String>) = web3TokenDao.findWeb3TokenItemsByIds(walletId, assetIds)

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

    suspend fun getClassicWalletId(): String? = web3WalletDao.getClassicWalletId()

    suspend fun searchAssetsByAddresses(addresses: List<String>) = routeService.searchAssetsByAddresses(
        AddressSearchRequest(addresses)
    )

    suspend fun createWallet(request: WalletRequest) = routeService.createWallet(request)

    suspend fun updateWallet(walletId: String, request: WalletRequest) = routeService.updateWallet(walletId, request)

    suspend fun insertWallet(wallet: Web3Wallet) = web3WalletDao.insertSuspend(wallet)

    suspend fun updateWalletName(walletId: String, newName: String) = web3WalletDao.updateWalletName(walletId, newName)

    suspend fun insertAddress(address: Web3Address) = web3AddressDao.insertSuspend(address)

    suspend fun insertAddressList(addresses: List<Web3Address>) = web3AddressDao.insertListSuspend(addresses)

    suspend fun fetchSessionsSuspend(userIds: List<String>) = userRepository.fetchSessionsSuspend(userIds)

    suspend fun destroyWallet(walletId: String) = routeService.destroyWallet(walletId)

    suspend fun deleteWallet(walletId: String) = web3WalletDao.deleteWallet(walletId)

    suspend fun deleteAddressesByWalletId(walletId: String) {
        web3AddressDao.getAddressesByWalletId(walletId).forEach { address ->
            Web3PropertyHelper.deleteKeyValue(address.destination)
        }
        web3AddressDao.deleteByWalletId(walletId)
    }

    suspend fun deleteAssetsByWalletId(walletId: String) = web3TokenDao.deleteByWalletId(walletId)

    suspend fun deleteHiddenTokens(walletId: String) = web3TokensExtraDao.deleteHiddenTokens(walletId)

    suspend fun deleteTransactionsByWalletId(walletId: String) = web3TransactionDao.deleteByWalletId(walletId)

    suspend fun getAddressesByChainId(walletId: String, chainId: String) = web3AddressDao.getAddressesByChainId(walletId, chainId)

    suspend fun getAddresses(walletId: String) = web3AddressDao.getAddressesByWalletId(walletId)

    suspend fun getAddressesGroupedByDestination(walletId: String) = web3AddressDao.getAddressesGroupedByDestination(walletId)

    suspend fun findWalletById(walletId: String) =
        web3WalletDao.getWalletById(walletId)?.updateWithLocalKeyInfo(context)

    suspend fun getWalletsExcluding(excludeWalletId: String, chainId: String, query: String) =
        web3WalletDao.getWalletsExcludingByName(excludeWalletId, chainId, query)
            .updateWithLocalKeyInfo(context)

    suspend fun getAllWallets() = web3WalletDao.getAllWallets().map { it.updateWithLocalKeyInfo(context) }

    suspend fun countAddressesByWalletId(walletId: String) = web3AddressDao.countAddressesByWalletId(walletId)

    suspend fun getFirstAddressByWalletId(walletId: String) = web3AddressDao.getFirstAddressByWalletId(walletId)

    suspend fun getAddressesByWalletId(walletId: String) = web3AddressDao.getAddressesByWalletId(walletId)

    suspend fun anyAddressExists(destinations: List<String>) = web3AddressDao.anyAddressExists(destinations)

    suspend fun allWeb3Tokens(walletIds: List<String>) = web3TokenDao.allWeb3Tokens(walletIds)

    suspend fun findUnifiedAssetItem(walletId: String) = web3TokenDao.findUnifiedAssetItem(walletId)
    suspend fun isAddressMatch(walletId: String, address: String): Boolean {
        return web3AddressDao.isAddressMatch(walletId, address)
    }

    suspend fun getAllWalletNames(categories :List<String>) = web3WalletDao.getAllWalletNames(categories)
}

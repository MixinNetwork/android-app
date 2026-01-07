package one.mixin.android.repository

import android.content.Context
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.paging.DataSource
import androidx.room.RoomRawQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.request.AddressSearchRequest
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.service.RouteService
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.property.Web3PropertyHelper
import one.mixin.android.db.OrderDao
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3ChainDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.WalletOutputDao
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.web3.updateWithLocalKeyInfo
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.route.Order
import one.mixin.android.vo.safe.toWeb3TokenItem
import timber.log.Timber
import java.math.BigDecimal
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
    val tokenRepository: TokenRepository,
    val userRepository: UserRepository,
    val web3ChainDao: Web3ChainDao,
    val orderDao: OrderDao,
    val walletOutputDao: WalletOutputDao,
) {
    suspend fun estimateFee(request: EstimateFeeRequest) = routeService.estimateFee(request)

    suspend fun refreshBitcoinTokenAmount(walletId: String, address: String) {
        if (walletId.isBlank() || address.isBlank()) return
        val totalUnspent: BigDecimal = walletOutputDao.sumUnspentAmount(address, Constants.ChainId.BITCOIN_CHAIN_ID)
        val amount: String = totalUnspent.stripTrailingZeros().toPlainString()
        web3TokenDao.updateTokenAmount(walletId, Constants.ChainId.BITCOIN_CHAIN_ID, amount)
    }

    suspend fun web3TokenItemByAddress(address: String) = web3TokenDao.web3TokenItemByAddress(address)

    suspend fun web3TokenItemById(walletId: String, assetId: String) = web3TokenDao.web3TokenItemById(walletId, assetId)
    
    suspend fun findWeb3TokenItemsByIds(walletId: String, assetIds: List<String>) = web3TokenDao.findWeb3TokenItemsByIds(walletId, assetIds)

    fun web3TokensExcludeHidden(walletId: String) = web3TokenDao.web3TokenItemsExcludeHidden(walletId)

    fun web3TokensExcludeHiddenRaw(walletId: String, defaultIconUrl: String = Constants.DEFAULT_ICON_URL) = web3TokenDao.web3TokenItemsExcludeHiddenRaw(
        RoomRawQuery(
            """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.wallet_id = t.wallet_id AND te.asset_id = t.asset_id
        WHERE t.wallet_id = :walletId AND (te.hidden != 1 OR te.hidden IS NULL) 
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
        """, onBindStatement = {
                it.bindText(1, walletId)
                it.bindText(2, defaultIconUrl)
            })
    )

    fun hiddenAssetItems(walletId: String) = web3TokenDao.hiddenAssetItems(walletId)
    
    suspend fun updateTokenHidden(tokenId: String, walletId: String, hidden: Boolean) {
        val tokensExtra = web3TokensExtraDao.findByAssetId(tokenId,  walletId)
        if (tokensExtra != null) {
            web3TokensExtraDao.updateHidden(tokenId, walletId, hidden)
        } else {
            web3TokensExtraDao.insertSuspend(Web3TokensExtra(walletId, tokenId, hidden))
        }
    }

    fun web3Transactions(walletId: String, assetId: String) =
        web3TransactionDao.web3Transactions(walletId, assetId).switchMap { list ->
            liveData {
                val assetIds = list.flatMap { it.senders.map { it.assetId } + it.receivers.map { it.assetId } + (it.approvals?.map { it.assetId } ?: emptyList()) }.distinct()
                val tokens = web3TokenDao.findWeb3TokenItemsByIds(walletId, assetIds).associateBy { it.assetId }
                val result = list.map { transaction ->
                    transaction.copy(
                        senders = transaction.senders.map {
                            it.copy(symbol = tokens[it.assetId]?.symbol)
                        },
                        receivers = transaction.receivers.map {
                            it.copy(symbol = tokens[it.assetId]?.symbol)
                        },
                        approvals = transaction.approvals?.map {
                            it.copy(symbol = tokens[it.assetId]?.symbol)
                        }
                    )
                }
                emit(result)
            }
        }

    fun allWeb3Transaction(filterParams: Web3FilterParams): DataSource.Factory<Int, Web3TransactionItem> {
        return web3TransactionDao.allTransactions(filterParams.buildQuery()).map { transaction ->
            val assetIds = transaction.senders.map { it.assetId } + transaction.receivers.map { it.assetId } + (transaction.approvals?.map { it.assetId } ?: emptyList())
            val tokens = web3TokenDao.findWeb3TokenItemsByIdsSync(filterParams.walletId, assetIds.distinct()).associateBy { it.assetId }
            transaction.copy(
                senders = transaction.senders.map {
                    it.copy(symbol = tokens[it.assetId]?.symbol)
                },
                receivers = transaction.receivers.map {
                    it.copy(symbol = tokens[it.assetId]?.symbol)
                },
                approvals = transaction.approvals?.map {
                    it.copy(symbol = tokens[it.assetId]?.symbol)
                }
            )
        }
    }

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

    suspend fun deleteOrders(walletId: String) = orderDao.deleteOrders(walletId)

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

    suspend fun getSafeWalletsByChainId(chainId: String) =
        web3WalletDao.getSafeWalletsByChainId(chainId).updateWithLocalKeyInfo(context)
    suspend fun getWalletsExcluding(excludeWalletId: String, chainId: String, query: String): List<WalletItem> {
        val wallets = if (chainId.isBlank()) {
            web3WalletDao.getWalletsExcludingByNameAllChains(excludeWalletId, query)
        } else {
            web3WalletDao.getWalletsExcludingByName(excludeWalletId, chainId, query)
        }
        return wallets.updateWithLocalKeyInfo(context)
    }

    suspend fun getAllWallets() = web3WalletDao.getAllWallets().map { it.updateWithLocalKeyInfo(context) }

    suspend fun getAllNoKeyWallets() = web3WalletDao.getAllWallets().map { it.updateWithLocalKeyInfo(context) }.filter {
        !it.hasLocalPrivateKey && (it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value || it.category == WalletCategory.IMPORTED_MNEMONIC.value)
    }

    suspend fun anyAddressExists(destinations: List<String>) = web3AddressDao.anyAddressExists(destinations)

    suspend fun allWeb3Tokens(walletIds: List<String>) = web3TokenDao.allWeb3Tokens(walletIds)

    suspend fun findUnifiedAssetItem(walletId: String) = web3TokenDao.findUnifiedAssetItem(walletId)
    suspend fun isAddressMatch(walletId: String, address: String): Boolean {
        return web3AddressDao.isAddressMatch(walletId, address)
    }

    suspend fun getAllWalletNames(categories :List<String>) = web3WalletDao.getAllWalletNames(categories)

    suspend fun getClassicWalletMaxIndex(): Int {
        return try {
            val classicWallets = web3WalletDao.getAllClassicWallets()

            if (classicWallets.isEmpty()) {
                return 0
            }

            var maxIndex = 0
            for (wallet in classicWallets) {
                val addresses = web3AddressDao.getAddressesByWalletId(wallet.id)
                for (address in addresses) {
                    address.path?.let { path ->
                        val index = CryptoWalletHelper.extractIndexFromPath(path)
                        if (index != null && index > maxIndex) {
                            maxIndex = index
                        }
                    }
                }
            }
            maxIndex
        } catch (e: Exception) {
            Timber.e(e, "Failed to get classic wallet max index")
            0
        }
    }

    suspend fun getWalletByDestination(destination: String) = web3AddressDao.getWalletByDestination(destination)?.updateWithLocalKeyInfo(MixinApplication.appContext)

    suspend fun getWalletByAddress(destination: String, chainId: String) = web3AddressDao.getWalletByAddress(destination, chainId)?.updateWithLocalKeyInfo(MixinApplication.appContext)

    // Only deposit display
    suspend fun getTokenByWalletAndAssetId(walletId: String, assetId: String): Web3TokenItem? {
        val localToken = web3TokenDao.web3TokenItemById(walletId, assetId)
        if (localToken != null) {
            return localToken
        }

        return try {
            val token = tokenRepository.findOrSyncAsset(assetId)
            token?.toWeb3TokenItem(walletId)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    suspend fun findChainById(chainId: String): Web3Chain? {
        return web3ChainDao.findChainById(chainId)
    }

    // Orders
    suspend fun inserOrders(orders: List<Order>) {
        if (orders.isEmpty()) return
        orderDao.insertListSuspend(orders)
    }

    suspend fun getPendingOrdersByWallet(walletId: String): List<Order> {
        return orderDao.getPendingOrdersByWallet(walletId)
    }

    suspend fun outputsByAddress(address: String, assetId: String): List<WalletOutput> = walletOutputDao.outputsByAddress(address, assetId)

    suspend fun outputsByAddressForSigning(address: String, assetId: String): List<WalletOutput> =
        walletOutputDao.outputsByAddressForSigning(address, assetId)

    suspend fun outputsByHash(hash: String, assetId: String): WalletOutput? = walletOutputDao.outputsByHash(hash, assetId)
}

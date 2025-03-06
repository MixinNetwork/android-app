package one.mixin.android.repository

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TokensExtraDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.safe.TokensExtra

@Singleton
class Web3Repository
@Inject
constructor(
    val web3TokenDao: Web3TokenDao,
    val web3TransactionDao: Web3TransactionDao,
    val web3TokensExtraDao: Web3TokensExtraDao,
    val web3AddressDao: Web3AddressDao,
    val web3WalletDao: Web3WalletDao
) {
    suspend fun insertWeb3Tokens(list: List<Web3Token>) = web3TokenDao.insertListSuspend(list)

    suspend fun web3TokenItemByChainId(chainId: String) = web3TokenDao.web3TokenItemByChainId(chainId)

    fun web3Tokens() = web3TokenDao.web3TokenItems()
    
    fun web3TokensExcludeHidden() = web3TokenDao.web3TokenItemsExcludeHidden()

    fun hiddenAssetItems() = web3TokenDao.hiddenAssetItems()
    
    suspend fun updateTokenHidden(tokenId: String, walletId: String, hidden: Boolean) {
        val tokensExtra = web3TokensExtraDao.findByAssetId(tokenId,  walletId)
        if (tokensExtra != null) {
            web3TokensExtraDao.updateHidden(tokenId, walletId, hidden)
        } else {
            web3TokensExtraDao.insertSuspend(Web3TokensExtra(tokenId, walletId, hidden,))
        }
    }

    fun web3Transactions(assetId: String) = web3TransactionDao.web3Transactions(assetId)
    
    suspend fun getAddressesByWalletId(walletId: String): List<Web3Address> {
        return web3AddressDao.getAddressesByWalletId(walletId)
    }

    suspend fun getClassicWalletId(): String? = web3WalletDao.getClassicWalletId()
}

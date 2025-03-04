package one.mixin.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TransactionDao

@Singleton
class Web3Repository
@Inject
constructor(
    val web3TokenDao: Web3TokenDao,
    val web3TransactionDao: Web3TransactionDao
) {
    suspend fun insertWeb3Tokens(list: List<Web3Token>) = web3TokenDao.insertListSuspend(list)

    fun web3Tokens() = web3TokenDao.web3TokenItems()

    fun web3Transactions(assetId: String) = web3TransactionDao.web3Transactions(assetId)
}

package one.mixin.android.ui.address

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import one.mixin.android.api.response.CashAccount
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAddressJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.vo.Address
import javax.inject.Inject

@HiltViewModel
class AddressViewModel
@Inject internal constructor(
    private val tokenRepository: TokenRepository,
    private val accountRepository: AccountRepository,
    private val web3Repository: Web3Repository,
    private val jobManager: MixinJobManager,
) : ViewModel() {
    fun addressesFlow(chainId: String) = tokenRepository.addressesFlow(chainId).map { list ->
        list.distinctBy { address ->
            Pair(address.destination, address.tag)
        }

    }

    fun allAddressesFlow() = tokenRepository.allAddressesFlow().map { list ->
        list.distinctBy { Triple(it.chainId, it.destination, it.tag) }
    }

    fun refreshAddressBook() {
        addressBookNetworks.forEach { network ->
            jobManager.addJobInBackground(RefreshAddressJob(network.chainId))
        }
    }

    fun recentAddressTransactions(address: Address) = tokenRepository.recentAddressTransactions(address)

    fun recentWeb3AddressTransactions(address: Address, walletId: String) = web3Repository.recentAddressTransactions(address, walletId)

    fun refreshAddressTransactions(walletId: String?) {
        if (walletId == null) {
            jobManager.addJobInBackground(RefreshSnapshotsJob())
        } else {
            jobManager.addJobInBackground(RefreshWeb3TransactionsJob(walletId))
        }
    }

    fun web3TokenItemById(walletId: String, assetId: String) = web3Repository.web3TokenItemById(walletId, assetId)

    suspend fun getChains() = tokenRepository.getChains()

    suspend fun findOrSyncAsset(assetId: String) = tokenRepository.findOrSyncAsset(assetId)

    suspend fun findWeb3WalletById(walletId: String) = web3Repository.findWalletById(walletId)

    suspend fun getSafeWalletsByChainId(chainId: String) = web3Repository.getSafeWalletsByChainId(chainId)

    suspend fun findCashAccount(): CashAccount? =
        PropertyHelper.findCashAccount()

    suspend fun validateExternalAddress(
        assetId: String, chain: String, destination: String, tag: String?
    ) = accountRepository.validateExternalAddress(assetId, chain, destination, tag)

}

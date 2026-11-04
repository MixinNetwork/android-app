package one.mixin.android.ui.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import one.mixin.android.vo.Chain
import one.mixin.android.vo.WalletCategory
import timber.log.Timber
import javax.inject.Inject

data class AddressBookState(
    val chains: List<Chain>,
    val walletChainIds: Set<String>?,
)

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

    private val addressBookStateFlow = MutableStateFlow<AddressBookState?>(null)
    val addressBookState: StateFlow<AddressBookState?> = addressBookStateFlow.asStateFlow()

    fun refreshAddressBook(walletId: String?) {
        viewModelScope.launch {
            val walletChainIds = walletChainIds(walletId)
            try {
                tokenRepository.refreshChains()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh chains for address book")
            }
            val chains = tokenRepository.getChains()
            addressBookStateFlow.value = AddressBookState(chains, walletChainIds)
            addressBookNetworks(chains, walletChainIds).forEach { network ->
                jobManager.addJobInBackground(RefreshAddressJob(network.chainId))
            }
        }
    }

    // Null keeps all privacy wallet chains; other wallets resolve their chain capabilities by category.
    suspend fun walletChainIds(walletId: String?): Set<String>? {
        if (walletId == null) return null
        val wallet = web3Repository.findWalletById(walletId)
        // Imported private key and watch wallets only know their chains from their addresses,
        // which may not be stored locally yet when the wallet was synced without them.
        val addresses = if (wallet?.category == WalletCategory.IMPORTED_PRIVATE_KEY.value || wallet?.category == WalletCategory.WATCH_ADDRESS.value) {
            web3Repository.getOrSyncAddresses(walletId)
        } else {
            web3Repository.getAddresses(walletId)
        }
        return addressBookWalletChainIds(
            wallet?.category,
            addresses.mapTo(HashSet()) { it.chainId },
            wallet?.safeChainId,
        )
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

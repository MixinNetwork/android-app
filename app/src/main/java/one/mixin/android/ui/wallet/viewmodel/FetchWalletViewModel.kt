package one.mixin.android.ui.wallet.viewmodel

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApp
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.response.web3.Web3WalletResponse
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.EthereumWallet
import one.mixin.android.crypto.SolanaWallet
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.job.RefreshWeb3Job
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.IndexedWallet
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FetchWalletViewModel @Inject constructor(
    private val web3Repository: Web3Repository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FetchWalletState.FETCHING)
    val state: StateFlow<FetchWalletState> = _state.asStateFlow()

    private val _wallets = MutableStateFlow<List<IndexedWallet>>(emptyList())
    val wallets: StateFlow<List<IndexedWallet>> = _wallets.asStateFlow()

    // Track selected WalletInfo objects
    private val _selectedWalletInfos = MutableStateFlow<Set<IndexedWallet>>(emptySet())
    val selectedWalletInfos: StateFlow<Set<IndexedWallet>> = _selectedWalletInfos.asStateFlow()

    private val _selectedAddresses = MutableStateFlow<Set<String>>(emptySet())
    val selectedAddresses: StateFlow<Set<String>> = _selectedAddresses.asStateFlow()

    private var mnemonic: String = ""

    private val _importSuccess = MutableStateFlow(false)
    val importSuccess: StateFlow<Boolean> = _importSuccess.asStateFlow()

    init {
        startFetching()
    }

    fun setMnemonic(mnemonic: String) {
        this.mnemonic = mnemonic
        startFetching()
    }

    private fun startFetching() {
        viewModelScope.launch {
            _state.value = FetchWalletState.FETCHING
            try {
                if (mnemonic.isNotBlank()) {
                    val wallets = (0 until 10).map { index ->
                        val ethereumWallet =
                            CryptoWalletHelper.mnemonicToEthereumWallet(mnemonic, index = index)
                        val solanaWallet =
                            CryptoWalletHelper.mnemonicToSolanaWallet(mnemonic, index = index)
                        IndexedWallet(index, ethereumWallet, solanaWallet)
                    }

                    val addresses = wallets.flatMap {
                        listOf(it.ethereumWallet.address, it.solanaWallet.address)
                    }
                    val response = web3Repository.searchAssetsByAddresses(addresses)
                    if (response.isSuccess && response.data != null) {
                        val tokensMap = response.data!!.associateBy(
                            { it.address },
                            { it.assets }
                        )
                        if (tokensMap.isEmpty()) {
                            _wallets.value = listOf(wallets[0])
                        }else {
                            val walletInfos = wallets.map { wallet ->
                                val evmTokens =
                                    tokensMap[wallet.ethereumWallet.address] ?: emptyList()
                                val solanaTokens =
                                    tokensMap[wallet.solanaWallet.address] ?: emptyList()
                                val allTokens = evmTokens + solanaTokens
                                IndexedWallet(
                                    wallet.index,
                                    wallet.ethereumWallet,
                                    wallet.solanaWallet,
                                    assets = allTokens
                                )
                            }
                            _wallets.value = walletInfos
                        }
                    } else {
                        _wallets.value = listOf(wallets[0])
                    }
                }
                _state.value = FetchWalletState.SELECT
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch wallet info")
                _state.value = FetchWalletState.SELECT
            }
        }
    }

    // Start importing selected wallet infos
    fun startImporting(selectedWalletInfos: Set<IndexedWallet>) {
        viewModelScope.launch {
            _state.value = FetchWalletState.IMPORTING
            _importSuccess.value = false
            try {
                // Create wallets for each selected wallet
                selectedWalletInfos.forEach { wallet ->
                    createWallet(wallet)
                }
                Timber.d("Successfully imported ${selectedWalletInfos.size} wallets")
                _importSuccess.value = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallets")
            }
        }
    }

    fun retry() {
        startFetching()
    }

    private suspend fun createWallet(indexedWallet: IndexedWallet) {
        // Create EVM wallet
        createWallet(
            name = "${MixinApplication.appContext.getString(R.string.Common_Wallet)} ${indexedWallet.index}",
            category = RefreshWeb3Job.WALLET_CATEGORY_PRIVATE,
            addresses = listOf(
                Web3AddressRequest(
                    destination = indexedWallet.ethereumWallet.address,
                    chainId = Constants.ChainId.ETHEREUM_CHAIN_ID
                )
                ,
                Web3AddressRequest(
                    destination = indexedWallet.solanaWallet.address,
                    chainId = Constants.ChainId.SOLANA_CHAIN_ID
                )
            )
        )
    }

    private suspend fun createWallet(name: String, category: String, addresses: List<Web3AddressRequest>) {
        val walletRequest = WalletRequest(
            name = name,
            category = category,
            addresses = addresses
        )

        requestRouteAPI(
            invokeNetwork = {
                web3Repository.createWallet(walletRequest)
            },
            successBlock = { response ->
                val wallet = response.data
                if (wallet != null) {
                    // Insert wallet and addresses into local database
                    insertWalletAndAddresses(wallet)
                } else {
                    Timber.e("Failed to create $category wallet: response data is null")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to create $category wallet: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userRepository.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }

    private suspend fun insertWalletAndAddresses(walletResponse: Web3WalletResponse) {
        val wallet = Web3Wallet(
            id = walletResponse.id,
            name = walletResponse.name,
            category = walletResponse.category,
            createdAt = walletResponse.createdAt,
            updatedAt = walletResponse.updatedAt
        )
        web3Repository.insertWallet(wallet)
        Timber.d("Created ${walletResponse.category} wallet with ID: ${walletResponse.id}")

        walletResponse.addresses?.let { addresses ->
            web3Repository.insertAddressList(addresses)
            Timber.d("Inserted wallet with ID: ${walletResponse.id}, ${addresses.size} addresses")
        }
    }

    fun toggleWalletSelection(wallet: IndexedWallet) {
        val current = _selectedWalletInfos.value
        _selectedWalletInfos.value = if (current.contains(wallet)) current - wallet else current + wallet
    }

    fun selectAll() {
        _selectedWalletInfos.value = _wallets.value.toSet()
    }
}

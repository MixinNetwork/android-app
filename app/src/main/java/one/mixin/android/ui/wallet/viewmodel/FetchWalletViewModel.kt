package one.mixin.android.ui.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.EthereumWallet
import one.mixin.android.repository.Web3Repository
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.WalletInfo
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FetchWalletViewModel @Inject constructor(
    private val web3Repository: Web3Repository
) : ViewModel() {

    private val _state = MutableStateFlow(FetchWalletState.FETCHING)
    val state: StateFlow<FetchWalletState> = _state.asStateFlow()

    private val _wallets = MutableStateFlow<List<WalletInfo>>(emptyList())
    val wallets: StateFlow<List<WalletInfo>> = _wallets.asStateFlow()

    // Track selected WalletInfo objects
    private val _selectedWalletInfos = MutableStateFlow<Set<WalletInfo>>(emptySet())
    val selectedWalletInfos: StateFlow<Set<WalletInfo>> = _selectedWalletInfos.asStateFlow()

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
                    data class IndexedWallet(val index: Int, val wallet: Any, val address: String)

                    val multiWallets = (0 until 10).flatMap { index ->
                        listOf(
                            CryptoWalletHelper.mnemonicToEthereumWallet(mnemonic, index = index)?.let {
                                IndexedWallet(index, it, it.address)
                            },
                            CryptoWalletHelper.mnemonicToSolanaWallet(mnemonic, index = index).let {
                                IndexedWallet(index, it, it.address)
                            }
                        )
                    }.filterNotNull()

                    val addresses = multiWallets.map { it.address }
                    val a = mutableListOf<String>()
                    a.addAll(addresses)
                    val response = web3Repository.searchAssetsByAddresses(a)
                    if (response.isSuccess && response.data != null) {
                        val assetsList = response.data!!
                        val walletInfos = assetsList.map { assetsView ->
                            val wallet = multiWallets.find { it.address == assetsView.address }
                            val balance = assetsView.assets
                                .sumOf { it.amount.toBigDecimal() }
                                .toPlainString()
                            WalletInfo(
                                wallet?.index ?: -1,
                                if (wallet?.wallet is EthereumWallet) Constants.ChainId.ETHEREUM_CHAIN_ID else Constants.ChainId.Solana,
                                assetsView.address,
                                balance
                            )
                        }
                        _wallets.value = walletInfos
                    } else {
                        _wallets.value = multiWallets.map {
                            WalletInfo(
                                it.index,
                                if (it.wallet is EthereumWallet) Constants.ChainId.ETHEREUM_CHAIN_ID else Constants.ChainId.Solana,
                                it.address
                            )
                        }
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
    fun startImporting(selectedWalletInfos: Set<WalletInfo>) {
        viewModelScope.launch {
            _state.value = FetchWalletState.IMPORTING
            _importSuccess.value = false
            try {
                // Create address records for each selected wallet
                selectedWalletInfos.forEach { wallet ->
                    val addressRequest = Web3AddressRequest(
                        destination = wallet.address,
                        chainId = wallet.chainId
                    )
                    val response = web3Repository.createAddress(addressRequest)
                    if (response.isSuccess) {
                        Timber.d("Created address for ${'$'}{wallet.address}")
                    } else {
                        Timber.e("Failed to create address for ${'$'}{wallet.address}")
                    }
                }
                Timber.d("Successfully imported ${'$'}{selectedWalletInfos.size} wallets")
                _importSuccess.value = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallets")
            }
        }
    }

    fun retry() {
        startFetching()
    }

    // Toggle selection of a WalletInfo object
    fun toggleWalletSelection(wallet: WalletInfo) {
        val current = _selectedWalletInfos.value
        _selectedWalletInfos.value = if (current.contains(wallet)) current - wallet else current + wallet
    }

    // Select all WalletInfo objects
    fun selectAll() {
        _selectedWalletInfos.value = _wallets.value.toSet()
    }
}

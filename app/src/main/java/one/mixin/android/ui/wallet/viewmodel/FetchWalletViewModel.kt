package one.mixin.android.ui.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.crypto.CryptoWalletHelper
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

    private val _selectedWallets = MutableStateFlow<Set<Int>>(emptySet())
    val selectedWallets: StateFlow<Set<Int>> = _selectedWallets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var mnemonic: String = ""

    init {
        startFetching()
    }

    fun setMnemonic(mnemonic: String) {
        this.mnemonic = mnemonic
        startFetching()
    }

    private fun startFetching() {
        viewModelScope.launch {
            _isLoading.value = true
            _state.value = FetchWalletState.FETCHING
            try {
                if (mnemonic.isNotBlank()) {
                    // Generate wallets from mnemonic
                    val multiWallets = (0 until 10).map { index ->
                        CryptoWalletHelper.mnemonicToEthereumWallet(mnemonic, index = index)
                    }
                    val addresses = multiWallets.map { it.address }
                    val a = mutableListOf<String>()
                    a.addAll(addresses)
                    a.add("BLeUXTx9thHGT7VJUtF9vHEmfMDgW1nnKZ9UVer2CoLX")
                    val response = web3Repository.searchAssetsByAddresses(a)
                    if (response.isSuccess && response.data != null) {
                        val assetsList = response.data!!
                        val walletInfos = assetsList.map { assetsView ->
                            val wallet = multiWallets.find { it.address == assetsView.address }
                            val balance = assetsView.assets
                                .sumOf { it.amount.toBigDecimal() }
                                .toPlainString()
                            WalletInfo(wallet?.index ?: -1, assetsView.address, balance)
                        }
                        _wallets.value = walletInfos
                    } else {
                        _wallets.value = multiWallets.map { WalletInfo(it.index, it.address) }
                    }
                }
                _state.value = FetchWalletState.SELECT
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch wallet info")
                _state.value = FetchWalletState.SELECT
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleWalletSelection(index: Int) {
        val current = _selectedWallets.value.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        _selectedWallets.value = current
    }

    /** Select all wallets available */
    fun selectAll() {
        _selectedWallets.value = _wallets.value.map { it.index }.toSet()
    }

    /** Clear all selected wallets */
    fun clearAll() {
        _selectedWallets.value = emptySet()
    }

    fun startImporting() {
        viewModelScope.launch {
            _state.value = FetchWalletState.IMPORTING
            _isLoading.value = true

            try {
                delay(2000)

                Timber.d("Successfully imported \\${_selectedWallets.value.size} wallets")
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallets")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        startFetching()
    }
}

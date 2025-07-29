package one.mixin.android.ui.wallet.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSingleWalletJob
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.Tip
import one.mixin.android.ui.wallet.WalletSecurityActivity
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.IndexedWallet
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.encodeToBase58String
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSigner
import org.sol4k.Keypair
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class FetchWalletViewModel @Inject constructor(
    private val tip: Tip,
    private val jobManager: MixinJobManager,
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

    private val _errorCode = MutableStateFlow<Int?>(null)
    val errorCode: StateFlow<Int?> = _errorCode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _partialSuccess = MutableStateFlow<Boolean?>(null)
    val partialSuccess: StateFlow<Boolean?> = _partialSuccess.asStateFlow()

    private var mnemonic: String = ""
    private var currentIndex = 0
    private var spendKey: ByteArray? = null

    init {
        startFetching(0)
    }

    fun setMnemonic(mnemonic: String) {
        this.mnemonic = mnemonic
        _wallets.value = emptyList()
        currentIndex = 0
        startFetching(0)
    }

    fun setSpendKey(spendKey: ByteArray) {
        this.spendKey = spendKey
    }

    fun getSpendKey(): ByteArray? {
        return spendKey
    }

    fun findMoreWallets() {
        currentIndex += 10
        startFetching(currentIndex)
    }

    private fun startFetching(offset: Int) {
        viewModelScope.launch {
            _state.value = FetchWalletState.FETCHING
            try {
                if (mnemonic.isNotBlank()) {
                    val names = web3Repository.getAllWalletNames(listOf(WalletCategory.IMPORTED_PRIVATE_KEY.value, WalletCategory.IMPORTED_MNEMONIC.value))
                    val commonWalletName = MixinApplication.appContext.getString(R.string.Common_Wallet)
                    val regex = """^$commonWalletName (\d+)$""".toRegex()
                    val maxIndex = names
                        .filterNotNull()
                        .mapNotNull { name ->
                            regex.find(name)?.groupValues?.get(1)?.toIntOrNull()
                        }.maxOrNull() ?: 0

                    val wallets = (offset until offset + 10).map { index ->
                        val ethereumWallet =
                            CryptoWalletHelper.mnemonicToEthereumWallet(mnemonic, index = index)
                        val solanaWallet =
                            CryptoWalletHelper.mnemonicToSolanaWallet(mnemonic, index = index)
                        IndexedWallet(maxIndex + index + 1, ethereumWallet, solanaWallet, exists = web3Repository.anyAddressExists(listOf(ethereumWallet.address, solanaWallet.address)))
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
                            if (offset == 0) {
                                _wallets.value = listOf(wallets[0])
                            }
                        } else {
                            val walletInfos = wallets.map { wallet ->
                                val evmTokens =
                                    tokensMap[wallet.ethereumWallet.address] ?: emptyList()
                                val solanaTokens =
                                    tokensMap[wallet.solanaWallet.address] ?: emptyList()
                                val allTokens = (evmTokens + solanaTokens).sortedByDescending {
                                    (it.priceUSD.toBigDecimalOrNull() ?: BigDecimal.ZERO) * (it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                }
                                IndexedWallet(
                                    wallet.index,
                                    wallet.ethereumWallet,
                                    wallet.solanaWallet,
                                    exists = wallet.exists,
                                    assets = allTokens
                                )
                            }.filter { it.assets.isNotEmpty() }
                            if (offset == 0 && walletInfos.isEmpty()) {
                                _wallets.value = listOf(wallets[0])
                            } else {
                                _selectedWalletInfos.value = (walletInfos.filter { it.exists.not() } + _selectedWalletInfos.value).toSet()
                                _wallets.value = _wallets.value + walletInfos
                            }
                        }
                    } else {
                        if (offset == 0) {
                            _wallets.value = listOf(wallets[0])
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
    fun startImporting() {
        viewModelScope.launch {
            _state.value = FetchWalletState.IMPORTING
            try {
                val walletsToCreate = selectedWalletInfos.value.map {
                    val name = "${MixinApplication.appContext.getString(R.string.Common_Wallet)} ${it.index}"
                    val category = WalletCategory.IMPORTED_MNEMONIC.value
                    val addresses = listOf(
                        Web3AddressRequest(
                            destination = it.ethereumWallet.address,
                            chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                            path = it.ethereumWallet.path
                        ),
                        Web3AddressRequest(
                            destination = it.solanaWallet.address,
                            chainId = Constants.ChainId.SOLANA_CHAIN_ID,
                            path = it.solanaWallet.path
                        )
                    )
                    Pair(WalletRequest(name, category, addresses), it.solanaWallet.mnemonic.split(" "))
                }

                val expectedCount = walletsToCreate.size
                val successCount = saveWallets(walletsToCreate)

                Timber.d("Import completed: $successCount/$expectedCount wallets imported successfully")
                if (expectedCount == successCount) {
                    _state.value = FetchWalletState.IMPORT_SUCCESS
                } else {
                    _partialSuccess.value = successCount > 0
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallets")
                _errorCode.value = null
                _errorMessage.value = e.message
                _state.value = FetchWalletState.IMPORT_ERROR
            }
        }
    }

    private suspend fun saveWallets(walletsToCreate: List<Pair<WalletRequest, List<String>>>): Int {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return 0
        }

        var successCount = 0

        for ((walletRequest, words) in walletsToCreate) {
            val result = requestRouteAPI(
                invokeNetwork = { web3Repository.createWallet(walletRequest) },
                successBlock = { response ->
                    response.data?.let { wallet ->
                        web3Repository.insertWallet(
                            Web3Wallet(
                                id = wallet.id,
                                name = wallet.name,
                                category = wallet.category,
                                createdAt = wallet.createdAt,
                                updatedAt = wallet.updatedAt,
                            )
                        )
                        saveWeb3PrivateKey(MixinApplication.appContext, currentSpendKey, wallet.id, words)
                        jobManager.addJobInBackground(RefreshSingleWalletJob(wallet.id))
                        successCount++
                    }
                },
                failureBlock = { response ->
                    Timber.e("Failed to create wallet: ${response.errorCode} - ${response.errorDescription}")
                    _errorCode.value = response.errorCode
                    _errorMessage.value = MixinApplication.appContext.getMixinErrorStringByCode(response.errorCode, response.errorDescription)
                    _state.value = FetchWalletState.IMPORT_ERROR
                    true
                },
                requestSession = { userRepository.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
                exceptionBlock = {
                    _errorMessage.value = ErrorHandler.getErrorMessage(it)
                    _state.value = FetchWalletState.IMPORT_ERROR
                    true
                }
            )
            if (result == null) break
        }

        return successCount
    }

    suspend fun getAddressesByChainId(walletId: String, chainId: String): Web3Address? {
        return web3Repository.getAddressesByChainId(walletId, chainId)
    }

    fun toggleWalletSelection(wallet: IndexedWallet) {
        val current = _selectedWalletInfos.value
        _selectedWalletInfos.value = if (current.contains(wallet)) current - wallet else current + wallet
    }

    fun selectAll() {
        if(_selectedWalletInfos.value.size == _wallets.value.filter { it.exists.not() }.size) {
            _selectedWalletInfos.value = emptySet()
        } else {
            _selectedWalletInfos.value = _wallets.value.filter { it.exists.not() }.toSet()
        }
    }

    fun saveWeb3PrivateKey(context: Context, spendKey: ByteArray, walletId: String, words: List<String>): Boolean {
        return try {
            val encryptedString = CryptoWalletHelper.encryptMnemonicWithSpendKey(spendKey, words)
            CryptoWalletHelper.saveWeb3PrivateKey(context, walletId, encryptedString)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save web3 private key")
            false
        }
    }

    fun getWeb3Priva(context: Context, chainId: String?, walletId: String?): String? {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return null
        }
        if (chainId == null) {
            Timber.e("Chain ID is null, cannot get private key.")
            return null
        }
        if (walletId != JsSigner.currentWalletId) {
            Timber.e("Wallet ID does not match current wallet ID, cannot get private key.")
            return null
        }
        return CryptoWalletHelper.getWeb3PrivateKey(context, currentSpendKey, chainId)?.let {
            if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
                val kp = Keypair.fromSecretKey(it)
                kp.secret.encodeToBase58String()
            } else {
                Numeric.toHexString(it)
            }
        }
    }

    fun getWeb3Mnemonic(context: Context): String? {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return null
        }
        return CryptoWalletHelper.getWeb3Mnemonic(context, currentSpendKey, JsSigner.currentWalletId)
    }

    fun importWallet(key: String, chainId: String, mode: WalletSecurityActivity.Mode) {
        viewModelScope.launch {
            try {
                val address: String
                val category: WalletCategory
                val names = web3Repository.getAllWalletNames(if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS) listOf(WalletCategory.WATCH_ADDRESS.value) else listOf(WalletCategory.IMPORTED_PRIVATE_KEY.value, WalletCategory.IMPORTED_MNEMONIC.value))
                val commonWalletName = MixinApplication.appContext.getString(R.string.Common_Wallet)
                val regex = """^$commonWalletName (\d+)$""".toRegex()
                val maxIndex = names
                    .filterNotNull()
                    .mapNotNull { name ->
                        regex.find(name)?.groupValues?.get(1)?.toIntOrNull()
                    }.maxOrNull() ?: 0

                val name = "${MixinApplication.appContext.getString(if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS) R.string.Watch_Wallet else R.string.Common_Wallet)} ${maxIndex + 1}"
                _state.value = FetchWalletState.IMPORTING
                when (mode) {
                    WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY -> {
                        address = CryptoWalletHelper.privateKeyToAddress(key, chainId)
                        category = WalletCategory.IMPORTED_PRIVATE_KEY
                    }
                    WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> {
                        address = key
                        category = WalletCategory.WATCH_ADDRESS
                    }
                    else -> {
                        throw IllegalArgumentException("Unsupported mode for import: $mode")
                    }
                }

                if (address.isBlank()) {
                    throw IllegalArgumentException("Could not derive or find address.")
                }

                val web3AddressRequest = Web3AddressRequest(
                    destination = address,
                    chainId = chainId,
                    path = null
                )

                val walletRequest = WalletRequest(
                    name = name,
                    category = category.value,
                    addresses = listOf(web3AddressRequest)
                )
                if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS) {
                    saveImportedWallet(walletRequest, null)
                } else {
                    saveImportedWallet(walletRequest, key)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallet")
                _state.value = FetchWalletState.SELECT
            }
        }
    }

    private suspend fun saveImportedWallet(walletRequest: WalletRequest, privateKey: String?) {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            _errorMessage.value = "Spend key is null"
            _state.value = FetchWalletState.IMPORT_ERROR
            return
        }

        requestRouteAPI(
            invokeNetwork = { web3Repository.createWallet(walletRequest) },
            successBlock = { response ->
                response.data?.let { wallet ->
                    web3Repository.insertWallet(
                        Web3Wallet(
                            id = wallet.id,
                            name = wallet.name,
                            category = wallet.category,
                            createdAt = wallet.createdAt,
                            updatedAt = wallet.updatedAt,
                        )
                    )
                    if (privateKey.isNullOrEmpty().not()) {
                        saveWeb3ImportedPrivateKey(MixinApplication.appContext, currentSpendKey, wallet.id, privateKey)
                    }
                    jobManager.addJobInBackground(RefreshSingleWalletJob(wallet.id))
                    Timber.d("Successfully imported wallet ${wallet.id}")

                    _state.value = FetchWalletState.IMPORT_SUCCESS
                }
            },
            failureBlock = { response ->
                _errorCode.value = response.errorCode
                _errorMessage.value = MixinApplication.appContext.getMixinErrorStringByCode(response.errorCode, response.errorDescription)
                _state.value = FetchWalletState.IMPORT_ERROR
                Timber.e("Failed to create wallet: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = { userRepository.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            exceptionBlock = {
                _errorMessage.value = ErrorHandler.getErrorMessage(it)
                true
            }
        )
    }

    private fun saveWeb3ImportedPrivateKey(context: Context, spendKey: ByteArray, walletId: String, privateKey: String): Boolean {
        return try {
            val encryptedString = CryptoWalletHelper.encryptPrivateKeyWithSpendKey(spendKey, privateKey)
            CryptoWalletHelper.saveWeb3PrivateKey(context, walletId, encryptedString)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save web3 private key")
            false
        }
    }

    fun savePrivateKey(walletId: String, privateKey: String) {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return
        }
        saveWeb3ImportedPrivateKey(MixinApplication.appContext, currentSpendKey, walletId, privateKey)
    }
}

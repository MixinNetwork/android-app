package one.mixin.android.ui.wallet.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSingleWalletJob
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.Tip
import one.mixin.android.ui.wallet.buildClassicWalletRequest
import one.mixin.android.ui.wallet.classicWalletIndexForCreation
import one.mixin.android.ui.wallet.createSignedWeb3AddressRequest
import one.mixin.android.ui.wallet.WalletSecurityActivity
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.IndexedWallet
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.defaultWalletSelection
import one.mixin.android.ui.wallet.components.fetchWalletFailureState
import one.mixin.android.ui.wallet.components.fetchWalletMissingMnemonicState
import one.mixin.android.ui.wallet.components.shouldStartWalletFetch
import one.mixin.android.ui.wallet.components.walletDestinationForWallet
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.ui.tip.matchesMnemonicWalletAddresses
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.encodeToBase58String
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.Web3Signer
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.crypto.DumpedPrivateKey
import org.bitcoinj.crypto.ECKey
import org.sol4k.Base58
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

    private val _errorCode = MutableStateFlow<Int?>(null)
    val errorCode: StateFlow<Int?> = _errorCode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _partialSuccess = MutableStateFlow<Boolean?>(null)
    val partialSuccess: StateFlow<Boolean?> = _partialSuccess.asStateFlow()

    private val _importedWalletDestination = MutableStateFlow<WalletDestination?>(null)
    val importedWalletDestination: StateFlow<WalletDestination?> = _importedWalletDestination.asStateFlow()

    suspend fun getAllNoKeyWallets() = web3Repository.getAllNoKeyWallets()

    private var mnemonic: String = ""
    private var currentIndex = 0
    private var spendKey: ByteArray? = null
    private var importCategory: String = WalletCategory.IMPORTED_MNEMONIC.value

    init {
        startFetching(0)
    }

    private fun getBitcoinPrivateKeyBytes(privateKey: String): ByteArray {
        return if (privateKey.length in 51..52 && (privateKey.startsWith("5") || privateKey.startsWith("K") || privateKey.startsWith("L"))) {
            DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKey).key.privKeyBytes
        } else {
            Numeric.hexStringToByteArray(privateKey)
        }
    }

    fun setMnemonic(mnemonic: String) {
        this.mnemonic = mnemonic
        _wallets.value = emptyList()
        _selectedWalletInfos.value = emptySet()
        _importedWalletDestination.value = null
        currentIndex = 0
        startFetching(0)
    }

    fun setSpendKey(spendKey: ByteArray) {
        this.spendKey = spendKey
    }

    fun setImportCategory(category: String) {
        importCategory = category
    }

    fun getSpendKey(): ByteArray? {
        return spendKey
    }

    suspend fun getDefaultImportWalletName(mode: WalletSecurityActivity.Mode): String {
        val categories = if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS) {
            listOf(WalletCategory.WATCH_ADDRESS.value)
        } else {
            listOf(
                WalletCategory.CLASSIC.value,
                WalletCategory.IMPORTED_PRIVATE_KEY.value,
                WalletCategory.IMPORTED_MNEMONIC.value
            )
        }
        val names = web3Repository.getAllWalletNames(categories)
        val walletName = MixinApplication.appContext.getString(
            if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS) {
                R.string.Watch_Wallet
            } else {
                R.string.Common_Wallet
            }
        )
        val regex = """^$walletName (\d+)$""".toRegex()
        val maxIndex = names
            .filterNotNull()
            .mapNotNull { name ->
                regex.find(name)?.groupValues?.get(1)?.toIntOrNull()
            }
            .maxOrNull() ?: 0

        return "$walletName ${maxIndex + 1}"
    }

    fun findMoreWallets() {
        currentIndex += 10
        startFetching(currentIndex)
    }

    fun retryFetching() {
        Timber.i("LoginFlow wallet_fetch_retry offset=$currentIndex")
        startFetching(currentIndex)
    }

    fun failFetching(errorMessage: String?) {
        Timber.i("LoginFlow wallet_fetch_failed source=prepare has_existing_wallets=${_wallets.value.isNotEmpty()}")
        _errorCode.value = null
        _errorMessage.value = errorMessage
        _state.value = fetchWalletFailureState(_wallets.value.isNotEmpty())
    }

    private var localMaxIndex: Int = 0

    private fun startFetching(offset: Int) {
        viewModelScope.launch {
            _state.value = FetchWalletState.FETCHING
            try {
                if (!shouldStartWalletFetch(mnemonic)) {
                    Timber.i("LoginFlow wallet_fetch_skip reason=missing_mnemonic")
                    _state.value = fetchWalletMissingMnemonicState()
                    return@launch
                }
                if (restoreExistingMnemonicWallets(mnemonic)) {
                    return@launch
                }
                Timber.i("LoginFlow wallet_fetch_start offset=$offset import_category=$importCategory")
                if (localMaxIndex == 0) {
                    val names = web3Repository.getAllWalletNames(listOf(WalletCategory.CLASSIC.value, WalletCategory.IMPORTED_PRIVATE_KEY.value, WalletCategory.IMPORTED_MNEMONIC.value))
                    val commonWalletName = MixinApplication.appContext.getString(R.string.Common_Wallet)
                    val regex = """^$commonWalletName (\d+)$""".toRegex()
                    localMaxIndex = names
                        .filterNotNull()
                        .mapNotNull { name ->
                            regex.find(name)?.groupValues?.get(1)?.toIntOrNull()
                        }.maxOrNull() ?: 0
                }

                val wallets = (offset until offset + 10).map { index ->
                    val ethereumWallet =
                        CryptoWalletHelper.mnemonicToEthereumWallet(mnemonic, index = index)
                    val solanaWallet =
                        CryptoWalletHelper.mnemonicToSolanaWallet(mnemonic, index = index)
                    val btcWallet = CryptoWalletHelper.mnemonicToBitcoinSegwitWallet(mnemonic, index = index)

                    val name = "${MixinApplication.appContext.getString(R.string.Common_Wallet)} ${localMaxIndex + 1}"
                    IndexedWallet(
                        name = name,
                        ethereumWallet = ethereumWallet,
                        solanaWallet = solanaWallet,
                        btcWallet = btcWallet,
                        exists = web3Repository.anyAddressExists(listOf(ethereumWallet.address, solanaWallet.address, btcWallet.address)),
                    )
                }

                val addresses = wallets.flatMap {
                    listOf(it.ethereumWallet.address, it.solanaWallet.address, it.btcWallet.address)
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
                            _selectedWalletInfos.value = defaultWalletSelection(_wallets.value)
                        }
                        Timber.i("LoginFlow wallet_fetch_result success=true offset=$offset found_wallets=0 default_wallet=${offset == 0}")
                    } else {
                        val walletInfos = wallets.map { wallet ->
                            val evmTokens =
                                tokensMap[wallet.ethereumWallet.address] ?: emptyList()
                            val solanaTokens =
                                tokensMap[wallet.solanaWallet.address] ?: emptyList()
                            val btcTokens =
                                tokensMap[wallet.btcWallet.address] ?: emptyList()
                            val allTokens = (evmTokens + solanaTokens + btcTokens).sortedByDescending {
                                (it.priceUSD.toBigDecimalOrNull() ?: BigDecimal.ZERO) * (it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                            }
                            wallet.copy(assets = allTokens)
                        }.filter { it.assets.isNotEmpty() }.map { wallet ->
                            localMaxIndex ++
                            val name = "${MixinApplication.appContext.getString(R.string.Common_Wallet)} $localMaxIndex"
                            wallet.copy(name = name)
                        }
                        if (offset == 0 && walletInfos.isEmpty()) {
                            localMaxIndex ++
                            _wallets.value = listOf(wallets[0])
                            _selectedWalletInfos.value = defaultWalletSelection(_wallets.value)
                        } else {
                            _wallets.value = _wallets.value + walletInfos
                            _selectedWalletInfos.value = (
                                walletInfos.filter { !it.exists } + _selectedWalletInfos.value
                            ).toSet()
                        }
                        Timber.i(
                            "LoginFlow wallet_fetch_result success=true offset=$offset found_wallets=${walletInfos.size} selected_count=${_selectedWalletInfos.value.size}"
                        )
                    }
                } else {
                    Timber.i("LoginFlow wallet_fetch_result success=false offset=$offset code=${response.errorCode}")
                    _errorCode.value = response.errorCode
                    _errorMessage.value = MixinApplication.appContext.getMixinErrorStringByCode(response.errorCode, response.errorDescription)
                    _state.value = fetchWalletFailureState(_wallets.value.isNotEmpty())
                    return@launch
                }
                _state.value = FetchWalletState.SELECT
            } catch (e: Exception) {
                Timber.i("LoginFlow wallet_fetch_result success=false offset=$offset exception=true")
                Timber.e(e, "Failed to fetch wallet info")
                _errorCode.value = null
                _errorMessage.value = ErrorHandler.getErrorMessage(e)
                _state.value = fetchWalletFailureState(_wallets.value.isNotEmpty())
            }
        }
    }

    private suspend fun restoreExistingMnemonicWallets(mnemonic: String): Boolean {
        if (importCategory != WalletCategory.IMPORTED_MNEMONIC.value) return false

        val matchingWallets = buildList {
            web3Repository.getAllNoKeyWallets().forEach { wallet ->
                if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value &&
                    matchesMnemonicWalletAddresses(mnemonic, web3Repository.getAddresses(wallet.id))
                ) {
                    add(wallet)
                }
            }
        }
        if (matchingWallets.isEmpty()) return false

        Timber.i("LoginFlow wallet_import_local_reuse matched_count=${matchingWallets.size}")
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            _errorCode.value = null
            _errorMessage.value = MixinApplication.appContext.getString(R.string.Save_failure)
            _state.value = FetchWalletState.IMPORT_ERROR
            return true
        }

        for (wallet in matchingWallets) {
            val saved = saveWeb3PrivateKey(
                MixinApplication.appContext,
                currentSpendKey,
                wallet.id,
                mnemonic.split(" "),
            )
            if (!saved) {
                Timber.e("LoginFlow wallet_import_local_reuse_failed wallet_id=${wallet.id}")
                _errorCode.value = null
                _errorMessage.value = MixinApplication.appContext.getString(R.string.Save_failure)
                _state.value = FetchWalletState.IMPORT_ERROR
                return true
            }
            jobManager.addJobInBackground(RefreshSingleWalletJob(wallet.id))
        }

        val wallet = matchingWallets.first()
        selectImportedWalletIfNeeded(wallet.id, wallet.category)
        _state.value = FetchWalletState.IMPORT_SUCCESS
        return true
    }

    // Start importing selected wallet infos
    fun startImporting() {
        viewModelScope.launch {
            _importedWalletDestination.value = null
            _state.value = FetchWalletState.IMPORTING
            try {
                Timber.i("LoginFlow wallet_import_viewmodel_start selected_count=${selectedWalletInfos.value.size} category=$importCategory")
                val walletsToCreate = selectedWalletInfos.value.map {
                    val category = importCategory
                    val addresses = listOf(
                        createSignedWeb3AddressRequest(
                            destination = it.btcWallet.address,
                            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                            path = it.btcWallet.path,
                            privateKey = it.btcWallet.privateKey,
                            category = category
                        ),
                        createSignedWeb3AddressRequest(
                            destination = it.ethereumWallet.address,
                            chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                            path = it.ethereumWallet.path,
                            privateKey = it.ethereumWallet.privateKey,
                            category = category
                        ),
                        createSignedWeb3AddressRequest(
                            destination = it.solanaWallet.address,
                            chainId = Constants.ChainId.SOLANA_CHAIN_ID,
                            path = it.solanaWallet.path,
                            privateKey = it.solanaWallet.privateKey,
                            category = category
                        )
                    )
                    Pair(WalletRequest(it.name, category, addresses), it.solanaWallet.mnemonic.split(" "))
                }

                val expectedCount = walletsToCreate.size
                val successCount = saveWallets(walletsToCreate)

                Timber.i("LoginFlow wallet_import_viewmodel_result success_count=$successCount expected_count=$expectedCount")
                if (expectedCount == successCount) {
                    _state.value = FetchWalletState.IMPORT_SUCCESS
                } else {
                    _partialSuccess.value = successCount > 0
                }
            } catch (e: Exception) {
                Timber.i("LoginFlow wallet_import_viewmodel_exception")
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
            Timber.i("LoginFlow wallet_import_save_failed reason=missing_spend_key")
            Timber.e("Spend key is null, cannot save wallets.")
            return 0
        }

        var successCount = 0

        for ((walletRequest, words) in walletsToCreate) {
            var privateKeySaveFailed = false
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
                        wallet.addresses?.takeIf { it.isNotEmpty() }?.let { addresses ->
                            web3Repository.insertAddressList(addresses)
                        }
                        val privateKeySaved = try {
                            saveWeb3PrivateKey(MixinApplication.appContext, currentSpendKey, wallet.id, words)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to save imported wallet mnemonic")
                            false
                        }
                        if (privateKeySaved) {
                            selectImportedWalletIfNeeded(wallet.id, wallet.category)
                            jobManager.addJobInBackground(RefreshSingleWalletJob(wallet.id))
                            successCount++
                        } else {
                            Timber.e("LoginFlow wallet_import_save_failed reason=private_key wallet_id=${wallet.id}")
                            _errorCode.value = null
                            _errorMessage.value = MixinApplication.appContext.getString(R.string.Save_failure)
                            _state.value = FetchWalletState.IMPORT_ERROR
                            privateKeySaveFailed = true
                        }
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
            if (result == null || privateKeySaveFailed) break
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
        if (_selectedWalletInfos.value.size == _wallets.value.filter { it.exists.not() }.size) {
            _selectedWalletInfos.value = emptySet()
        } else {
            _selectedWalletInfos.value = _wallets.value.filter { it.exists.not() }.toSet()
        }
    }

    fun saveWeb3PrivateKey(context: Context, spendKey: ByteArray, walletId: String, words: List<String>): Boolean {
        return CryptoWalletHelper.saveMnemonicWithSpendKey(context, spendKey, walletId, words)
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
        if (walletId != Web3Signer.currentWalletId) {
            Timber.e("Wallet ID does not match current wallet ID, cannot get private key.")
            return null
        }
        return CryptoWalletHelper.getWeb3PrivateKey(context, currentSpendKey, chainId)?.let {
            when (chainId) {
                Constants.ChainId.SOLANA_CHAIN_ID -> {
                    val kp = Keypair.fromSecretKey(it)
                    kp.secret.encodeToBase58String()
                }
                Constants.ChainId.BITCOIN_CHAIN_ID -> {
                    val ecKey: ECKey = ECKey.fromPrivate(it, true)
                    ecKey.getPrivateKeyEncoded(BitcoinNetwork.MAINNET).toBase58()
                }
                in Constants.Web3EvmChainIds -> {
                    Numeric.toHexString(it)
                }
                else -> {
                    null
                }
            }
        }
    }

    fun getWeb3Mnemonic(context: Context): String? {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return null
        }
        return CryptoWalletHelper.getWeb3Mnemonic(context, currentSpendKey, Web3Signer.currentWalletId)
    }

    fun importWallet(key: String, chainId: String, mode: WalletSecurityActivity.Mode, walletName: String? = null) {
        viewModelScope.launch {
            try {
                val address: String
                val category: WalletCategory
                _importedWalletDestination.value = null
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
                val name = walletName?.trim()?.takeIf { it.isNotEmpty() } ?: getDefaultImportWalletName(mode)
                val web3AddressRequest = createSignedWeb3AddressRequest(
                    destination = address,
                    chainId = chainId,
                    path = null,
                    privateKey = key.let {
                        if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
                            // Solana private keys are provided as Base58; EVM private keys are provided as hex.
                            Base58.decode(key)
                        } else if (chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                            getBitcoinPrivateKeyBytes(key)
                        } else {
                            Numeric.hexStringToByteArray(key)
                        }
                    },
                    category = category.value
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
                    wallet.addresses?.takeIf { it.isNotEmpty() }?.let { addresses ->
                        web3Repository.insertAddressList(addresses)
                    }
                    selectImportedWalletIfNeeded(wallet.id, wallet.category)
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
        return CryptoWalletHelper.savePrivateKeyWithSpendKey(context, spendKey, walletId, privateKey)
    }

    fun savePrivateKey(walletId: String, chainId: String, privateKey: String) {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return
        }
        val normalizedPrivateKey: String = normalizeImportedPrivateKey(chainId, privateKey) ?: run {
            Timber.e("Invalid private key format for chainId: %s", chainId)
            return
        }
        saveWeb3ImportedPrivateKey(MixinApplication.appContext, currentSpendKey, walletId, normalizedPrivateKey)
    }

    private fun normalizeImportedPrivateKey(chainId: String, privateKey: String): String? {
        return when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> normalizeBitcoinPrivateKeyToWif(privateKey)
            else -> privateKey
        }
    }

    private fun normalizeBitcoinPrivateKeyToWif(privateKey: String): String? {
        return try {
            if (privateKey.length in 51..52 && (privateKey.startsWith("5") || privateKey.startsWith("K") || privateKey.startsWith("L"))) {
                DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKey)
                privateKey
            } else {
                val privateKeyBytes = Numeric.hexStringToByteArray(privateKey)
                if (privateKeyBytes.size != 32) return null
                val ecKey: ECKey = ECKey.fromPrivate(privateKeyBytes, true)
                ecKey.getPrivateKeyEncoded(BitcoinNetwork.MAINNET).toBase58()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun createClassicWallet() {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            _errorMessage.value = "Spend key is null"
            _state.value = FetchWalletState.IMPORT_ERROR
            return
        }
        viewModelScope.launch {
            try {
                _importedWalletDestination.value = null
                _state.value = FetchWalletState.IMPORTING
                val hasClassicWallet = web3Repository.getClassicWalletId() != null
                val classicIndex = classicWalletIndexForCreation(
                    hasClassicWallet = hasClassicWallet,
                    maxClassicIndex = web3Repository.getClassicWalletMaxIndex(),
                )
                val walletRequest = buildClassicWalletRequest(web3Repository, currentSpendKey, classicIndex)
                saveImportedWallet(walletRequest, null)
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallet")
                _state.value = FetchWalletState.IMPORT_ERROR
            }
        }
    }

    private suspend fun selectImportedWalletIfNeeded(walletId: String, category: String) {
        if (_importedWalletDestination.value != null) return
        _importedWalletDestination.value = walletDestinationForWallet(walletId, category)
        Web3Signer.setWallet(walletId, category) { queryWalletId ->
            runBlocking { web3Repository.getAddresses(queryWalletId) }
        }
    }

}

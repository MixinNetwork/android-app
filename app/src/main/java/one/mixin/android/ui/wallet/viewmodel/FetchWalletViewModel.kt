package one.mixin.android.ui.wallet.viewmodel

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.Constants.Tip.ENCRYPTED_WEB3_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.response.AssetView
import one.mixin.android.api.response.web3.Web3WalletResponse
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.toEntropy
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.event.AddWalletSuccessEvent
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.putString
import one.mixin.android.job.RefreshWeb3Job
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.Tip
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.IndexedWallet
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@HiltViewModel
class FetchWalletViewModel @Inject constructor(
    private val tip: Tip,
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

    fun findMoreWallets() {
        currentIndex += 10
        startFetching(currentIndex)
    }

    private fun startFetching(offset: Int) {
        viewModelScope.launch {
            _state.value = FetchWalletState.FETCHING
            try {
                if (mnemonic.isNotBlank()) {
                    val wallets = (offset until offset + 10).map { index ->
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
                                    assets = allTokens
                                )
                            }.filter { it.assets.isNotEmpty() }
                            if (offset == 0 && walletInfos.isEmpty()) {
                                _wallets.value = listOf(wallets[0])
                            } else {
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
                    val name = "${MixinApplication.appContext.getString(R.string.Common_Wallet)} ${it.index + 1}"
                    val category = RefreshWeb3Job.WALLET_CATEGORY_PRIVATE
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
                    Triple(WalletRequest(name, category, addresses), it.assets, it.solanaWallet.mnemonic.split(" "))
                }
                saveWallets(walletsToCreate)
                Timber.d("Successfully imported ${selectedWalletInfos.value.size} wallets")
                RxBus.publish(AddWalletSuccessEvent())
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallets")
            }
        }
    }

    private suspend fun saveWallets(walletsToCreate: List<Triple<WalletRequest, List<AssetView>, List<String>>>) {
        val currentSpendKey = spendKey
        if (currentSpendKey == null) {
            Timber.e("Spend key is null, cannot save wallets.")
            return
        }

        walletsToCreate.forEach { (walletRequest, assets, words) ->
            requestRouteAPI(
                invokeNetwork = { web3Repository.createWallet(walletRequest) },
                successBlock = { response ->
                    response.data?.let { wallet ->
                        insertWalletAndAddresses(wallet)
                        web3Repository.insertWeb3Tokens(assets.map { it.toWebToken(wallet.id) })
                        saveWeb3PrivateKey(MixinApplication.appContext, currentSpendKey, wallet.id, words)
                    } ?: Timber.e("Failed to create wallet: response data is null")
                },
                failureBlock = { response ->
                    Timber.e("Failed to create wallet: ${response.errorCode} - ${response.errorDescription}")
                    false
                },
                requestSession = { userRepository.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
                defaultErrorHandle = {}
            )
        }
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

    private suspend fun saveWeb3PrivateKey(context: Context, spendKey: ByteArray, walletId: String, words: List<String>): Boolean {
        return try {
            val entropy = runCatching { toEntropy(words) }.getOrNull() ?: return false
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(spendKey)
            val encryptionKeyBytes = masterKeyPair.privateKey.toByteArray()
            val sha = MessageDigest.getInstance("SHA-256")
            val hashedKey = sha.digest(encryptionKeyBytes)
            val secretKey = SecretKeySpec(hashedKey, "AES")

            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedPrivateKey = cipher.doFinal(entropy)

            val encryptedData = iv + encryptedPrivateKey
            val encryptedString = Base64.encodeToString(encryptedData, Base64.NO_WRAP)

            val encryptedPrefs = runCatching {
                EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_WEB3_KEY,
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.onFailure {
                context.deleteSharedPreferences(ENCRYPTED_WEB3_KEY)
            }.getOrNull()

            encryptedPrefs?.putString(walletId, encryptedString)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save web3 private key")
            false
        }
    }

}

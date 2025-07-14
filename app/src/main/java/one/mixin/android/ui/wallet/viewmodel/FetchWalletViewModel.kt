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
    private var pin: String = ""

    init {
        startFetching()
    }

    fun setMnemonic(mnemonic: String) {
        this.mnemonic = mnemonic
        startFetching()
    }

    fun setPin(pin: String) {
        this.pin = pin
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
            try {
                // Create wallets for each selected wallet
                selectedWalletInfos.forEach { wallet ->
                    createWallet(pin,wallet)
                }
                Timber.d("Successfully imported ${selectedWalletInfos.size} wallets")
                RxBus.publish(AddWalletSuccessEvent())
            } catch (e: Exception) {
                Timber.e(e, "Failed to import wallets")
            }
        }
    }

    fun retry() {
        startFetching()
    }

    private suspend fun createWallet(pin:String, indexedWallet: IndexedWallet) {
        createWallet(
            pin,
            indexedWallet,
            name = "${MixinApplication.appContext.getString(R.string.Common_Wallet)} ${indexedWallet.index + 1}",
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
            ),
            indexedWallet.assets
        )
    }

    private suspend fun createWallet(
        pin: String,
        indexedWallet: IndexedWallet,
        name: String,
        category: String,
        addresses: List<Web3AddressRequest>,
        assets: List<AssetView>
    ) {
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
                    insertWalletAndAddresses(wallet)
                    web3Repository.insertWeb3Tokens(assets.map { it.toWebToken(wallet.id) })
                    saveWeb3PrivateKey(
                        MixinApplication.appContext,
                        pin,
                        indexedWallet.ethereumWallet.address,
                        indexedWallet.ethereumWallet.privateKey
                    )
                    saveWeb3PrivateKey(
                        MixinApplication.appContext,
                        pin,
                        indexedWallet.solanaWallet.address,
                        indexedWallet.solanaWallet.privateKey
                    )
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

    suspend fun saveWeb3PrivateKey(context: Context, pin: String, address: String, privateKey: String): Boolean {
        return try {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val tipPriv = result.getOrThrow()
            val spendKey = tip.getSpendPrivFromEncryptedSalt(
                tip.getMnemonicFromEncryptedPreferences(context),
                tip.getEncryptedSalt(context),
                pin,
                tipPriv
            )
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
            val encryptedPrivateKey = cipher.doFinal(Numeric.hexStringToByteArray(privateKey))

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

            encryptedPrefs?.putString(address, encryptedString)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save web3 private key")
            false
        }
    }

}

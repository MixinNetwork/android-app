package one.mixin.android.ui.home.web3

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.getChainFromName
import one.mixin.android.db.web3.vo.isTransferFeeFree
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.maxUtxoCount
import one.mixin.android.ui.home.inscription.component.OwnerState
import one.mixin.android.ui.oldwallet.AssetRepository
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mlkit.firstUrl
import one.mixin.android.vo.Account
import one.mixin.android.vo.ConnectionUI
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.User
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toMixAddress
import one.mixin.android.web3.ChainType
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSignMessage
import org.sol4k.Convert.lamportToSol
import org.sol4k.PublicKey
import org.sol4kt.VersionedTransactionCompat
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class Web3ViewModel
@Inject
internal constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val assetRepository: AssetRepository,
    private val tokenRepository: TokenRepository,
    private val jobManager: MixinJobManager,
    private val web3Repository: Web3Repository,
    private val rpc: Rpc,
) : ViewModel() {
    var scrollOffset: Int = 0

    suspend fun findMarketItemByAssetId(assetId: String) = tokenRepository.findMarketItemByAssetId(assetId)

    fun web3TokensExcludeHidden(walletId: String) = web3Repository.web3TokensExcludeHidden(walletId)

    suspend fun web3TokensExcludeHiddenRaw(walletId: String) = withContext(Dispatchers.IO) {
        return@withContext web3Repository.web3TokensExcludeHiddenRaw(walletId)
    }

    fun hiddenAssetItems(walletId: String) = web3Repository.hiddenAssetItems(walletId)

    suspend fun updateTokenHidden(tokenId: String, walletId: String, hidden: Boolean) =
        web3Repository.updateTokenHidden(tokenId, walletId, hidden)

    suspend fun web3TokenItemById(walletId: String, assetId: String) = withContext(Dispatchers.IO) {
        web3Repository.web3TokenItemById(walletId, assetId)
    }

    fun getTokenPriceUsdFlow(assetId: String): Flow<String?> = flow {
        val item = tokenRepository.findAssetItemById(assetId)?.priceUsd
        emit(item)
    }.catch { e ->
        emit(null)
    }.flowOn(Dispatchers.IO)

    fun web3Transactions(walletId: String, assetId: String) = web3Repository.web3Transactions(walletId, assetId)

    fun web3TokenExtraFlow(walletId: String, assetId: String) =
        tokenRepository.web3TokenExtraFlow(walletId, assetId)

    suspend fun findOrSyncAsset(
        assetId: String,
    ): TokenItem? {
        return withContext(Dispatchers.IO) {
            tokenRepository.findOrSyncAsset(assetId)
        }
    }

    fun disconnect(
        version: WalletConnect.Version,
        topic: String,
    ) {
        when (version) {
            WalletConnect.Version.V2 -> {
                WalletConnectV2.disconnect(topic)
            }

            WalletConnect.Version.TIP -> {}
        }
    }

    fun getLatestActiveSignSessions(): List<ConnectionUI> {
        val v2List =
            WalletConnectV2.getListOfActiveSessions().mapIndexed { index, wcSession ->
                ConnectionUI(
                    index = index,
                    icon = wcSession.metaData?.icons?.firstOrNull(),
                    name = wcSession.metaData!!.name.takeIf { it.isNotBlank() } ?: "Dapp",
                    uri = wcSession.metaData!!.url.takeIf { it.isNotBlank() } ?: "Not provided",
                    data = wcSession.topic,
                )
            }
        return v2List
    }

    fun dapps(chainId: String): List<Dapp> {
        val gson = GsonHelper.customGson
        val dapps = MixinApplication.get().defaultSharedPreferences.getString("dapp_$chainId", null)
        return if (dapps == null) {
            emptyList()
        } else {
            gson.fromJson(dapps, Array<Dapp>::class.java).toList()
        }
    }

    suspend inline fun fuzzySearchUrl(query: String?): String? {
        return if (query.isNullOrEmpty()) {
            null
        } else {
            firstUrl(query)
        }
    }

    private fun updateTokens(chain: String, tokens: List<Web3Token>) {
        val tokenMap = if (chain == ChainType.ethereum.name) evmTokenMap else solanaTokenMap
        val newTokenIds = tokens.map { "${it.chainId}${it.assetKey}" }.toSet()

        val missingTokenIds = tokenMap.keys - newTokenIds
        missingTokenIds.forEach { tokenId ->
            val token = tokenMap[tokenId]
            if (token != null) {
                tokenMap[tokenId] = token.copy(balance = "0")
            }
        }

        tokens.forEach { token ->
            val tokenId = "${token.chainId}${token.assetKey}"
            tokenMap[tokenId] = token
        }
    }

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

    suspend fun findBotPublicKey(
        conversationId: String,
        botId: String,
    ) = userRepository.findBotPublicKey(conversationId, botId)

    suspend fun findAddres(token: Web3Token): String? {
        return tokenRepository.findDepositEntry(token.chainId)?.destination
    }

    suspend fun findAndSyncDepositEntry(token: Web3TokenItem) =
        withContext(Dispatchers.IO) {
            tokenRepository.findAndCheckDepositEntry(token.chainId, token.assetId).first
        }

    suspend fun web3TokenItems(chainIds: List<String>) = tokenRepository.web3TokenItems(chainIds)

    suspend fun getFees(
        id: String,
        destination: String,
    ) = tokenRepository.getFees(id, destination)

    suspend fun findTokenItems(ids: List<String>): List<TokenItem> =
        tokenRepository.findTokenItems(ids)

    suspend fun findWeb3TokenItems(walletId: String): List<Web3TokenItem> =
        tokenRepository.findWeb3TokenItems(walletId)

    suspend fun findTokensExtra(assetId: String) =
        withContext(Dispatchers.IO) {
            tokenRepository.findTokensExtra(assetId)
        }

    suspend fun syncAsset(assetId: String) =
        withContext(Dispatchers.IO) {
            tokenRepository.syncAsset(assetId)
        }

    suspend fun findAssetItemById(assetId: String) = tokenRepository.findAssetItemById(assetId)

    fun collectibles(sortOrder: SortOrder): LiveData<List<SafeCollectible>> =
        tokenRepository.collectibles(sortOrder)

    fun collectiblesByHash(collectionHash: String): LiveData<List<SafeCollectible>> =
        tokenRepository.collectiblesByHash(collectionHash)

    fun collections(sortOrder: SortOrder): LiveData<List<SafeCollection>> =
        tokenRepository.collections(sortOrder)

    fun collectionByHash(hash: String): LiveData<SafeCollection?> =
        tokenRepository.collectionByHash(hash)

    fun inscriptionByHash(hash: String) = tokenRepository.inscriptionByHash(hash)

    suspend fun buildNftTransaction(
        inscriptionHash: String,
        receiver: User,
        release: Boolean = false,
    ): NftBiometricItem? =
        withContext(Dispatchers.IO) {
            val output =
                tokenRepository.findUnspentOutputByHash(inscriptionHash) ?: return@withContext null
            val inscriptionItem =
                tokenRepository.findInscriptionByHash(inscriptionHash) ?: return@withContext null
            val inscriptionCollection =
                tokenRepository.findInscriptionCollectionByHash(inscriptionHash)
                    ?: return@withContext null
            val asset =
                tokenRepository.findTokenItemByAsset(output.asset) ?: return@withContext null
            val releaseAmount =
                if (release) {
                    BigDecimal(output.amount).divide(BigDecimal(2), 8, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString()
                } else {
                    null
                }
            return@withContext NftBiometricItem(
                asset = asset,
                traceId = UUID.randomUUID().toString(),
                amount = output.amount,
                memo = null,
                state = PaymentStatus.pending.name,
                receivers = listOf(receiver),
                reference = null,
                inscriptionItem = inscriptionItem,
                inscriptionCollection = inscriptionCollection,
                releaseAmount = releaseAmount,
            )
        }

    fun inscriptionStateByHash(inscriptionHash: String) =
        tokenRepository.inscriptionStateByHash(inscriptionHash)

    private suspend fun getSolMinimumBalanceForRentExemption(address: PublicKey): BigDecimal =
        withContext(Dispatchers.IO) {
            try {
                val accountInfo = rpc.getAccountInfo(address) ?: return@withContext BigDecimal.ZERO
                val mb = rpc.getMinimumBalanceForRentExemption(accountInfo.space) ?: return@withContext BigDecimal.ZERO
                return@withContext lamportToSol(BigDecimal(mb))
            } catch (e: Exception) {
                return@withContext BigDecimal.ZERO
            }
        }

    suspend fun calcFee(
        token: Web3TokenItem,
        transaction: JsSignMessage,
        fromAddress: String,
    ): BigDecimal? {
        val chain = token.getChainFromName()
        if (chain == Chain.Solana) {
            val tx = VersionedTransactionCompat.from(transaction.data ?: "")
            val fee = tx.calcFee(fromAddress)
            return fee
        } else {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    web3Repository.estimateFee(
                        EstimateFeeRequest(
                            token.chainId,
                            null,
                            transaction.data ?: transaction.wcEthereumTransaction?.data,
                            fromAddress,
                            transaction.wcEthereumTransaction?.to,
                            transaction.wcEthereumTransaction?.value,
                        )
                    )
                }.getOrNull()
            }
            if (r?.isSuccess != true) return BigDecimal.ZERO
            return withContext(Dispatchers.IO) {
                val tipGas = buildTipGas(chain.chainId, r.data!!)
                tipGas.displayValue(transaction.wcEthereumTransaction?.maxFeePerGas) ?: BigDecimal.ZERO
            }
        }
    }

    suspend fun getWeb3Tx(txhash: String) = assetRepository.getWeb3Tx(txhash)

    suspend fun isBlockhashValid(blockhash: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext rpc.isBlockhashValid(blockhash) ?: false
        }

    suspend fun getBotPublicKey(botId: String, force: Boolean) =
        userRepository.getBotPublicKey(botId, force)

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.upsert(user)
        }

    suspend fun getOwner(hash: String): OwnerState {
        try {
            val item = withContext(Dispatchers.IO) { tokenRepository.getInscriptionItem(hash) }
                ?: return OwnerState()
            if (item.owner != null) {
                val mixinAddress = item.owner.toMixAddress() ?: return OwnerState()
                return if (mixinAddress.uuidMembers.isNotEmpty()) {
                    val users = userRepository.findOrRefreshUsers(mixinAddress.uuidMembers)
                    val title = if (mixinAddress.uuidMembers.size > 1) {
                        "(${mixinAddress.threshold}/${mixinAddress.uuidMembers.size})"
                    } else {
                        null
                    }
                    OwnerState(title = title, users = users)
                } else {
                    OwnerState(owner = item.owner)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return OwnerState()
    }

    suspend fun getStakeAccounts(account: String): List<StakeAccount>? {
        return handleMixinResponse(
            invokeNetwork = { assetRepository.getStakeAccounts(account) },
            successBlock = {
                it.data
            }
        )
    }

    suspend fun checkUtxoSufficiency(
        assetId: String,
        amount: String,
    ): String? {
        val desiredAmount = BigDecimal(amount)
        val candidateOutputs = tokenRepository.findOutputs(maxUtxoCount, assetIdToAsset(assetId))
        if (candidateOutputs.isEmpty()) {
            return null
        }

        val selectedOutputs = mutableListOf<Output>()
        var totalSelectedAmount = BigDecimal.ZERO
        candidateOutputs.forEach { output ->
            val outputAmount = BigDecimal(output.amount)
            selectedOutputs.add(output)
            totalSelectedAmount += outputAmount
            if (totalSelectedAmount >= desiredAmount) {
                return null
            }
        }

        if (selectedOutputs.size >= maxUtxoCount) {
            return totalSelectedAmount.toPlainString()
        }

        if (totalSelectedAmount < desiredAmount) {
            // Refresh when balance is insufficient
            jobManager.addJobInBackground(SyncOutputJob())
            return null
        }

        throw Exception("Impossible")
    }


    suspend fun firstUnspentTransaction() =
        withContext(Dispatchers.IO) {
            tokenRepository.firstUnspentTransaction()
        }

    suspend fun findLatestTrace(
        opponentId: String?,
        destination: String?,
        tag: String?,
        amount: String,
        assetId: String,
    ) = withContext(Dispatchers.IO) {
        tokenRepository.findLatestTrace(opponentId, destination, tag, amount, assetId)
    }

    suspend fun getAddressesByChainId(walletId: String, chainId: String): Web3Address? {
        return web3Repository.getAddressesByChainId(walletId, chainId)
    }

    suspend fun getClassicWalletId(): String? = web3Repository.getClassicWalletId()

    suspend fun getTransactionsById(traceId: String) = tokenRepository.getTransactionsById(traceId)

    suspend fun findTokensByIds(walletId: String, assetIds: List<String>): List<Web3TokenItem> = withContext(Dispatchers.IO) {
        return@withContext web3Repository.findWeb3TokenItemsByIds(walletId, assetIds)
    }

    suspend fun getRawTransactionByHashAndChain(hash: String, chainId: String) = tokenRepository.getRawTransactionByHashAndChain(hash, chainId)

    suspend fun getWalletName(walletId: String): String? = web3Repository.findWalletById(walletId)?.name

    suspend fun findWalletById(walletId: String) = web3Repository.findWalletById(walletId)

    suspend fun getAddresses(walletId: String) = web3Repository.getAddresses(walletId)

    suspend fun getAddressesGroupedByDestination(walletId: String) = web3Repository.getAddressesGroupedByDestination(walletId)

    companion object {
        private val evmTokenMap = mutableMapOf<String, Web3Token>()
        private val solanaTokenMap = mutableMapOf<String, Web3Token>()
    }

    fun marketById(assetId: String) = tokenRepository.marketById(assetId)

    suspend fun getPendingRawTransactions(walletId: String) = tokenRepository.getPendingRawTransactions(walletId)

    suspend fun getPendingTransactions(walletId: String) = tokenRepository.getPendingTransactions(walletId)

    suspend fun getPendingRawTransactions(walletId: String, chainId: String) = tokenRepository.getPendingRawTransactions(walletId, chainId)

    fun getPendingTransactionCount(walletId: String): LiveData<Int> = tokenRepository.getPendingTransactionCount(walletId)

    suspend fun transaction(hash: String, chainId: String) = tokenRepository.transaction(hash, chainId)

    suspend fun updateTransaction(hash: String, status: String, chainId: String) =
        withContext(Dispatchers.IO) { tokenRepository.updateTransaction(hash, status, chainId) }

    suspend fun insertRawTransaction(raw: Web3RawTransaction) =
        withContext(Dispatchers.IO) { tokenRepository.insertWeb3RawTransaction(raw) }

    suspend fun anyAddressExists(destinations: List<String>) = web3Repository.anyAddressExists(destinations)
    suspend fun isAddressMatch(walletId: String?, address: String): Boolean {
        if (walletId == null) return false
        return withContext(Dispatchers.IO) {
            web3Repository.isAddressMatch(walletId, address)
        }
    }

    // index 0 is address,index 1 is privacy wallet, 2 is common wallet, 3 is fee free wallet
    suspend fun checkAddressAndGetDisplayName(destination: String, tag: String?, chainId: String): Pair<String, Int>? {
        return withContext(Dispatchers.IO) {

            if (tag.isNullOrBlank()) {
                val existsInAddresses = tokenRepository.findDepositEntry(chainId)?.destination == destination
                if (existsInAddresses) return@withContext Pair(MixinApplication.appContext.getString(R.string.Privacy_Wallet), 1)
            }

            val wallet = web3Repository.getWalletByDestination(destination)
            if (wallet != null) {
                return@withContext Pair(wallet.name, if (wallet.isTransferFeeFree()) 3 else 2)
            }

            tokenRepository.findAddressByDestination(destination, tag ?: "", chainId)?.let { label ->
                return@withContext Pair(label, 0)
            }
            return@withContext null
        }
    }
}
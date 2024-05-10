package one.mixin.android.ui.home.web3

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.getChainIdFromName
import one.mixin.android.api.service.Web3Service
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat2
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mlkit.firstUrl
import one.mixin.android.vo.ConnectionUI
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.SafeInscription
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class Web3ViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val web3Service: Web3Service,
) : ViewModel() {
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
        if (dapps == null) {
            return emptyList<Dapp>()
        } else {
            return gson.fromJson(dapps, Array<Dapp>::class.java).toList()
        }
    }

    suspend inline fun fuzzySearchUrl(query: String?): String? {
        return if (query.isNullOrEmpty()) {
            null
        } else {
            firstUrl(query)
        }
    }

    suspend fun web3Account(address: String) = web3Service.web3Account(address)

    suspend fun web3Transaction(address: String, chainId: String, fungibleId: String) = web3Service.transactions(address, chainId, fungibleId)

    suspend fun saveSession(participantSession: ParticipantSession) {
        userRepository.saveSession(participantSession)
    }

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

    suspend fun findBotPublicKey(
        conversationId: String,
        botId: String,
    ) = userRepository.findBotPublicKey(conversationId, botId)

    suspend fun findAddres(token: Web3Token): String? {
        return tokenRepository.findDepositEntry(token.getChainIdFromName())?.destination
    }

    suspend fun findAndSyncDepositEntry(token: Web3Token): String? =
        withContext(Dispatchers.IO) {
            tokenRepository.findAndSyncDepositEntry(token.getChainIdFromName()).first?.destination
        }

    suspend fun web3TokenItems() = tokenRepository.web3TokenItems()

    suspend fun getFees(
        id: String,
        destination: String,
    ) = tokenRepository.getFees(id, destination)

    suspend fun findTokensExtra(assetId: String) = withContext(Dispatchers.IO) {
        tokenRepository.findTokensExtra(assetId)
    }

    suspend fun syncAsset(assetId: String) = withContext(Dispatchers.IO) {
        tokenRepository.syncAsset(assetId)
    }

    fun inscriptions(): LiveData<List<SafeInscription>> = tokenRepository.inscriptions()

    fun inscriptionByHash(hash: String) = tokenRepository.inscriptionByHash(hash)

    suspend fun buildNftTransaction(inscriptionHash: String, receiver: User): NftBiometricItem? = withContext(Dispatchers.IO) {
        val output = tokenRepository.findUnspentOutputByHash(inscriptionHash) ?: return@withContext null
        val inscriptionItem = tokenRepository.findInscriptionByHash(inscriptionHash) ?: return@withContext null
        val inscriptionCollection = tokenRepository.findInscriptionCollectionByHash(inscriptionHash) ?: return@withContext null
        val asset = tokenRepository.findTokenItemByAsset(output.asset) ?: return@withContext null
        return@withContext NftBiometricItem(
            asset = asset,
            traceId = UUID.randomUUID().toString(),
            amount = output.amount,
            memo = null,
            state = PaymentStatus.pending.name,
            receivers = listOf(receiver),
            reference = null,
            inscriptionItem = inscriptionItem,
            inscriptionCollection = inscriptionCollection
        )
    }

    fun inscriptionStateByHash(inscriptionHash: String) = tokenRepository.inscriptionStateByHash(inscriptionHash)

}

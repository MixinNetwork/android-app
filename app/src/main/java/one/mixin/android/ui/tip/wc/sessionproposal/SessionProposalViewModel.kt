package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.isSigningEligible
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WalletConnectAddresses
import one.mixin.android.tip.wc.internal.getSupportedNamespaces
import one.mixin.android.vo.safe.UnifiedAssetItem
import one.mixin.android.web3.js.Web3Signer
import java.math.BigDecimal
import javax.inject.Inject

data class WalletConnectSelectableWallet(
    val wallet: WalletItem,
    val balance: BigDecimal,
    val addresses: WalletConnectAddresses,
    val isConnected: Boolean,
)

@HiltViewModel
class SessionProposalViewModel
    @Inject
    internal constructor(
        val web3Repository: Web3Repository,
        val tokenRepository: TokenRepository,
    ) : ViewModel() {
        private var account: String = ""
            get() {
                return Web3Signer.address
            }

        fun rejectSession(
            version: WalletConnect.Version,
            topic: String,
        ) {
            when (version) {
                WalletConnect.Version.V2 -> {
                    WalletConnectV2.rejectSession(topic)
                }
                WalletConnect.Version.TIP -> {}
            }
        }

        fun getSessionProposalUI(
            version: WalletConnect.Version,
            chain: Chain,
            sessionProposal: Wallet.Model.SessionProposal?,
        ): SessionProposalUI? {
            when (version) {
                WalletConnect.Version.V2 -> {
                    if (sessionProposal == null) return null
                    return SessionProposalUI(
                        peer =
                            PeerUI(
                                icon = sessionProposal.icons.firstOrNull().toString(),
                                name = sessionProposal.name,
                                desc = sessionProposal.description,
                                uri = sessionProposal.url.toUri().host ?: "",
                                account = account,
                            ),
                        chain = chain,
                    )
                }
                WalletConnect.Version.TIP -> {
                    return WalletConnectTIP.getSessionProposalUI()
                }
            }
        }

        suspend fun findWalletById(walletId: String) = withContext(Dispatchers.IO) {
            web3Repository.findWalletById(walletId)
        }

        suspend fun loadWalletDisplayData(walletId: String): WalletConnectSelectableWallet? =
            withContext(Dispatchers.IO) {
                if (walletId.isBlank()) return@withContext null
                val wallet = web3Repository.findWalletById(walletId) ?: return@withContext null
                val addresses = web3Repository.getAddresses(walletId).toWalletConnectAddresses()
                val balance = calculateWalletUsdBalance(walletId)
                val connectedWalletIds = resolveConnectedWalletIds()
                WalletConnectSelectableWallet(
                    wallet = wallet,
                    balance = balance,
                    addresses = addresses,
                    isConnected = wallet.id in connectedWalletIds,
                )
            }

        suspend fun loadSelectableWallets(
            topic: String?,
            query: String = "",
        ): List<WalletConnectSelectableWallet> =
            withContext(Dispatchers.IO) {
                val sessionProposal = topic?.takeIf { it.isNotBlank() }?.let { WalletConnectV2.getSessionProposal(it) }
                val connectedWalletIds = resolveConnectedWalletIds()
                val trimmedQuery = query.trim()
                web3Repository.getAllWallets()
                    .filter { it.isSigningEligible() }
                    .mapNotNull { wallet ->
                        val rawAddresses = web3Repository.getAddresses(wallet.id)
                        val wcAddresses = rawAddresses.toWalletConnectAddresses()
                        if (!isWalletCompatibleWithProposal(wcAddresses, sessionProposal)) {
                            return@mapNotNull null
                        }
                        if (trimmedQuery.isNotEmpty()) {
                            val matchesName = wallet.name.contains(trimmedQuery, ignoreCase = true)
                            val matchesAddress = rawAddresses.any { it.destination.contains(trimmedQuery, ignoreCase = true) }
                            if (!matchesName && !matchesAddress) {
                                return@mapNotNull null
                            }
                        }
                        WalletConnectSelectableWallet(
                            wallet = wallet,
                            balance = calculateWalletUsdBalance(wallet.id),
                            addresses = wcAddresses,
                            isConnected = wallet.id in connectedWalletIds,
                        )
                    }
            }

        private suspend fun calculateWalletUsdBalance(walletId: String): BigDecimal {
            val tokens = web3Repository.findUnifiedAssetItem(walletId)
            return tokens.sumOf { calculateAssetValue(it) }
        }

        private fun calculateAssetValue(item: UnifiedAssetItem): BigDecimal {
            return try {
                BigDecimal(item.balance).multiply(BigDecimal(item.priceUsd))
            } catch (_: Exception) {
                BigDecimal.ZERO
            }
        }

        private fun resolveConnectedWalletIds(): Set<String> =
            resolveConnectedWalletIds(
                activeSessionWalletIds =
                    WalletConnectV2.getListOfActiveSessions().mapNotNull { session ->
                        WalletConnectV2.getSessionWalletId(session.topic)
                    },
                currentWalletId = Web3Signer.currentWalletId,
            )

        private fun isWalletCompatibleWithProposal(
            addresses: WalletConnectAddresses,
            sessionProposal: Wallet.Model.SessionProposal?,
        ): Boolean {
            if (sessionProposal == null) {
                return addresses.evm.isNotBlank() || addresses.solana.isNotBlank() || addresses.bitcoin.isNotBlank()
            }
            val supportedWalletChains =
                getSupportedNamespaces(addresses)
                    .values
                    .flatMap { it.chains ?: emptyList() }
                    .toSet()
            if (supportedWalletChains.isEmpty()) return false
            val requiredNamespaces = sessionProposal.requiredNamespaces.values.filter { !it.chains.isNullOrEmpty() }
            if (requiredNamespaces.any { ns -> ns.chains!!.none { it in supportedWalletChains } }) {
                return false
            }
            val allNamespaces =
                (sessionProposal.requiredNamespaces.values + sessionProposal.optionalNamespaces.values)
                    .filter { !it.chains.isNullOrEmpty() }
            return allNamespaces.isEmpty() || allNamespaces.any { ns ->
                ns.chains!!.any { it in supportedWalletChains }
            }
        }

        suspend fun checkAddressAndGetDisplayName(destination: String, chainId: String?): Pair<String, Boolean>? {
            return withContext(Dispatchers.IO) {

                if (chainId != null) {
                    val existsInAddresses = tokenRepository.findDepositEntry(chainId)?.destination == destination
                    if (existsInAddresses) return@withContext Pair(MixinApplication.appContext.getString(R.string.Privacy_Wallet), false)
                }

                val wallet = web3Repository.getWalletByDestination(destination)
                if (wallet != null) {
                    return@withContext Pair(wallet.name, true)
                }
                return@withContext null
            }
        }
}

internal fun resolveConnectedWalletIds(
    activeSessionWalletIds: Collection<String>,
    currentWalletId: String,
): Set<String> =
    buildSet {
        activeSessionWalletIds.filterTo(this) { it.isNotBlank() }
        if (currentWalletId.isNotBlank()) {
            add(currentWalletId)
        }
    }

internal fun List<Web3Address>.toWalletConnectAddresses(): WalletConnectAddresses =
    WalletConnectAddresses(
        evm = firstOrNull { it.chainId in Constants.Web3ChainIds }?.destination.orEmpty(),
        solana = firstOrNull { it.chainId == Constants.ChainId.SOLANA_CHAIN_ID }?.destination.orEmpty(),
        bitcoin = firstOrNull { it.chainId == Constants.ChainId.BITCOIN_CHAIN_ID }?.destination.orEmpty(),
    )

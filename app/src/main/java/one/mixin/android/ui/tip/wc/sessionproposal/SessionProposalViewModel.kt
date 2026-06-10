package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.web3.js.Web3Signer
import javax.inject.Inject

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

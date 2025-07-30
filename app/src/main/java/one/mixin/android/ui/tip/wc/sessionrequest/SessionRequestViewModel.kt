package one.mixin.android.ui.tip.wc.sessionrequest

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSigner
import org.web3j.utils.Numeric
import javax.inject.Inject

@HiltViewModel
class SessionRequestViewModel
    @Inject
    internal constructor(
        val web3Repository: Web3Repository,
        val tokenRepository: TokenRepository
    ) : ViewModel() {
        private var account: String = ""
            get() {
                return JsSigner.address
            }


        fun rejectRequest(
            version: WalletConnect.Version,
            topic: String,
            msg: String? = null,
        ) {
            when (version) {
                WalletConnect.Version.V2 -> {
                    WalletConnectV2.rejectRequest(msg, topic)
                }
                WalletConnect.Version.TIP -> {}
            }
        }

        fun getSessionRequestUI(
            version: WalletConnect.Version,
            chain: Chain,
            signData: WalletConnect.WCSignData.V2SignData<*>?,
            sessionRequest: Wallet.Model.SessionRequest?,
        ): SessionRequestUI<*>? {
            when (version) {
                WalletConnect.Version.V2 -> {
                    if (signData == null || sessionRequest == null) return null
                    val peer = sessionRequest.peerMetaData ?: return null
                    val peerUI =
                        PeerUI(
                            name = peer.name,
                            icon = peer.icons.firstOrNull() ?: "",
                            uri = peer.url.toUri().host ?: "",
                            desc = peer.description,
                            account = account,
                        )
                    return SessionRequestUI(
                        peerUI = peerUI,
                        requestId = signData.requestId,
                        data = signData.signMessage,
                        chain = chain,
                    )
                }
                WalletConnect.Version.TIP -> {
                    return WalletConnectTIP.getSessionRequestUI()
                }
            }
        }

        fun <T> getContent(
            version: WalletConnect.Version,
            gson: Gson,
            data: T,
        ): String =
            when (version) {
                WalletConnect.Version.V2 -> {
                    if (data is WCEthereumSignMessage && (data.type == WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE || data.type == WCEthereumSignMessage.WCSignType.TYPED_MESSAGE)) {
                        if (data.type == WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE) {
                            String(Numeric.cleanHexPrefix(data.data).hexStringToByteArray())
                        } else {
                            gson.toJson(data.data)
                        }
                    } else {
                        gson.toJson(data)
                    }
                }
                WalletConnect.Version.TIP -> {
                    data as String
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
                    if (wallet.category == WalletCategory.CLASSIC.value) {
                        return@withContext Pair(MixinApplication.appContext.getString(R.string.Common_Wallet), false)
                    }
                    return@withContext Pair(wallet.name, false)
                }
                if (chainId != null) {
                    val address = tokenRepository.matchAddress(destination, chainId)
                    if (address != null) {
                        return@withContext Pair(address.label, true)
                    }
                }
                return@withContext null
            }
        }
}

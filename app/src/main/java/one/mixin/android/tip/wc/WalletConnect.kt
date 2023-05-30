package one.mixin.android.tip.wc

import android.content.Context
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.walletconnect.web3.wallet.client.Wallet
import one.mixin.android.Constants
import one.mixin.android.api.response.GasPriceType
import one.mixin.android.api.response.TipGas
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.session.Session
import org.kethereum.rpc.EthereumRPCException
import org.kethereum.rpc.HttpEthereumRPC
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.utils.Numeric
import timber.log.Timber

abstract class WalletConnect {
    companion object {
        const val TAG = "WalletConnect"

        internal const val defaultGasLimit = "250000"

        fun isEnabled(context: Context): Boolean = Session.getAccount()?.hasPin == true && !Session.getTipPub().isNullOrBlank() &&
            (context.defaultSharedPreferences.getBoolean(Constants.Debug.WALLET_CONNECT_DEBUG, false) || Session.isTipFeatureEnabled())
    }

    enum class Version {
        V1, V2, TIP
    }

    enum class RequestType {
        SessionProposal, SessionRequest, SwitchNetwork,
    }

    sealed class WCSignData<T>(
        open val requestId: Long,
        open val signMessage: T,
    ) {
        data class V1SignData<T>(
            override val requestId: Long,
            override val signMessage: T,
            var tipGas: TipGas? = null,
            var gasPriceType: GasPriceType = GasPriceType.Propose,
        ) : WCSignData<T>(requestId, signMessage)

        data class V2SignData<T>(
            override val requestId: Long,
            override val signMessage: T,
            val sessionRequest: Wallet.Model.SessionRequest,
            var tipGas: TipGas? = null,
            var gasPriceType: GasPriceType = GasPriceType.Propose,
        ) : WCSignData<T>(requestId, signMessage)

        data class TIPSignData(
            override val signMessage: String,
        ) : WCSignData<String>(0L, signMessage)
    }

    var chain: Chain = Chain.Polygon
        protected set
    protected var rpc = HttpEthereumRPC(baseURL = chain.rpcServers[0])

    open var currentSignData: WCSignData<*>? = null

    fun signMessage(priv: ByteArray, message: WCEthereumSignMessage): String {
        val keyPair = ECKeyPair.create(priv)
        val signature = if (message.type == WCEthereumSignMessage.WCSignType.TYPED_MESSAGE) {
            val encoder = StructuredDataEncoder(message.data)
            Sign.signMessage(encoder.hashStructuredData(), keyPair, false)
        } else {
            Sign.signPrefixedMessage(Numeric.hexStringToByteArray(message.data), keyPair)
        }
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        return Numeric.toHexString(b)
    }

    protected fun throwError(error: EthereumRPCException, msgAction: ((String) -> Unit)? = null) {
        val msg = "error code: ${error.code}, message: ${error.message}"
        Timber.d("$TAG error $msg")
        msgAction?.invoke(msg)
        throw WalletConnectException(error.code, error.message)
    }
}

package one.mixin.android.tip.wc

import android.os.Build
import android.util.LruCache
import com.walletconnect.web3.wallet.client.Wallet
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.extension.toUri
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WalletConnectException
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

abstract class WalletConnect {
    companion object {
        const val TAG = "WalletConnect"

        internal const val web3jTimeout = 20L

        fun isEnabled(): Boolean =
            Session.getAccount()?.hasPin == true && !Session.getTipPub().isNullOrBlank()

        fun connect(
            url: String,
            afterConnect: (() -> Unit)? = null,
        ) {
            if (!url.startsWith("wc:")) return

            val uri =
                when {
                    url.contains("wc://") -> url
                    url.contains("wc:/") -> url.replace("wc:/", "wc://")
                    else -> url.replace("wc:", "wc://")
                }.toUri()
            val version = uri.host?.toIntOrNull()
            if (version == 2) {
                val symKey = uri.getQueryParameter("symKey")
                // only handle wc pair url
                if (symKey != null) {
                    WalletConnectV2.pair(url)
                    afterConnect?.invoke()
                }
            } else if (version == 1) {
                val tip = MixinApplication.get().getString(R.string.not_supported_wc_version)
                RxBus.publish(WCErrorEvent(WCError(WalletConnectException(0, "${tip}\n\n$url"))))
            }
        }
    }

    enum class Version {
        V2,
        TIP,
    }

    enum class RequestType {
        Connect,
        SessionProposal,
        SessionRequest,
    }

    sealed class WCSignData<T>(
        open val requestId: Long,
        open val signMessage: T,
    ) {
        data class V2SignData<T>(
            override val requestId: Long,
            override val signMessage: T,
            val sessionRequest: Wallet.Model.SessionRequest,
            var tipGas: TipGas? = null,
            var solanaFee: BigDecimal? = null,
        ) : WCSignData<T>(requestId, signMessage)

        data class TIPSignData(
            override val signMessage: String,
        ) : WCSignData<String>(0L, signMessage)
    }

    private var web3jPool = LruCache<Chain, Web3j>(3)

    protected fun getWeb3j(chain: Chain): Web3j {
        val exists = web3jPool[chain]
        return if (exists == null) {
            val web3j = Web3j.build(HttpService(chain.rpcUrl, buildOkHttpClient()))
            web3jPool.put(chain, web3j)
            web3j
        } else {
            exists
        }
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        return builder.build()
    }

    fun signMessage(
        priv: ByteArray,
        message: WCEthereumSignMessage,
    ): String {
        val keyPair = ECKeyPair.create(priv)
        val signature =
            if (message.type == WCEthereumSignMessage.WCSignType.TYPED_MESSAGE) {
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

    protected fun throwError(
        error: Response.Error,
        msgAction: ((String) -> Unit)? = null,
    ) {
        val msg = "error code: ${error.code}, message: ${error.message}"
        Timber.d("$TAG error $msg")
        msgAction?.invoke(msg)
        throw WalletConnectException(error.code, error.message)
    }
}

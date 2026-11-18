package one.mixin.android.tip.wc

import android.content.Context
import android.net.Uri
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.isWalletConnectSigningEligible
import one.mixin.android.extension.toUri
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WalletConnectAddresses
import one.mixin.android.tip.wc.internal.WalletConnectException
import one.mixin.android.tip.wc.internal.accountForChainId
import one.mixin.android.tip.wc.internal.buildUpdatedNamespaces
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.eip712SigningChain
import one.mixin.android.web3.hashEip712Message
import one.mixin.android.web3.js.Web3Signer
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.protocol.core.Response
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class SigningBlockedReason {
    WATCH_ONLY,
    MISSING_PRIVATE_KEY,
    UNSUPPORTED_WALLET,
}

sealed interface SigningWalletPrecheck {
    data class Eligible(
        val walletId: String,
        val category: String,
        val addresses: WalletConnectAddresses,
    ) : SigningWalletPrecheck

    data class Blocked(
        val walletId: String,
        val category: String,
        val reason: SigningBlockedReason,
    ) : SigningWalletPrecheck {
        fun messageResId(): Int =
            when (reason) {
                SigningBlockedReason.WATCH_ONLY -> R.string.watch_only_wallet_dapp_connect_warning_message
                SigningBlockedReason.MISSING_PRIVATE_KEY -> R.string.no_key_wallet_dapp_connect_warning_message
                SigningBlockedReason.UNSUPPORTED_WALLET -> R.string.unsupported_wallet_dapp_connect_warning_message
            }

        fun localizedMessage(context: Context): String = context.getString(messageResId())

        fun fallbackMessage(): String =
            when (reason) {
                SigningBlockedReason.WATCH_ONLY ->
                    "This is a watch-only wallet and cannot connect to dApps. Switch to a wallet that can sign."
                SigningBlockedReason.MISSING_PRIVATE_KEY ->
                    "This wallet has no private key on this device and cannot connect to dApps. Switch to a wallet that can sign."
                SigningBlockedReason.UNSUPPORTED_WALLET ->
                    "This wallet cannot sign on this device and cannot connect to dApps. Switch to a wallet that can sign."
            }

        fun toException(context: Context? = null): WalletConnectSigningBlockedException =
            WalletConnectSigningBlockedException(
                reason = reason,
                walletId = walletId,
                category = category,
                message = context?.let(::localizedMessage) ?: fallbackMessage(),
            )
    }
}

class WalletConnectSigningBlockedException(
    val reason: SigningBlockedReason,
    val walletId: String,
    val category: String,
    message: String,
) : WalletConnectException(WalletConnect.CODE_SIGNING_BLOCKED, message)

enum class SessionRequestGuardFailure {
    INELIGIBLE_WALLET,
    STALE_WALLET_SESSION,
    MISSING_CHAIN_ADDRESS,
}

sealed interface SessionRequestGuardResult {
    data class Allowed(
        val walletId: String,
        val address: String,
    ) : SessionRequestGuardResult

    data class Rejected(
        val failure: SessionRequestGuardFailure,
        val reason: SigningBlockedReason? = null,
        val message: String,
    ) : SessionRequestGuardResult
}

internal class WalletConnectSessionGuard(
    private val loadBindings: () -> Map<String, String> = { emptyMap() },
    private val saveBindings: (Map<String, String>) -> Unit = {},
) {
    private val lock = Any()
    private val proposalWalletIds = ConcurrentHashMap<String, String>()
    private val approvedProposalWalletIds = ConcurrentHashMap<String, String>()
    private val sessionWalletIds = ConcurrentHashMap<String, String>()
    @Volatile
    private var loaded = false

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (!loaded) {
                runCatching { loadBindings() }.getOrNull()?.let { persisted ->
                    sessionWalletIds.putAll(persisted)
                }
                loaded = true
            }
        }
    }

    private fun persistSessions() {
        synchronized(lock) {
            runCatching { saveBindings(sessionWalletIds.toMap()) }
        }
    }

    fun bindProposal(pairingTopic: String, walletId: String) {
        if (pairingTopic.isBlank() || walletId.isBlank()) return
        proposalWalletIds[pairingTopic] = walletId
    }

    fun clearProposal(pairingTopic: String) {
        if (pairingTopic.isBlank()) return
        proposalWalletIds.remove(pairingTopic)
        approvedProposalWalletIds.remove(pairingTopic)
    }

    fun getProposalWalletId(pairingTopic: String): String? = proposalWalletIds[pairingTopic]

    fun canApproveProposal(
        pairingTopic: String,
        precheck: SigningWalletPrecheck,
    ): Boolean {
        if (precheck !is SigningWalletPrecheck.Eligible) return false
        val expectedWalletId = proposalWalletIds[pairingTopic]
        return expectedWalletId == null || expectedWalletId == precheck.walletId
    }

    fun markProposalApproved(pairingTopic: String, walletId: String) {
        if (pairingTopic.isBlank() || walletId.isBlank()) return
        proposalWalletIds.remove(pairingTopic)
        approvedProposalWalletIds[pairingTopic] = walletId
    }

    fun consumeApprovedOrProposalWalletId(pairingTopic: String): String? {
        if (pairingTopic.isBlank()) return null
        return approvedProposalWalletIds.remove(pairingTopic) ?: proposalWalletIds.remove(pairingTopic)
    }

    fun bindSession(sessionTopic: String, walletId: String) {
        if (sessionTopic.isBlank() || walletId.isBlank()) return
        ensureLoaded()
        if (sessionWalletIds[sessionTopic] != walletId) {
            sessionWalletIds[sessionTopic] = walletId
            persistSessions()
        }
    }

    fun removeSession(sessionTopic: String) {
        if (sessionTopic.isBlank()) return
        ensureLoaded()
        if (sessionWalletIds.remove(sessionTopic) != null) {
            persistSessions()
        }
    }

    fun getSessionWalletId(sessionTopic: String): String? {
        if (sessionTopic.isBlank()) return null
        ensureLoaded()
        return sessionWalletIds[sessionTopic]
    }

    fun clearAll() {
        ensureLoaded()
        proposalWalletIds.clear()
        approvedProposalWalletIds.clear()
        if (sessionWalletIds.isNotEmpty()) {
            sessionWalletIds.clear()
            persistSessions()
        }
    }

    fun validateRequest(
        topic: String,
        chainId: String?,
        precheck: SigningWalletPrecheck,
        sessionNamespaces: Map<String, Wallet.Model.Namespace.Session> = emptyMap(),
    ): SessionRequestGuardResult =
        WalletConnect.validateSessionRequest(
            precheck = precheck,
            sessionWalletId = getSessionWalletId(topic),
            chainId = chainId,
            sessionNamespaces = sessionNamespaces,
        )

    fun handleWalletSwitch(
        precheck: SigningWalletPrecheck,
        activeSessions: List<Pair<String, Map<String, Wallet.Model.Namespace.Session>>>,
        disconnectSession: (String) -> Unit,
        updateSession: (String, Map<String, Wallet.Model.Namespace.Session>) -> Boolean,
    ) {
        if (precheck !is SigningWalletPrecheck.Eligible || !precheck.addresses.hasAnyAddress()) {
            activeSessions.forEach { (topic, _) ->
                disconnectSession(topic)
            }
            clearAll()
            return
        }
        activeSessions.forEach { (topic, namespaces) ->
            val updated = buildUpdatedNamespaces(namespaces, precheck.addresses)
            if (updated == null || !updateSession(topic, updated)) {
                disconnectSession(topic)
                removeSession(topic)
            } else {
                bindSession(topic, precheck.walletId)
            }
        }
    }
}

private fun WalletConnectAddresses.hasAnyAddress(): Boolean =
    evm.isNotBlank() || solana.isNotBlank() || bitcoin.isNotBlank()

abstract class WalletConnect {
    internal data class ConnectDispatcher(
        val pairV2: (String) -> Unit,
        val publishEvent: (WCEvent) -> Unit,
        val publishError: (WCErrorEvent) -> Unit,
        val contextProvider: () -> Context?,
    )

    companion object {
        const val TAG = "WalletConnect"
        const val CODE_SIGNING_BLOCKED = 4100

        internal const val web3jTimeout = 20L

        internal var localPrivateKeyPresenceProvider: (String) -> Boolean = { walletId ->
            runCatching {
                CryptoWalletHelper.hasPrivateKey(MixinApplication.appContext, walletId)
            }.getOrDefault(false)
        }

        internal var connectDispatcher: ConnectDispatcher = defaultConnectDispatcher()

        private fun defaultConnectDispatcher(): ConnectDispatcher =
            ConnectDispatcher(
                pairV2 = { url -> WalletConnectV2.pair(url) },
                publishEvent = { event -> RxBus.publish(event) },
                publishError = { error -> RxBus.publish(error) },
                contextProvider = { runCatching { MixinApplication.appContext }.getOrNull() },
            )

        internal fun resetTestHooks() {
            localPrivateKeyPresenceProvider = { walletId ->
                runCatching {
                    CryptoWalletHelper.hasPrivateKey(MixinApplication.appContext, walletId)
                }.getOrDefault(false)
            }
            connectDispatcher = defaultConnectDispatcher()
        }

        fun evaluateSigningWallet(
            walletId: String?,
            category: String?,
            hasLocalPrivateKey: Boolean,
            addresses: WalletConnectAddresses,
        ): SigningWalletPrecheck {
            if (walletId.isNullOrBlank()) {
                return SigningWalletPrecheck.Blocked(
                    walletId = "",
                    category = category.orEmpty(),
                    reason = SigningBlockedReason.UNSUPPORTED_WALLET,
                )
            }
            if (isWalletConnectSigningEligible(category, hasLocalPrivateKey)) {
                return SigningWalletPrecheck.Eligible(
                    walletId = walletId,
                    category = requireNotNull(category),
                    addresses = addresses,
                )
            }
            val reason =
                when (category) {
                    WalletCategory.WATCH_ADDRESS.value -> SigningBlockedReason.WATCH_ONLY
                    WalletCategory.IMPORTED_MNEMONIC.value,
                    WalletCategory.IMPORTED_PRIVATE_KEY.value -> SigningBlockedReason.MISSING_PRIVATE_KEY
                    else -> SigningBlockedReason.UNSUPPORTED_WALLET
                }
            return SigningWalletPrecheck.Blocked(
                walletId = walletId,
                category = category.orEmpty(),
                reason = reason,
            )
        }

        fun evaluateSigningWallet(
            wallet: WalletItem?,
            addresses: WalletConnectAddresses,
        ): SigningWalletPrecheck =
            evaluateSigningWallet(
                walletId = wallet?.id,
                category = wallet?.category,
                hasLocalPrivateKey = wallet?.hasLocalPrivateKey == true,
                addresses = addresses,
            )

        fun evaluateSigningWallet(
            wallet: Web3Wallet?,
            addresses: WalletConnectAddresses,
        ): SigningWalletPrecheck =
            evaluateSigningWallet(
                walletId = wallet?.id,
                category = wallet?.category,
                hasLocalPrivateKey = wallet?.hasLocalPrivateKey == true,
                addresses = addresses,
            )

        fun checkCurrentSigningWallet(
            hasPrivateKeyLookup: (String) -> Boolean = localPrivateKeyPresenceProvider,
        ): SigningWalletPrecheck {
            val walletId = Web3Signer.currentWalletId
            val category = Web3Signer.currentWalletCategory
            if (walletId.isBlank()) {
                return evaluateSigningWallet(
                    walletId = null,
                    category = category,
                    hasLocalPrivateKey = false,
                    addresses = WalletConnectAddresses("", "", ""),
                )
            }
            val effectiveHasLocalPrivateKey =
                when (category) {
                    WalletCategory.CLASSIC.value -> Web3Signer.hasLocalPrivateKey
                    WalletCategory.IMPORTED_MNEMONIC.value,
                    WalletCategory.IMPORTED_PRIVATE_KEY.value -> Web3Signer.hasLocalPrivateKey && hasPrivateKeyLookup(walletId)
                    else -> false
                }
            return evaluateSigningWallet(
                walletId = walletId,
                category = category,
                hasLocalPrivateKey = effectiveHasLocalPrivateKey,
                addresses =
                    if (isWalletConnectSigningEligible(category, effectiveHasLocalPrivateKey)) {
                        WalletConnectAddresses(Web3Signer.evmAddress, Web3Signer.solanaAddress, Web3Signer.btcAddress)
                    } else {
                        WalletConnectAddresses("", "", "")
                    },
            )
        }

        fun validateSessionRequest(
            precheck: SigningWalletPrecheck,
            sessionWalletId: String?,
            chainId: String?,
            sessionNamespaces: Map<String, Wallet.Model.Namespace.Session> = emptyMap(),
        ): SessionRequestGuardResult {
            if (precheck is SigningWalletPrecheck.Blocked) {
                return SessionRequestGuardResult.Rejected(
                    failure = SessionRequestGuardFailure.INELIGIBLE_WALLET,
                    reason = precheck.reason,
                    message = precheck.fallbackMessage(),
                )
            }
            val eligible = precheck as SigningWalletPrecheck.Eligible
            if (sessionWalletId.isNullOrBlank() || sessionWalletId != eligible.walletId) {
                return SessionRequestGuardResult.Rejected(
                    failure = SessionRequestGuardFailure.STALE_WALLET_SESSION,
                    message = "Session is not bound to the current wallet",
                )
            }
            val targetChainId = chainId
                ?: return SessionRequestGuardResult.Rejected(
                    failure = SessionRequestGuardFailure.MISSING_CHAIN_ADDRESS,
                    message = "Missing chainId for session request",
                )
            val chainAddress = eligible.addresses.accountForChainId(targetChainId)
                ?: return SessionRequestGuardResult.Rejected(
                    failure = SessionRequestGuardFailure.MISSING_CHAIN_ADDRESS,
                    message = "Current wallet has no address for $targetChainId",
                )
            if (sessionNamespaces.isNotEmpty()) {
                val prefix = "$targetChainId:"
                val matchingChainAccounts =
                    sessionNamespaces.values
                        .flatMap { it.accounts }
                        .filter { it.startsWith(prefix) }
                if (matchingChainAccounts.isNotEmpty()) {
                    val matchesCurrentAddress =
                        matchingChainAccounts.any { account ->
                            val accountAddress = account.removePrefix(prefix)
                            if (targetChainId.startsWith("eip155:")) {
                                accountAddress.equals(chainAddress, ignoreCase = true)
                            } else {
                                accountAddress == chainAddress
                            }
                        }
                    if (!matchesCurrentAddress) {
                        return SessionRequestGuardResult.Rejected(
                            failure = SessionRequestGuardFailure.STALE_WALLET_SESSION,
                            message = "Session account does not match the current wallet",
                        )
                    }
                }
            }
            return SessionRequestGuardResult.Allowed(
                walletId = eligible.walletId,
                address = chainAddress,
            )
        }

        fun isEnabled(): Boolean =
            Session.getAccount()?.hasPin == true && !Session.getTipPub().isNullOrBlank()

        fun isPaymentLink(url: String): Boolean =
            try {
                val host = parseUriHost(url)
                host == "pay.walletconnect.com" && WalletKit.Pay.isPaymentLink(url)
            } catch (e: Exception) {
                false
            }

        private fun parseUriHost(url: String): String? {
            val androidHost = runCatching { Uri.parse(url)?.host }.getOrNull()
            if (!androidHost.isNullOrBlank()) return androidHost
            return runCatching { URI(url).host }.getOrNull()
        }

        internal fun parseWcVersionAndSymKey(url: String): Pair<Int?, String?> {
            val normalizedUrl =
                when {
                    url.contains("wc://") -> url
                    url.contains("wc:/") -> url.replace("wc:/", "wc://")
                    else -> url.replace("wc:", "wc://")
                }
            val androidUri = runCatching { normalizedUrl.toUri() }.getOrNull()
            val androidHost = runCatching { androidUri?.host }.getOrNull()
            if (androidUri != null && androidHost != null) {
                return androidHost.toIntOrNull() to runCatching { androidUri.getQueryParameter("symKey") }.getOrNull()
            }
            val parsed = runCatching { URI(normalizedUrl) }.getOrNull() ?: return null to null
            val version = parsed.host?.toIntOrNull()
            val symKey =
                parsed.rawQuery
                    ?.split("&")
                    ?.firstNotNullOfOrNull { param ->
                        val parts = param.split("=", limit = 2)
                        if (parts.size == 2 && parts[0] == "symKey" && parts[1].isNotBlank()) parts[1] else null
                    }
            return version to symKey
        }

        fun connect(
            url: String,
            afterConnect: (() -> Unit)? = null,
        ) {
            if (isPaymentLink(url)) {
                connectDispatcher.publishEvent(WCEvent.Pay(Version.V2, RequestType.Pay, url))
                afterConnect?.invoke()
                return
            }
            if (!url.startsWith("wc:")) return

            val (version, symKey) = parseWcVersionAndSymKey(url)
            if (version == 2) {
                // only handle wc pair url
                if (symKey != null) {
                    when (val precheck = checkCurrentSigningWallet()) {
                        is SigningWalletPrecheck.Blocked -> {
                            val exception = precheck.toException(connectDispatcher.contextProvider())
                            connectDispatcher.publishError(WCErrorEvent(WCError(exception)))
                            afterConnect?.invoke()
                        }
                        is SigningWalletPrecheck.Eligible -> {
                            connectDispatcher.pairV2(url)
                            afterConnect?.invoke()
                        }
                    }
                }
            } else if (version == 1) {
                val tip = connectDispatcher.contextProvider()?.getString(R.string.not_supported_wc_version) ?: "Not supported WC v1"
                connectDispatcher.publishError(WCErrorEvent(WCError(WalletConnectException(0, "${tip}\n\n$url"))))
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
        Pay,
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

    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder.build()
    }

    fun signMessage(
        priv: ByteArray,
        message: WCEthereumSignMessage,
        checkChain: Boolean = false,
    ): String {
        val keyPair = ECKeyPair.create(priv)
        val signature =
            if (message.type == WCEthereumSignMessage.WCSignType.TYPED_MESSAGE) {
                if (checkChain) {
                    eip712SigningChain(message.data, null, MixinApplication.appContext.getString(R.string.eip712_invalid_chain_id))
                }
                Sign.signMessage(hashEip712Message(message.data), keyPair, false)
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

internal data class ActiveSessionWalletSnapshot(
    val boundWalletId: String?,
    val accountAddresses: List<String> = emptyList(),
)

/**
 * 解析当前存在活跃 WalletConnect 会话的钱包 ID 集合。
 * 优先使用已记录的会话绑定，若为空则按会话账户地址回查钱包，最后才回退到当前签名钱包。
 */
internal suspend fun resolveActiveConnectedWalletIds(
    sessions: List<ActiveSessionWalletSnapshot>,
    fallbackWalletId: String,
    resolveWalletIdByAddress: (suspend (String) -> String?)? = null,
): Set<String> {
    if (sessions.isEmpty()) return emptySet()
    return buildSet {
        sessions.forEach { session ->
            val boundId = session.boundWalletId?.takeIf { it.isNotBlank() }
            if (boundId != null) {
                add(boundId)
                return@forEach
            }
            var resolvedByAddress: String? = null
            if (resolveWalletIdByAddress != null) {
                for (address in session.accountAddresses.distinct()) {
                    val matched = resolveWalletIdByAddress(address)?.takeIf { it.isNotBlank() }
                    if (matched != null) {
                        resolvedByAddress = matched
                        break
                    }
                }
            }
            if (resolvedByAddress != null) {
                add(resolvedByAddress)
            } else if (fallbackWalletId.isNotBlank()) {
                add(fallbackWalletId)
            }
        }
    }
}

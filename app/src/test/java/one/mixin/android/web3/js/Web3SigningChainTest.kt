package one.mixin.android.web3.js

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.math.BigInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.web.WebFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.sol4k.Base58
import org.sol4k.Keypair
import org.sol4kt.MessageCompat
import org.sol4kt.MessageHeader
import org.sol4kt.exception.MaliciousInstructionException
import org.web3j.rlp.RlpDecoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric

@RunWith(RobolectricTestRunner::class)
class Web3SigningChainTest {
    private val secret = ByteArray(32) { 1 }
    private val signer = Keypair.fromSecretKey(secret)

    @Before
    fun initializeContext() {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext<Context>()
        Web3Signer.updateAddress("solana", signer.publicKey.toBase58())
        Web3Signer.updateAddress("ethereum", "0x1111111111111111111111111111111111111111")
        Web3Signer.switchChain(SwitchChain(Chain.Ethereum.hexReference)).getOrThrow()
    }

    @Test
    fun browserRequestKeepsSolanaChainAndSignatureAfterAnotherBridgeCall() {
        var request: JsSignMessage? = null
        val bridge = WebFragment.Web3Interface({}, { _, _, message -> error(message) }, { request = it }, { error("Missing account") })
        val message = "Security regression - no transaction".toByteArray()
        bridge.postMessage("""{"id":1,"name":"signMessage","network":"solana","object":{"data":"${Base58.encode(message)}"}}""")
        bridge.postMessage("""{"id":2,"name":"requestAccounts","network":"ethereum","object":{}}""")

        val captured = requireNotNull(request)
        assertEquals(Chain.Ethereum, Web3Signer.currentChain)
        assertEquals(Chain.Solana, captured.chain)
        assertEquals("solana", Web3Signer.jsNetworkOf(requireNotNull(captured.chain)))
        val signature = Web3Signer.signMessage(secret, requireNotNull(captured.data), captured.type, requireNotNull(captured.chain))
        assertTrue(signer.publicKey.verify(Numeric.hexStringToByteArray(signature), message))
    }

    @Test
    fun browserSigningRejectsEverySupportedTransactionMessageAfterSwitchingChain() {
        val message = MessageCompat(
            MessageCompat.MessageVersion.Legacy, MessageHeader(1, 0, 0), listOf(signer.publicKey),
            Base58.encode(ByteArray(32)), emptyList(), emptyList(),
        )
        for (version in MessageCompat.MessageVersion.entries) {
            assertThrows(MaliciousInstructionException::class.java) {
                Web3Signer.signMessage(secret, Base58.encode(message.copy(version = version).serialize()), JsSignMessage.TYPE_MESSAGE, Chain.Solana)
            }
        }
    }

    @Test
    fun previewKeepsRequestChainWhileNonceLookupIsSuspended() = runBlocking {
        val requestChain = Chain.Arbitrum
        val nonce = CompletableDeferred<BigInteger>()
        val transaction = WCEthereumTransaction(
            Web3Signer.evmAddress, Web3Signer.evmAddress, null, null, null, null, null, null, "0x0", "",
        )
        val gas = TipGas(requestChain.assetId, BigInteger.valueOf(21000), BigInteger.ONE, BigInteger.ONE)
        Web3Signer.switchChain(SwitchChain(requestChain.hexReference)).getOrThrow()
        val preview = async(start = CoroutineStart.UNDISPATCHED) {
            Web3Signer.ethPreviewTransaction(Web3Signer.evmAddress, transaction, gas, requestChain) { nonce.await() }
        }
        assertTrue(preview.isActive)
        Web3Signer.switchChain(SwitchChain(Chain.Base.hexReference)).getOrThrow()
        nonce.complete(BigInteger.ONE)

        val encoded = Numeric.hexStringToByteArray(preview.await())
        val fields = RlpDecoder.decode(encoded.copyOfRange(1, encoded.size)).values.single() as RlpList
        assertEquals(BigInteger.valueOf(42161), (fields.values.first() as RlpString).asPositiveBigInteger())
        assertEquals(Chain.Base, Web3Signer.currentChain)
    }
}

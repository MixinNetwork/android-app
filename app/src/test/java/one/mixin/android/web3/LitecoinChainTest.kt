package one.mixin.android.web3

import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.api.service.RouteService
import one.mixin.android.crypto.UtxoKeyGenerator
import one.mixin.android.db.web3.Web3RawTransactionDao
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.db.web3.vo.getChainSymbolFromName
import one.mixin.android.db.web3.vo.isTransferSupported
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.pay.generateDepositUri
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.send.UtxoTransactionSigner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal

class LitecoinChainTest {
    @Test
    fun tokenTransferUsesLitecoinKeyUtxosAndFeeUnits() = runBlocking {
        val chainId = Constants.ChainId.Litecoin
        val privateKey = "4ab54480cbbaa53d5d3ba43a3e85d221df085490e88346ad11579e17000db7bb".hexStringToByteArray()
        val sender = UtxoKeyGenerator.privateKeyToAddress(privateKey, chainId)
        val recipient = "ltc1qwlezpr3890hcp6vva9twqh27mr6edadreqvhnn"
        val token = Web3TokenItem(
            walletId = "wallet", assetId = chainId, chainId = chainId, name = "Litecoin",
            assetKey = "", symbol = "LTC", iconUrl = "", precision = 8,
            balance = "0.004", priceUsd = "1", changeUsd = "0", chainIcon = null,
            chainName = null, chainSymbol = null, hidden = false, level = 1,
        )
        val utxo = WalletOutput(
            outputId = "output", assetId = chainId,
            transactionHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            outputIndex = 2, amount = "0.004", address = sender, pubkeyHex = "", pubkeyType = "",
            status = "unspent", createdAt = "", updatedAt = "",
        )
        val rpc = Rpc(unused(RouteService::class.java), unused(Web3RawTransactionDao::class.java))

        assertTrue(token.isTransferSupported())
        assertEquals("Litecoin", token.getChainDisplayName())
        assertEquals("LTC", token.getChainSymbolFromName())
        assertEquals("litecoin:$recipient?amount=0.001", generateDepositUri(chainId, chainId, "", recipient, "0.001", 8))
        val transaction = token.buildTransaction(rpc, sender, recipient, "0.001", localUtxos = listOf(utxo), rate = BigDecimal.TEN)
        assertEquals(JsSignMessage.TYPE_UTXO_TRANSACTION, transaction.type)
        assertEquals(chainId, transaction.utxoChainId)
        assertEquals(0, BigDecimal("0.0000141").compareTo(requireNotNull(transaction.fee)))
        val signed = UtxoTransactionSigner.sign(requireNotNull(transaction.data), privateKey, chainId, listOf(utxo))
        assertEquals(sender, signed.fromAddress)
        assertEquals(listOf("output"), signed.consumedOutputIds)
    }

    private fun <T : Any> unused(type: Class<T>): T = requireNotNull(type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            error("Unexpected call: ${method.name}")
        },
    ))
}

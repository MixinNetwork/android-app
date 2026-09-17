package one.mixin.android.web3

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.service.RouteService
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.Web3RawTransactionDao
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.db.web3.vo.getChainFromName
import one.mixin.android.db.web3.vo.getChainSymbolFromName
import one.mixin.android.db.web3.vo.isNativeEvmAsset
import one.mixin.android.pay.generateDepositUri
import one.mixin.android.pay.parseEthereum
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WalletConnectAddresses
import one.mixin.android.tip.wc.internal.getSupportedNamespaces
import one.mixin.android.web3.js.SwitchChain
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.web3.js.findChainByHexReference
import one.mixin.android.web3.swap.isSwapSearchChainAvailable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.web3j.utils.Numeric
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ArcChainTest {
    @Test
    fun classicWalletSupportsArcAndPreservesNativeAndErc20Units() = runBlocking {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext<Context>()
        val chainId = Constants.ChainId.Arc
        val address = "0x1111111111111111111111111111111111111111"
        val usdcContract = "0x3600000000000000000000000000000000000000"
        val erc20Contract = "0x2222222222222222222222222222222222222222"
        val rpc = Rpc(unused(RouteService::class.java), unused(Web3RawTransactionDao::class.java))

        assertEquals("3f42cb95-274e-366b-9ab7-e1528d929a06", chainId)
        assertEquals(Chain.Arc, Web3ChainId.getChain(5042))
        assertEquals(ChainType.ethereum, Web3ChainId.getChainType(5042))
        assertEquals(Chain.Arc, findChainByHexReference("0x13b2"))
        assertEquals(chainId, Chain.Arc.getWeb3ChainId())
        assertEquals("USDC", Chain.Arc.symbol)
        assertEquals("https://rpc.mainnet.arc.io", Chain.Arc.rpcUrl)
        assertTrue(chainId in Constants.Web3ChainIds)
        assertTrue(isSwapSearchChainAvailable(chainId, setOf(Constants.ChainId.ETHEREUM_CHAIN_ID)))
        assertFalse(isSwapSearchChainAvailable(chainId, setOf(Constants.ChainId.SOLANA_CHAIN_ID)))
        val namespace = getSupportedNamespaces(WalletConnectAddresses(address, "", ""))["eip155"]!!
        assertTrue("eip155:5042:$address" in namespace.accounts)
        assertEquals("ARC Mainnet", Web3Signer.switchChain(SwitchChain("0x13b2")).getOrThrow())

        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        assertEquals(
            CryptoWalletHelper.mnemonicToAddress(mnemonic, Constants.ChainId.ETHEREUM_CHAIN_ID, index = 1),
            CryptoWalletHelper.mnemonicToAddress(mnemonic, chainId, index = 1),
        )

        val native = Web3TokenItem(
            walletId = "wallet", assetId = chainId, chainId = chainId, name = "USDC",
            assetKey = "0x0000000000000000000000000000000000000000", symbol = "USDC",
            iconUrl = "", precision = 18, balance = "10", priceUsd = "1", changeUsd = "0",
            chainIcon = null, chainName = null, chainSymbol = null, hidden = false, level = 1,
        )
        assertEquals("Arc", native.getChainDisplayName())
        assertEquals("USDC", native.getChainSymbolFromName())
        assertEquals(Chain.Arc, native.getChainFromName())
        for (token in listOf(native, native.copy(assetKey = usdcContract, precision = 6))) {
            assertTrue(token.isNativeEvmAsset())
            val tx = assertNotNull(token.buildTransaction(rpc, address, address, "1.5").wcEthereumTransaction)
            assertEquals(address, tx.to)
            assertEquals(BigInteger("1500000000000000000"), Numeric.decodeQuantity(tx.value))
            assertEquals(null, tx.data)
        }
        val erc20 = native.copy(assetId = "erc20", assetKey = erc20Contract, precision = 6)
        assertFalse(erc20.isNativeEvmAsset())
        val tx = assertNotNull(erc20.buildTransaction(rpc, address, address, "1.5").wcEthereumTransaction)
        assertEquals(erc20Contract, tx.to)
        assertEquals("0x0", tx.value)
        val data = assertNotNull(tx.data)
        assertTrue(data.startsWith("0xa9059cbb"))
        assertEquals(BigInteger("1500000"), BigInteger(data.takeLast(64), 16))
        assertEquals(Chain.Arc, Web3Signer.currentChain)
        val gas = TipGas(Chain.Arc.chainId, BigInteger("21000"), BigInteger("20000000000"), BigInteger.ZERO)
        assertEquals(0, BigDecimal("0.00042").compareTo(gas.displayValue(null)))

        assertEquals(
            "ethereum:$address@5042?value=1500000000000000000",
            generateDepositUri(chainId, chainId, usdcContract, address, "1.5", 6),
        )
        assertEquals(
            "ethereum:$usdcContract@5042/transfer?address=$address&amount=1.5&uint256=1500000",
            generateDepositUri("erc20", chainId, usdcContract, address, "1.5", 6),
        )
        val transfer = parseEthereum(
            url = "ethereum:$address@5042",
            validateAddress = { assetId, resolvedChain, destination ->
                assertEquals(chainId, resolvedChain)
                AddressResponse(destination, assetId = assetId)
            },
            getFee = { _, _ -> emptyList() },
            findAssetIdByAssetKey = { null },
            getAssetPrecisionById = { null },
            balanceCheck = { _, _, _, _ -> },
        )
        assertEquals(chainId, assertNotNull(transfer).assetId)
    }

    private fun <T : Any> unused(type: Class<T>): T = requireNotNull(type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            error("Unexpected call: ${method.name}")
        },
    ))
}

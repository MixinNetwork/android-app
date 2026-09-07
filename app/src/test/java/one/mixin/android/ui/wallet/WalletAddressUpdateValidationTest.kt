package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletAddressUpdateValidationTest {
    @Test
    fun acceptsMatchingRequestedAddressesAndExcludesUnrelatedAddresses() {
        val requests =
            listOf(
                request(BITCOIN_CHAIN_ID, "bc1-expected", "m/84'/0'/0'/0/1"),
                request(PEARL_CHAIN_ID, "prl1-expected", "m/86'/808276'/0'/0/1"),
            )
        val bitcoin = address("btc", BITCOIN_CHAIN_ID, "bc1-expected", "m/84'/0'/0'/0/1")
        val pearl = address("pearl", PEARL_CHAIN_ID, "prl1-expected", "m/86'/808276'/0'/0/1")
        val unrelated = address("evm", Constants.ChainId.ETHEREUM_CHAIN_ID, "0x123", "m/44'/60'/0'/0/1")

        val validated =
            validateWalletAddressUpdateResponse(
                WALLET_ID,
                requests,
                wallet(listOf(unrelated, pearl, bitcoin)),
            )

        assertEquals(listOf(bitcoin, pearl), validated)
    }

    @Test
    fun rejectsMismatchedDestination() {
        val request = request(PEARL_CHAIN_ID, "prl1-expected", "m/86'/808276'/0'/0/1")
        val attackerAddress = address("attacker", PEARL_CHAIN_ID, "prl1-attacker", request.path)

        assertNull(
            validateWalletAddressUpdateResponse(
                WALLET_ID,
                listOf(request),
                wallet(listOf(attackerAddress)),
            ),
        )
    }

    @Test
    fun rejectsAdditionalAddressForRequestedChain() {
        val request = request(PEARL_CHAIN_ID, "prl1-expected", "m/86'/808276'/0'/0/1")
        val expectedAddress = address("expected", PEARL_CHAIN_ID, request.destination, request.path)
        val attackerAddress = address("attacker", PEARL_CHAIN_ID, "prl1-attacker", request.path)

        assertNull(
            validateWalletAddressUpdateResponse(
                WALLET_ID,
                listOf(request),
                wallet(listOf(expectedAddress, attackerAddress)),
            ),
        )
    }

    @Test
    fun rejectsMismatchedWalletOrPath() {
        val request = request(BITCOIN_CHAIN_ID, "bc1-expected", "m/84'/0'/0'/0/1")
        val wrongWallet =
            address(
                "wrong-wallet",
                BITCOIN_CHAIN_ID,
                request.destination,
                request.path,
                walletId = "other-wallet",
            )
        val wrongPath = address("wrong-path", BITCOIN_CHAIN_ID, request.destination, "m/84'/0'/0'/0/2")

        assertNull(validateWalletAddressUpdateResponse(WALLET_ID, listOf(request), wallet(listOf(wrongWallet))))
        assertNull(validateWalletAddressUpdateResponse(WALLET_ID, listOf(request), wallet(listOf(wrongPath))))
    }

    private fun request(
        chainId: String,
        destination: String,
        path: String,
    ) = Web3AddressRequest(
        destination = destination,
        chainId = chainId,
        path = path,
    )

    private fun address(
        addressId: String,
        chainId: String,
        destination: String,
        path: String?,
        walletId: String = WALLET_ID,
    ) = Web3Address(
        addressId = addressId,
        walletId = walletId,
        chainId = chainId,
        destination = destination,
        path = path,
        createdAt = "2026-08-18T00:00:00Z",
    )

    private fun wallet(addresses: List<Web3Address>) =
        Web3Wallet(
            id = WALLET_ID,
            category = "classic",
            name = "Common Wallet 1",
            createdAt = "2026-08-18T00:00:00Z",
            updatedAt = "2026-08-18T00:00:00Z",
        ).apply {
            this.addresses = addresses
        }

    companion object {
        private const val WALLET_ID = "wallet-id"
        private const val BITCOIN_CHAIN_ID = Constants.ChainId.BITCOIN_CHAIN_ID
        private const val PEARL_CHAIN_ID = Constants.ChainId.PEARL_CHAIN_ID
    }
}

package one.mixin.android.web3.js

import one.mixin.android.api.response.web3.EIP7702SignRequest
import one.mixin.android.api.response.web3.EthGaslessTxPayload
import one.mixin.android.api.response.web3.shouldSign
import org.web3j.crypto.Hash
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.math.BigInteger

internal object GaslessEvmValidator {
    private const val GASLESS_EIP7702_AUTHORIZED_ADDRESS = "0xe6cae83bde06e4c305530e199d7217f42808555b"

    fun validate(payload: EthGaslessTxPayload, fromAddress: String, expectedChainId: BigInteger): String? {
        require(expectedChainId.signum() > 0)
        val userOperation = requireNotNull(payload.userOperation)
        require(Regex("0x[0-9a-fA-F]{40}").matches(fromAddress))
        require(fromAddress.equals(userOperation.sender, ignoreCase = true))
        val signing = requireNotNull(payload.signing)
        // UserOperation hash and callData are not verified: the account ABI and EntryPoint are not defined in this repo.
        digestBytes(requireNotNull(signing.userOperation).message)
        return signing.eip7702Auth?.takeIf { it.shouldSign }?.let { authorizationDigest(it, expectedChainId) }
    }

    fun authorizationDigest(auth: EIP7702SignRequest, expectedChainId: BigInteger): String {
        val chainId = quantity(auth.chainId)
        require(chainId.signum() > 0 && chainId == expectedChainId && chainId.bitLength() <= 256)
        require(GASLESS_EIP7702_AUTHORIZED_ADDRESS.equals(auth.address, ignoreCase = true))
        val nonce = quantity(auth.nonce)
        require(nonce.bitLength() <= 64)
        val encoded = RlpEncoder.encode(
            RlpList(
                RlpString.create(chainId),
                RlpString.create(Numeric.hexStringToByteArray(GASLESS_EIP7702_AUTHORIZED_ADDRESS)),
                RlpString.create(nonce),
            ),
        )
        val digest = Hash.sha3(byteArrayOf(0x05) + encoded)
        require(digest.contentEquals(digestBytes(auth.message)))
        return Numeric.toHexString(digest)
    }

    private fun quantity(value: String?): BigInteger {
        requireNotNull(value)
        require(Regex("(?:0[xX][0-9a-fA-F]+|[0-9]+)").matches(value))
        return if (value.startsWith("0x", ignoreCase = true)) BigInteger(value.substring(2), 16) else BigInteger(value)
    }

    private fun digestBytes(message: String?): ByteArray {
        requireNotNull(message)
        require(Regex("(?:0x)?[0-9a-fA-F]{64}").matches(message))
        return Numeric.hexStringToByteArray(message)
    }
}

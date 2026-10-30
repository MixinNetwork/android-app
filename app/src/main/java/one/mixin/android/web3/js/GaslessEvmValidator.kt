package one.mixin.android.web3.js

import one.mixin.android.api.response.web3.EIP7702SignRequest
import one.mixin.android.api.response.web3.EthGaslessTxPayload
import one.mixin.android.api.response.web3.UserOperationJson
import one.mixin.android.api.response.web3.shouldSign
import org.web3j.crypto.Hash
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.math.BigInteger

internal object GaslessEvmValidator {
    private const val GASLESS_EIP7702_AUTHORIZED_ADDRESS = "0xe6cae83bde06e4c305530e199d7217f42808555b"
    private const val ENTRY_POINT = "0x4337084d9e255ff0702461cf8895ce9e3b5ff108"

    fun validate(
        payload: EthGaslessTxPayload,
        fromAddress: String,
        expectedChainId: BigInteger,
        tokenAddress: String?,
        amount: BigInteger,
        recipient: String,
        feeTokenAddress: String?,
        feeAmount: BigInteger,
    ): Pair<String, String?> {
        require(expectedChainId.signum() > 0)
        val userOperation = requireNotNull(payload.userOperation)
        val paymasterAndData = bytes(userOperation.paymasterAndData)
        // Without a paymaster, EntryPoint charges the account beyond the confirmed fee.
        require(paymasterAndData.size >= 52 && paymasterAndData.take(20).any { it != 0.toByte() })
        addressBytes(fromAddress)
        require(fromAddress.equals(userOperation.sender, ignoreCase = true))
        addressBytes(recipient)
        require(amount.signum() > 0 && amount.bitLength() <= 256)
        require(feeAmount.signum() >= 0 && feeAmount.bitLength() <= 256)
        val calls = decodeCalls(bytes(userOperation.callData))
        val transferIndex = calls.indexOfFirst { matchesTransfer(it, tokenAddress, recipient, amount) }
        require(transferIndex >= 0)
        calls.filterIndexed { index, _ -> index != transferIndex }.forEach { call ->
            val paid = if (feeTokenAddress == null) call.value else {
                require(call.data.size == 68)
                BigInteger(1, call.data.copyOfRange(36, 68))
            }
            require(paid <= feeAmount)
            val feeRecipient = if (feeTokenAddress == null) call.target else Numeric.toHexString(call.data.copyOfRange(16, 36))
            require(matchesTransfer(call, feeTokenAddress, feeRecipient, paid))
        }
        val signing = requireNotNull(payload.signing)
        val digest = userOperationHash(userOperation, expectedChainId)
        require(digest.contentEquals(digestBytes(requireNotNull(signing.userOperation).message)))
        val authorization = signing.eip7702Auth?.takeIf { it.shouldSign }?.let { authorizationDigest(it, expectedChainId) }
        return Numeric.toHexString(digest) to authorization
    }

    private fun userOperationHash(op: UserOperationJson, chainId: BigInteger): ByteArray {
        val initCode = bytes(op.initCode)
        // v0.8 recognizes the marker only when the remaining first 20 bytes are zero.
        val delegated = initCode.size >= 2 && initCode[0] == 0x77.toByte() && initCode[1] == 0x02.toByte() &&
            initCode.take(20).drop(2).all { it == 0.toByte() }
        val effectiveInitCode = if (delegated) addressBytes(GASLESS_EIP7702_AUTHORIZED_ADDRESS) + initCode.drop(20).toByteArray() else initCode
        val structHash = Hash.sha3(
            Hash.sha3("PackedUserOperation(address sender,uint256 nonce,bytes initCode,bytes callData,bytes32 accountGasLimits,uint256 preVerificationGas,bytes32 gasFees,bytes paymasterAndData)".toByteArray()) +
                ByteArray(12) + addressBytes(op.sender) + uint(quantity(op.nonce)) +
                Hash.sha3(effectiveInitCode) + Hash.sha3(bytes(op.callData)) +
                uint(quantity(op.verificationGasLimit), 16) + uint(quantity(op.callGasLimit), 16) +
                uint(quantity(op.preVerificationGas)) +
                uint(quantity(op.maxPriorityFeePerGas), 16) + uint(quantity(op.maxFeePerGas), 16) +
                Hash.sha3(bytes(op.paymasterAndData)),
        )
        val domain = Hash.sha3(
            Hash.sha3("EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)".toByteArray()) +
                Hash.sha3("ERC4337".toByteArray()) + Hash.sha3("1".toByteArray()) +
                uint(chainId) + ByteArray(12) + addressBytes(ENTRY_POINT),
        )
        return Hash.sha3(byteArrayOf(0x19, 0x01) + domain + structHash)
    }

    private data class Call(val target: String, val value: BigInteger, val data: ByteArray)

    private fun matchesTransfer(call: Call, tokenAddress: String?, recipient: String, amount: BigInteger): Boolean =
        if (tokenAddress == null) {
            call.target.equals(recipient, ignoreCase = true) && call.value == amount && call.data.isEmpty()
        } else {
            addressBytes(tokenAddress)
            call.target.equals(tokenAddress, ignoreCase = true) && call.value == BigInteger.ZERO &&
                call.data.contentEquals(selector("transfer(address,uint256)") + ByteArray(12) + addressBytes(recipient) + uint(amount))
        }

    private fun decodeCalls(data: ByteArray): List<Call> {
        fun word(offset: Int): BigInteger {
            require(offset >= 0 && offset <= data.size - 32)
            return BigInteger(1, data.copyOfRange(offset, offset + 32))
        }
        fun smallWord(offset: Int): Int {
            val value = word(offset)
            require(value <= BigInteger.valueOf(data.size.toLong()))
            return value.toInt()
        }
        fun call(offset: Int): Pair<Call, Int> {
            val target = word(offset)
            require(target.bitLength() <= 160)
            val value = word(offset + 32)
            require(smallWord(offset + 64) == 96)
            val length = smallWord(offset + 96)
            require(length == 0 || length == 68)
            val end = offset + 128 + ((length + 31) / 32) * 32
            require(end <= data.size)
            require(data.copyOfRange(offset + 128 + length, end).all { it == 0.toByte() })
            return Call(Numeric.toHexString(uint(target, 20)), value, data.copyOfRange(offset + 128, offset + 128 + length)) to end
        }
        require(data.size >= 4)
        val selector = data.copyOfRange(0, 4)
        if (selector.contentEquals(selector("execute(address,uint256,bytes)"))) {
            val (call, end) = call(4)
            require(end == data.size)
            return listOf(call)
        }
        require(selector.contentEquals(selector("executeBatch((address,uint256,bytes)[])")))
        require(smallWord(4) == 32)
        val count = smallWord(36)
        require(count in 1..2)
        var next = 68 + count * 32
        return List(count) { index ->
            require(smallWord(68 + index * 32) == next - 68)
            val (call, end) = call(next)
            next = end
            call
        }.also { require(next == data.size) }
    }

    private fun selector(signature: String): ByteArray = Hash.sha3(signature.toByteArray()).copyOfRange(0, 4)

    private fun uint(value: BigInteger, size: Int = 32): ByteArray {
        require(value.signum() >= 0 && value.bitLength() <= size * 8)
        return Numeric.toBytesPadded(value, size)
    }

    private fun addressBytes(value: String): ByteArray {
        require(Regex("0x[0-9a-fA-F]{40}").matches(value))
        return Numeric.hexStringToByteArray(value)
    }

    private fun bytes(value: String?): ByteArray {
        requireNotNull(value)
        require(Regex("0x(?:[0-9a-fA-F]{2})*").matches(value))
        return Numeric.hexStringToByteArray(value)
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

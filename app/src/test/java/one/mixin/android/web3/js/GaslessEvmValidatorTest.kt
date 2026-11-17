package one.mixin.android.web3.js

import one.mixin.android.api.response.web3.EIP7702SignRequest
import one.mixin.android.api.response.web3.EthGaslessSignRequests
import one.mixin.android.api.response.web3.EthGaslessTxPayload
import one.mixin.android.api.response.web3.UserOperationJson
import one.mixin.android.api.response.web3.UserOpSignRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Hash
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.utils.Numeric
import java.math.BigInteger

class GaslessEvmValidatorTest {
    private val address = "0xe6cae83bde06e4c305530e199d7217f42808555b"
    private val digest = "0x5ff19acadaf683f545b800216672c273bcb8563df38a3f451ad080f8b936979b"
    private val auth = EIP7702SignRequest(null, digest, "1", address, "0")

    @Test
    fun authorizationDigestMatchesKnownRlpVector() {
        assertEquals(digest, GaslessEvmValidator.authorizationDigest(auth, BigInteger.ONE))
        assertEquals(
            digest,
            GaslessEvmValidator.authorizationDigest(
                auth.copy(chainId = "0x1", nonce = "0x0", address = address.uppercase()),
                BigInteger.ONE,
            ),
        )
        val baseDigest = "0xa0e2f32aadf3cb41a0b826c942b125f90a103d7f22466af057772f7b6c974a79"
        assertEquals(
            baseDigest,
            GaslessEvmValidator.authorizationDigest(auth.copy(chainId = "8453", message = baseDigest), BigInteger("8453")),
        )
        val nonceDigest = "0xd8b62c69c3d3376caee21f220244f51d6e13958b12ef48e3dce6e6a30ff24d7b"
        assertEquals(nonceDigest, GaslessEvmValidator.authorizationDigest(auth.copy(nonce = "1", message = nonceDigest), BigInteger.ONE))
    }

    @Test
    fun rejectsWildcardChainEvenWithMatchingDigest() {
        val wildcardDigest = Hash.sha3("0x05d78094e6cae83bde06e4c305530e199d7217f42808555b80")
        for (chainId in listOf("0", "0x0")) {
            for (expectedChain in listOf(BigInteger.ZERO, BigInteger.ONE)) {
                assertThrows(IllegalArgumentException::class.java) {
                    GaslessEvmValidator.authorizationDigest(auth.copy(chainId = chainId, message = wildcardDigest), expectedChain)
                }
            }
        }
    }

    @Test
    fun rejectsWrongChainTargetNonceAndDigest() {
        assertThrows(IllegalArgumentException::class.java) {
            GaslessEvmValidator.authorizationDigest(auth, BigInteger("8453"))
        }
        for (invalid in listOf(
            auth.copy(address = "0x" + "11".repeat(20)),
            auth.copy(nonce = "1"),
            auth.copy(nonce = "-1"),
            auth.copy(nonce = "18446744073709551616"),
            auth.copy(nonce = null),
            auth.copy(chainId = null),
            auth.copy(chainId = "-1"),
            auth.copy(message = "0x" + "00".repeat(32)),
            auth.copy(message = "0x1234"),
            auth.copy(message = "0x" + "gg".repeat(32)),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                GaslessEvmValidator.authorizationDigest(invalid, BigInteger.ONE)
            }
        }
    }

    private val recipient = "0x" + "11".repeat(20)
    private val sponsor = "0x" + "22".repeat(20)
    private val token = "0x" + "33".repeat(20)
    private val amount = BigInteger("1000000000000000000")
    private val fee = BigInteger("2000000000000000")

    private fun call(target: String, value: BigInteger, data: String = "0x") =
        DynamicStruct(Address(target), Uint256(value), DynamicBytes(Numeric.hexStringToByteArray(data)))

    private fun execute(target: String = recipient, value: BigInteger = amount, data: String = "0x"): String =
        FunctionEncoder.encode(Function("execute", call(target, value, data).value, emptyList()))

    private fun batch(vararg calls: DynamicStruct): String =
        FunctionEncoder.encode(Function("executeBatch", listOf(DynamicArray(DynamicStruct::class.java, *calls)), emptyList()))

    private fun transfer(to: String, value: BigInteger): String =
        FunctionEncoder.encode(Function("transfer", listOf(Address(to), Uint256(value)), emptyList()))

    private fun operation(callData: String = execute(), initCode: String = "0x") = UserOperationJson(
        address, "0x7", initCode, callData, "0x12345", "0x23456", "0x34567", "0x45678", "0x56789",
        "0x" + "ab".repeat(60), "0x",
    )

    private fun independentHash(op: UserOperationJson, effectiveInitCode: String = op.initCode, chainId: Int = 1): String {
        fun packed(high: String, low: String) = "0x" + high.removePrefix("0x").padStart(32, '0') + low.removePrefix("0x").padStart(32, '0')
        val json = """
            {
              "types": {
                "EIP712Domain": [
                  {"name":"name","type":"string"}, {"name":"version","type":"string"},
                  {"name":"chainId","type":"uint256"}, {"name":"verifyingContract","type":"address"}
                ],
                "PackedUserOperation": [
                  {"name":"sender","type":"address"}, {"name":"nonce","type":"uint256"},
                  {"name":"initCode","type":"bytes"}, {"name":"callData","type":"bytes"},
                  {"name":"accountGasLimits","type":"bytes32"}, {"name":"preVerificationGas","type":"uint256"},
                  {"name":"gasFees","type":"bytes32"}, {"name":"paymasterAndData","type":"bytes"}
                ]
              },
              "primaryType":"PackedUserOperation",
              "domain":{"name":"ERC4337","version":"1","chainId":$chainId,"verifyingContract":"0x4337084d9e255ff0702461cf8895ce9e3b5ff108"},
              "message":{
                "sender":"${op.sender}","nonce":"${Numeric.decodeQuantity(op.nonce)}",
                "initCode":"$effectiveInitCode","callData":"${op.callData}",
                "accountGasLimits":"${packed(op.verificationGasLimit, op.callGasLimit)}",
                "preVerificationGas":"${Numeric.decodeQuantity(op.preVerificationGas)}",
                "gasFees":"${packed(op.maxPriorityFeePerGas, op.maxFeePerGas)}",
                "paymasterAndData":"${op.paymasterAndData}"
              }
            }
        """.trimIndent()
        return Numeric.toHexString(StructuredDataEncoder(json).hashStructuredData())
    }

    private fun payload(op: UserOperationJson = operation(), message: String = independentHash(op)) =
        EthGaslessTxPayload(op, EthGaslessSignRequests(UserOpSignRequest("", message)))

    private fun validate(
        payload: EthGaslessTxPayload,
        tokenAddress: String? = null,
        value: BigInteger = amount,
        feeTokenAddress: String? = null,
        feeValue: BigInteger = fee,
        sender: String = address,
    ) = GaslessEvmValidator.validate(payload, sender, BigInteger.ONE, tokenAddress, value, recipient, feeTokenAddress, feeValue)

    @Test
    fun acceptsNativeTransferWithAndWithoutFeeAndAuthorization() {
        val single = payload()
        assertEquals(single.signing.userOperation.message, validate(single).first)
        assertNull(validate(single).second)
        assertEquals(digest, validate(single.copy(signing = single.signing.copy(eip7702Auth = auth))).second)
        assertNull(validate(single.copy(signing = single.signing.copy(eip7702Auth = auth.copy(message = "")))).second)
        val withFee = payload(operation(batch(call(recipient, amount), call(sponsor, fee))))
        assertEquals(withFee.signing.userOperation.message, validate(withFee).first)
        assertEquals(withFee.signing.userOperation.message, validate(withFee, feeValue = fee + BigInteger.ONE).first)
    }

    @Test
    fun acceptsErc20TransferWithNativeOrTokenFeeInEitherOrder() {
        val units = "1.25".toBigDecimal().movePointRight(6).toBigIntegerExact()
        for (feeToken in listOf(null, token, sponsor)) {
            val payment = if (feeToken == null) call(sponsor, fee) else call(feeToken, BigInteger.ZERO, transfer(sponsor, fee))
            val main = call(token, BigInteger.ZERO, transfer(recipient, units))
            for (calls in listOf(arrayOf(main, payment), arrayOf(payment, main))) {
                val candidate = payload(operation(batch(*calls)))
                assertEquals(candidate.signing.userOperation.message, validate(candidate, token, units, feeToken).first)
            }
        }
        val nativeWithTokenFee = payload(operation(batch(call(recipient, amount), call(token, BigInteger.ZERO, transfer(sponsor, fee)))))
        validate(nativeWithTokenFee, feeTokenAddress = token)
    }

    @Test
    fun signingRejectsAuthorizationDigestHiddenInUserOperation() {
        val otherDelegateDigest = Hash.sha3("0x05d70194" + "11".repeat(20) + "80")
        assertThrows(IllegalArgumentException::class.java) {
            validate(payload(message = otherDelegateDigest))
        }
    }

    @Test
    fun rejectsMissingOrInvalidPaymasterEvenWithMatchingHashAndZeroFee() {
        for (paymasterAndData in listOf(
            "0x",
            sponsor,
            sponsor + "00".repeat(31),
            "0x" + "00".repeat(20) + "ab".repeat(40),
        )) {
            val op = operation().copy(
                paymasterAndData = paymasterAndData,
                preVerificationGas = "0xffffffffff",
                maxFeePerGas = "0xffffffffff",
            )
            assertThrows(IllegalArgumentException::class.java) {
                validate(payload(op), feeValue = BigInteger.ZERO)
            }
        }
    }

    @Test
    fun acceptsPaymasterWithoutAdditionalDataAndZeroFee() {
        val op = operation().copy(paymasterAndData = sponsor + "00".repeat(15) + "01" + "00".repeat(16))
        val candidate = payload(op)
        assertEquals(candidate.signing.userOperation.message, validate(candidate, feeValue = BigInteger.ZERO).first)
    }

    @Test
    fun rejectsMismatchedHashSenderAndChain() {
        for (message in listOf(digest, "0x1234", "0x" + "00".repeat(32), independentHash(operation(), chainId = 8453))) {
            assertThrows(IllegalArgumentException::class.java) { validate(payload(message = message)) }
        }
        assertThrows(IllegalArgumentException::class.java) { validate(payload(), sender = recipient) }
        val original = payload()
        assertThrows(IllegalArgumentException::class.java) {
            validate(original.copy(userOperation = original.userOperation.copy(paymasterAndData = "0x")))
        }
    }

    @Test
    fun rejectsWrongRecipientAmountExtraCallSelectorAndTrailingData() {
        for (data in listOf(
            execute(sponsor), execute(value = amount + BigInteger.ONE),
            batch(call(recipient, amount), call(sponsor, fee), call(sponsor, BigInteger.ZERO)),
            "0x12345678" + execute().substring(10), execute() + "00",
            execute(data = "0x12"),
            batch(call(recipient, amount), call(sponsor, fee + BigInteger.ONE)),
            batch(call(recipient, amount), call(token, BigInteger.ZERO, transfer(sponsor, fee))),
        )) {
            assertThrows(IllegalArgumentException::class.java) { validate(payload(operation(data))) }
        }
        for (data in listOf(
            transfer(sponsor, amount), transfer(recipient, amount + BigInteger.ONE),
            transfer(recipient, amount) + "00", "0x12345678" + transfer(recipient, amount).substring(10),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                validate(payload(operation(execute(token, BigInteger.ZERO, data))), token)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            validate(payload(operation(execute(sponsor, BigInteger.ZERO, transfer(recipient, amount)))), token)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validate(payload(operation(execute(token, BigInteger.ONE, transfer(recipient, amount)))), token)
        }
        val tokenFee = payload(operation(batch(call(recipient, amount), call(token, BigInteger.ZERO, transfer(sponsor, fee + BigInteger.ONE)))))
        assertThrows(IllegalArgumentException::class.java) { validate(tokenFee, feeTokenAddress = token) }
    }

    @Test
    fun rejectsInvalidFeeCallsAndNonCanonicalAbi() {
        for (payment in listOf(
            call(sponsor, BigInteger.ZERO, transfer(sponsor, fee)),
            call(token, BigInteger.ONE, transfer(sponsor, fee)),
            call(token, BigInteger.ZERO, "0x12345678" + transfer(sponsor, fee).substring(10)),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                validate(payload(operation(batch(call(recipient, amount), payment))), feeTokenAddress = token)
            }
        }
        val tokenCall = execute(token, BigInteger.ZERO, transfer(recipient, amount))
        val badPadding = tokenCall.dropLast(2) + "01"
        val badAddressPadding = tokenCall.substring(0, 10) + "01" + tokenCall.substring(12)
        val validBatch = batch(call(recipient, amount), call(sponsor, fee))
        val overlappingCalls = validBatch.substring(0, 202) + validBatch.substring(138, 202) + validBatch.substring(266)
        for (data in listOf(badPadding, badAddressPadding)) {
            assertThrows(IllegalArgumentException::class.java) { validate(payload(operation(data)), token) }
        }
        assertThrows(IllegalArgumentException::class.java) { validate(payload(operation(overlappingCalls))) }
    }

    @Test
    fun hashes7702InitCodeUsingV08MarkerRules() {
        for ((initCode, effective) in listOf(
            "0x7702" to address,
            ("0x7702" + "00".repeat(18)) to address,
            ("0x7702" + "00".repeat(18) + "abcd") to (address + "abcd"),
            "0x770201" to "0x770201",
        )) {
            val op = operation(initCode = initCode)
            val expected = independentHash(op, effective)
            assertEquals(expected, validate(payload(op, expected)).first)
        }
    }

    @Test
    fun rejectsMalformedAbiAndOutOfRangeQuantities() {
        val valid = execute()
        val badOffset = valid.substring(0, 138) + "0".repeat(63) + "1" + valid.substring(202)
        for (data in listOf("0x", valid.dropLast(2), badOffset)) {
            assertThrows(IllegalArgumentException::class.java) { validate(payload(operation(data))) }
        }
        for (op in listOf(
            operation().copy(callGasLimit = "0x1" + "00".repeat(16)),
            operation().copy(maxPriorityFeePerGas = "-1"),
            operation().copy(nonce = "0x1" + "00".repeat(32)),
            operation().copy(initCode = "0x1"),
        )) {
            assertThrows(IllegalArgumentException::class.java) { validate(payload(op, digest)) }
        }
    }
}

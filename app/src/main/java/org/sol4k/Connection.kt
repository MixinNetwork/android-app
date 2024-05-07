package org.sol4k

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.sol4k.api.AccountInfo
import org.sol4k.api.Blockhash
import org.sol4k.api.Commitment
import org.sol4k.api.Commitment.FINALIZED
import org.sol4k.api.EpochInfo
import org.sol4k.api.Health
import org.sol4k.api.IsBlockhashValidResult
import org.sol4k.api.TokenAccountBalance
import org.sol4k.api.TransactionSimulation
import org.sol4k.api.TransactionSimulationError
import org.sol4k.api.TransactionSimulationSuccess
import org.sol4k.exception.RpcException
import org.sol4k.rpc.Balance
import org.sol4k.rpc.BlockhashResponse
import org.sol4k.rpc.EpochInfoResult
import org.sol4k.rpc.GetAccountInfoResponse
import org.sol4k.rpc.Identity
import org.sol4k.rpc.RpcErrorResponse
import org.sol4k.rpc.RpcRequest
import org.sol4k.rpc.RpcResponse
import org.sol4k.rpc.SimulateTransactionResponse
import org.sol4k.rpc.TokenBalanceResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class Connection @JvmOverloads constructor(
    private val rpcUrl: String,
    private val commitment: Commitment = FINALIZED,
) {
    @JvmOverloads
    constructor(
        rpcUrl: RpcUrl,
        commitment: Commitment = FINALIZED,
    ) : this(rpcUrl.value, commitment)

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getBalance(walletAddress: PublicKey): BigInteger {
        val balance: Balance = rpcCall("getBalance", listOf(walletAddress.toBase58()))
        return balance.value
    }

    @JvmOverloads
    fun getTokenAccountBalance(
        accountAddress: PublicKey,
        commitment: Commitment = this.commitment,
    ): TokenAccountBalance {
        val result: TokenBalanceResult = rpcCall(
            "getTokenAccountBalance",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(mapOf("commitment" to commitment.toString())),
            ),
        )
        val (amount, decimals, uiAmountString) = result.value
        return TokenAccountBalance(
            amount = BigInteger(amount),
            decimals = decimals,
            uiAmount = uiAmountString,
        )
    }

    @JvmOverloads
    fun getLatestBlockhash(commitment: Commitment = this.commitment): String =
        this.getLatestBlockhashExtended(commitment).blockhash

    @JvmOverloads
    fun getLatestBlockhashExtended(commitment: Commitment = this.commitment): Blockhash {
        val result: BlockhashResponse = rpcCall(
            "getLatestBlockhash",
            listOf(mapOf("commitment" to commitment.toString())),
        )
        return Blockhash(
            blockhash = result.value.blockhash,
            slot = result.context.slot,
            lastValidBlockHeight = result.value.lastValidBlockHeight,
        )
    }

    @JvmOverloads
    fun isBlockhashValid(blockhash: String, commitment: Commitment = this.commitment): Boolean {
        val result: IsBlockhashValidResult = rpcCall(
            "isBlockhashValid",
            listOf(
                Json.encodeToJsonElement(blockhash),
                Json.encodeToJsonElement(mapOf("commitment" to commitment.toString())),
            ),
        )
        return result.value
    }

    fun getHealth(): Health {
        val result: String = rpcCall("getHealth", listOf<String>())
        return if (result == "ok") Health.OK else Health.ERROR
    }

    fun getEpochInfo(): EpochInfo {
        val result: EpochInfoResult = rpcCall("getEpochInfo", listOf<String>())
        return EpochInfo(
            absoluteSlot = result.absoluteSlot,
            blockHeight = result.blockHeight,
            epoch = result.epoch,
            slotIndex = result.slotIndex,
            slotsInEpoch = result.slotsInEpoch,
            transactionCount = result.transactionCount,
        )
    }

    fun getIdentity(): PublicKey {
        val (identity) = rpcCall<Identity, String>("getIdentity", listOf())
        return PublicKey(identity)
    }

    fun getTransactionCount(): Long = rpcCall<Long, String>("getTransactionCount", listOf())

    fun getAccountInfo(accountAddress: PublicKey): AccountInfo? {
        val (value) = rpcCall<GetAccountInfoResponse, JsonElement>(
            "getAccountInfo",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(mapOf("encoding" to "base64")),
            ),
        )
        return value?.let {
            val data = Base64.getDecoder().decode(value.data[0])
            AccountInfo(
                data,
                executable = value.executable,
                lamports = value.lamports,
                owner = PublicKey(value.owner),
                rentEpoch = value.rentEpoch,
                space = value.space ?: data.size,
            )
        }
    }

    fun requestAirdrop(accountAddress: PublicKey, amount: Long): String {
        return rpcCall(
            "requestAirdrop",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(amount),
            ),
        )
    }

    fun sendTransaction(transactionBytes: ByteArray): String {
        val encodedTransaction = Base64.getEncoder().encodeToString(transactionBytes)
        return rpcCall(
            "sendTransaction",
            listOf(
                Json.encodeToJsonElement(encodedTransaction),
                Json.encodeToJsonElement(mapOf("encoding" to "base64")),
            )
        )
    }

    fun simulateTransaction(transaction: CompiledTransaction): TransactionSimulation {
        val encodedTransaction = Base64.getEncoder().encodeToString(transaction.serialize())
        val result: SimulateTransactionResponse = rpcCall(
            "simulateTransaction",
            listOf(
                Json.encodeToJsonElement(encodedTransaction),
                Json.encodeToJsonElement(mapOf("encoding" to "base64")),
            )
        )
        val (err, logs) = result.value
        if (err != null) {
            when (err) {
                is JsonPrimitive -> return TransactionSimulationError(err.content)
                else -> throw IllegalArgumentException("Failed to parse the error")
            }
        } else if (logs != null) {
            return TransactionSimulationSuccess(logs)
        }
        throw IllegalArgumentException("Unable to parse simulation response")
    }

    private inline fun <reified T, reified I : Any> rpcCall(method: String, params: List<I>): T {
        val connection = URL(rpcUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use {
            val body = Json.encodeToString(
                RpcRequest(method, params)
            )
            it.write(body.toByteArray())
        }
        val responseBody = connection.inputStream.use {
            BufferedReader(InputStreamReader(it)).use { reader ->
                reader.readText()
            }
        }
        connection.disconnect()
        try {
            val (result) = jsonParser.decodeFromString<RpcResponse<T>>(responseBody)
            return result
        } catch (_: SerializationException) {
            val (error) = jsonParser.decodeFromString<RpcErrorResponse>(responseBody)
            throw RpcException(error.code, error.message, responseBody)
        }
    }
}

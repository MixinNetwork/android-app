package one.mixin.android.web3

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.RpcRequest
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.web3.Web3RawTransactionDao
import one.mixin.android.web3.js.Web3Signer
import org.sol4k.PublicKey
import org.sol4k.api.AccountInfo
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.Base64

class Rpc(
    val routeService: RouteService,
    val web3RawTransactionDao: Web3RawTransactionDao,
) {
    inner class SolRpcConfig (
        val encoding: String,
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private suspend fun getNonce(chainId: String): BigInteger? = web3RawTransactionDao.getNonce(
        Web3Signer.currentWalletId, chainId)?.toBigIntegerOrNull()

    suspend fun nonceAt(chainId: String, address: String): BigInteger? {
        val remote = handleMixinResponse(
            invokeNetwork = { routeService.rpc(chainId, RpcRequest("eth_getTransactionCount", listOf(address, "latest"))) },
            successBlock = {  Numeric.decodeQuantity(it.data) }
        )

        val local = getNonce(chainId)?.add(BigInteger.ONE)
        
        val result = if (remote != null && local != null) {
            val remoteInt = remote
            val localInt = local
            if (remoteInt > localInt) remoteInt else localInt
        } else {
            remote ?: local
        }
        Timber.d("[Web3] Nonce calculation - chainId: $chainId, address: $address, remoteNonce: $remote, localNonce: $local, finalNonce: ${result}")
        return result
    }

    suspend fun getAccountInfo(accountAddress: PublicKey): AccountInfo? {
        return handleMixinResponse(
            invokeNetwork = {
                routeService.rpc(SOLANA_CHAIN_ID,
                    RpcRequest("getAccountInfo", listOf(
                        accountAddress.toBase58(),
                        SolRpcConfig("base64"),
                    )))
                },
            successBlock = {
                if (it.data == null || it.data == "null") {
                    return@handleMixinResponse null
                }
                val value = jsonParser.decodeFromString<org.sol4kt.rpc.GetAccountInfoValue>(it.data!!)
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
        )
    }

    suspend fun getTokenSupply(tokenPubkey: String): TokenAmount? {
        return handleMixinResponse(
            invokeNetwork = {
                routeService.rpc(SOLANA_CHAIN_ID,
                    RpcRequest("getTokenSupply", listOf(tokenPubkey)))
            },
            successBlock = {
                if (it.data == null || it.data == "null") {
                    return@handleMixinResponse null
                }
                jsonParser.decodeFromString<TokenAmount>(it.data!!)
            }
        )
    }

    suspend fun getMinimumBalanceForRentExemption(space: Int): Long? {
        return handleMixinResponse(
            invokeNetwork = {
                routeService.rpc(SOLANA_CHAIN_ID,
                    RpcRequest("getMinimumBalanceForRentExemption", listOf(space)))
            },
            successBlock = {
                if (it.data == null || it.data == "null") {
                    return@handleMixinResponse null
                }
                it.data!!.toLong()
            }
        )
    }

    suspend fun getLatestBlockhash(): String? {
        return handleMixinResponse(
            invokeNetwork = {
                routeService.rpc(SOLANA_CHAIN_ID,
                    RpcRequest("getLatestBlockhash", listOf(mapOf("commitment" to "confirmed")))
                )
            },
            successBlock = {
                if (it.data == null || it.data == "null") {
                    return@handleMixinResponse null
                }
                val blockhashValue = jsonParser.decodeFromString<org.sol4kt.rpc.BlockhashValue>(it.data!!)
                blockhashValue.blockhash
            }
        )
    }

    suspend fun isBlockhashValid(blockhash: String): Boolean? {
        return handleMixinResponse(
            invokeNetwork = {
                routeService.rpc(SOLANA_CHAIN_ID,
                    RpcRequest("isBlockhashValid", listOf(blockhash)))
            },
            successBlock = {
                return@handleMixinResponse jsonParser.decodeFromString<Boolean>(it.data!!)
            }
        )
    }
}

// waiting for org.sol4k.rpc.TokenAmount fix type of amount
@Serializable
data class TokenAmount(
    val amount: String,
    val decimals: Int,
    val uiAmountString: String,
)
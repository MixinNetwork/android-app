package one.mixin.android.web3

import kotlinx.serialization.json.Json
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.RpcRequest
import one.mixin.android.api.service.RouteService
import org.sol4k.PublicKey
import org.sol4k.api.AccountInfo
import org.sol4k.rpc.BlockhashValue
import org.sol4k.rpc.GetAccountInfoValue
import org.sol4k.rpc.TokenAmount
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.Base64

class Rpc(
    val routeService: RouteService,
) {
    inner class SolRpcConfig (
        val encoding: String,
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun nonceAt(chainId: String, address: String): BigInteger? {
        return handleMixinResponse(
            invokeNetwork = { routeService.rpc(chainId, RpcRequest("eth_getTransactionCount", listOf(address, "latest"))) },
            successBlock = {  Numeric.decodeQuantity(it.data) }
        )
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
                val value = jsonParser.decodeFromString<GetAccountInfoValue>(it.data!!)
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
                it.data!!.toLong()
            }
        )
    }

    suspend fun getLatestBlockhash(): String? {
        return handleMixinResponse(
            invokeNetwork = {
                routeService.rpc(SOLANA_CHAIN_ID,
                    RpcRequest("getLatestBlockhash", listOf()))
            },
            successBlock = {
                val blockhashValue = jsonParser.decodeFromString<BlockhashValue>(it.data!!)
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
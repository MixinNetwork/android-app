package one.mixin.android.web3

import one.mixin.android.api.DataErrorException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.EstimateFeeResponse
import one.mixin.android.db.web3.vo.Web3TokenFeeItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.throwIfAnyMaliciousInstruction
import org.sol4kt.VersionedTransactionCompat
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal

data class DappTransactionPreflight(
    val balance: Web3TokenFeeItem,
    val tipGas: TipGas? = null,
) {
    val insufficientBalance: Boolean
        get() = balance.amount + balance.fee > balance.token.balance.toBigDecimal()
}

suspend fun preflightDappTransaction(
    message: JsSignMessage,
    chain: Chain,
    account: String,
    findToken: suspend (String) -> Web3TokenItem?,
    estimateFee: suspend (EstimateFeeRequest) -> MixinResponse<EstimateFeeResponse>,
    cachedTipGas: TipGas? = null,
): DappTransactionPreflight? {
    val amount: BigDecimal
    val fee: BigDecimal
    val tipGas: TipGas?
    when (message.type) {
        JsSignMessage.TYPE_TRANSACTION -> {
            val transaction = message.wcEthereumTransaction ?: throw DataErrorException()
            amount = Convert.fromWei(Numeric.decodeQuantity(transaction.value ?: "0x0").toBigDecimal(), Convert.Unit.ETHER)
            tipGas = cachedTipGas ?: estimateFee(
                EstimateFeeRequest(chain.getWeb3ChainId(), null, transaction.data, transaction.from, transaction.to, transaction.value),
            ).let { response ->
                if (!response.isSuccess) throw Web3Exception(response.errorCode, response.errorDescription)
                buildTipGas(chain.chainId, response.data ?: throw DataErrorException())
            }
            fee = tipGas.displayValue(transaction.maxFeePerGas) ?: throw DataErrorException()
        }
        JsSignMessage.TYPE_RAW_TRANSACTION -> {
            val transaction = VersionedTransactionCompat.from(message.data ?: throw DataErrorException())
            transaction.throwIfAnyMaliciousInstruction()
            amount = BigDecimal.ZERO
            fee = transaction.calcFee(account)
            tipGas = null
            if (fee == BigDecimal.ZERO) return null
        }
        else -> return null
    }
    require(amount >= BigDecimal.ZERO && fee >= BigDecimal.ZERO)
    val token = findToken(chain.getWeb3ChainId()) ?: throw DataErrorException()
    return DappTransactionPreflight(Web3TokenFeeItem(token, amount, fee), tipGas)
}

package one.mixin.android.job

import androidx.room.withTransaction
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.safe.TokensExtra
import timber.log.Timber
import java.math.BigDecimal

class CheckBalanceJob(
    val assets: ArrayList<String>,
) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        private const val BALANCE_LIMIT = 100
        const val TAG = "CheckBalanceJob"
    }

    override fun onRun() = runBlocking {
        Timber.d("$TAG start checkBalance assetId size: ${assets.size}")
        assets.forEach { asset ->
            val tokensExtra = tokensExtraDao.findByAsset(asset)
            val token = tokenDao.findTokenByAsset(asset) ?: return@forEach
            mixinDatabase.withTransaction {
                val value = calcBalanceByAssetId(asset)
                if (tokensExtra == null) {
                    tokensExtraDao.insertSuspend(TokensExtra(token.assetId, token.asset, false, value.toPlainString(), nowInUtc()))
                } else {
                    tokensExtraDao.updateBalanceByAssetId(token.assetId, value.toPlainString(), nowInUtc())
                }
            }
        }
    }

    private tailrec suspend fun calcBalanceByAssetId(asset: String, offset: Int = 0, amount: BigDecimal = BigDecimal.ZERO): BigDecimal {
        var result = amount
        val outputs = if (offset == 0) {
            outputDao.findUnspentOutputsByAsset(BALANCE_LIMIT, asset)
        } else {
            outputDao.findUnspentOutputsByAssetOffset(BALANCE_LIMIT, asset, offset)
        }
        if (outputs.isEmpty()) return amount
        result += outputs.map { BigDecimal(it.amount) }.sumOf { it }
        return if (outputs.size >= BALANCE_LIMIT) {
            calcBalanceByAssetId(asset, offset + BALANCE_LIMIT, result)
        } else {
            result
        }
    }
}

class BalanceNotEqualsException(message: String) : Exception(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

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

    override fun onRun() =
        runBlocking {
            Timber.d("$TAG start checkBalance assetId size: ${assets.size}")
            val nonExistIds = assets.minus(tokenDao.findExistByKernelAssetId(assets).toSet())
            if (nonExistIds.isNotEmpty()) {
                syncToken(nonExistIds)
            }
            for (asset in assets) {
                val tokensExtra = tokensExtraDao.findByAsset(asset)
                val token = tokenDao.findTokenByAsset(asset) ?: continue
                mixinDatabase.withTransaction {
                    val amount = calcBalanceByAssetId(asset)
                    if (tokensExtra == null) {
                        tokensExtraDao.insertSuspend(TokensExtra(token.assetId, token.asset, false, amount.toPlainString(), nowInUtc()))
                    } else {
                        tokensExtraDao.updateBalanceByAssetId(token.assetId, amount.toPlainString(), nowInUtc())
                    }
                }
            }
        }

    private suspend fun syncToken(nonExistIds: List<String>) {
        val resp = tokenService.fetchTokenSuspend(nonExistIds)
        if (!resp.isSuccess || resp.data == null) {
            return
        }
        val tokens = requireNotNull(resp.data)
        tokenDao.insertList(tokens)
        return
    }

    private suspend fun calcBalanceByAssetId(asset: String): BigDecimal {
        var offset = 0
        var total = BigDecimal.ZERO

        while (true) {
            val outputs = if (offset == 0) {
                outputDao.findUnspentOutputsByAsset(BALANCE_LIMIT, asset)
            } else {
                outputDao.findUnspentOutputsByAssetOffset(BALANCE_LIMIT, asset, offset)
            }
            if (outputs.isEmpty()) break
            total += outputs.sumOf { BigDecimal(it.amount) }
            if (outputs.size < BALANCE_LIMIT) break
            offset += BALANCE_LIMIT
        }
        return total
    }
}

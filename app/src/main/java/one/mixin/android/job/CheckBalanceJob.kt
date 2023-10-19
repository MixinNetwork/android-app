package one.mixin.android.job

import androidx.room.withTransaction
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.reportException
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.assetIdToAsset
import timber.log.Timber
import java.math.BigDecimal

class CheckBalanceJob(
    val assets: ArrayList<String>,
) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "CheckBalanceJob"
    }

    override fun onRun() = runBlocking {
        Timber.d("$TAG start checkBalance assetId size: ${assets.size}")
        assets.forEach { asset ->
            val assetsExtra = assetsExtraDao.findByAsset(asset)
            val token = tokenDao.findTokenByAsset(asset)?:return@forEach
            mixinDatabase.withTransaction {
                if (assetsExtra == null) {
                    val value = outputDao.calcBalanceByAssetId(asset)
                    assetsExtraDao.insertSuspend(AssetsExtra(token.assetId, token.asset,false, BigDecimal(value).toPlainString(), nowInUtc()))
                } else {
                    val value = outputDao.calcBalanceByAssetId(asset)
                    assetsExtraDao.updateBalanceByAssetId(token.assetId, value.toString(), nowInUtc())
                }
            }
        }
    }
}

class BalanceNotEqualsException(message: String) : Exception(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

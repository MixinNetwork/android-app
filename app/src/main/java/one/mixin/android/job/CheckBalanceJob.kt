package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.util.reportException
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
            val assetExtraBalance = assetsExtraDao.findByAsset(asset)?.balance ?: return@forEach
            val calcBalance = outputDao.calcBalanceByAssetId(asset)
            if (BigDecimal(assetExtraBalance) != BigDecimal(calcBalance)) {
                val msg = "$TAG assetExtraBalance not equals calculated balance"
                Timber.d(msg)
                reportException(BalanceNotEqualsException(msg))
            }
        }
    }
}

class BalanceNotEqualsException(message: String) : Exception(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

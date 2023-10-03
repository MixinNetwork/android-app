package one.mixin.android.job

import androidx.room.withTransaction
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.toToken
import java.math.BigDecimal

class UpdateTokenJob(
    val assetId: String,
) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "UpdateTokenJob"
    }

    override fun onRun() = runBlocking {
        val exists = tokenDao.checkExists(assetId)
        if (exists == null) {
            // TODO new asset API?
            val r = assetService.getAssetByIdSuspend(assetId)
            if (!r.isSuccess || r.data == null) return@runBlocking
            val token = requireNotNull(r.data).toToken()
            tokenDao.insertSuspend(token)
        }
        MixinDatabase.getDatabase(MixinApplication.appContext).withTransaction {
            val assetsExtra = assetsExtraDao.findByAssetId(assetId)
            if (assetsExtra == null) {
                assetsExtraDao.insertSuspend(AssetsExtra(assetId, false, null, null))
            }
        }
        val assetExtra = assetsExtraDao.findByAssetId(assetId) ?: return@runBlocking
        val utxoId = assetExtra.utxoId
        val latestUtxoId = outputDao.findLatestUtxoIdByAssetId(assetId) ?: return@runBlocking
        if (latestUtxoId == utxoId) return@runBlocking

        val amount = if (utxoId == null) {
            outputDao.calcAmountByAssetId(assetId)
        } else {
            outputDao.calcAmountByUtxoIdAndAssetId(assetId, utxoId)
        }
        if (amount == 0.0) return@runBlocking
        val balance = assetExtra.balance?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        assetsExtraDao.updateBalanceAndUtxoIdByAssetId(assetId, (balance + BigDecimal(amount)).toPlainString(), latestUtxoId)
    }
}

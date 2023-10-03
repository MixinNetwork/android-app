package one.mixin.android.job

import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.toToken
import timber.log.Timber

class ProcessUtxoJob : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(TAG).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "ProcessUtxoJob"

        private const val keyProcessUtxoId = "keyProcessUtxoId"
        const val processUtxoLimit = 50
    }

    override fun onRun() = runBlocking {
        val changedAssetIds = arraySetOf<String>()
        processUtxo(changedAssetIds)
        if (changedAssetIds.isNotEmpty()) {
            jobManager.addJobInBackground(CheckBalanceJob(changedAssetIds))
        }
    }

    private tailrec suspend fun processUtxo(changedAssetIds: ArraySet<String>, utxoId: String? = null) {
        Timber.d("$TAG processUtxo changedAssetIds size: ${changedAssetIds.size}, utxoId: $utxoId")
        val processedUtxoId = utxoId ?: propertyDao.findValueByKey(keyProcessUtxoId)
        val outputs = if (processedUtxoId.isNullOrBlank()) {
            outputDao.findOutputs()
        } else {
            outputDao.findOutputsByUtxoId(processedUtxoId)
        }
        if (outputs.isEmpty()) {
            Timber.d("$TAG unprocessed outputs empty")
            return
        }
        outputs.forEach { output ->
            val exists = tokenDao.checkExists(output.assetId)
            if (exists == null) {
                // TODO new asset API?
                val r = assetService.getAssetByIdSuspend(output.assetId)
                if (!r.isSuccess || r.data == null) return // TODO
                val token = requireNotNull(r.data).toToken()
                tokenDao.insertSuspend(token)
            }
            val assetsExtra = assetsExtraDao.findByAssetId(output.assetId)
            if (assetsExtra == null) {
                assetsExtraDao.insertSuspend(AssetsExtra(output.assetId, false, null, null))
            }
            assetsExtraDao.updateBalanceByAssetId(output.assetId, output.amount)
            propertyDao.updateValueByKey(keyProcessUtxoId, output.utxoId)
        }

        changedAssetIds.addAll(outputs.groupBy { it.assetId }.keys)
        if (outputs.size <= processUtxoLimit) {
            processUtxo(changedAssetIds, outputs.last().utxoId)
        }
    }
}

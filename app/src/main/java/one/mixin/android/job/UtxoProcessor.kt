package one.mixin.android.job

import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.MixinDatabase
import one.mixin.android.util.reportException
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.toToken
import timber.log.Timber
import java.math.BigDecimal

class UtxoProcessor(
    private val mixinDatabase: MixinDatabase,
    private val jobManager: MixinJobManager,
    private val assetService: AssetService,
    private val lifecycleScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "UtxoProcessor"
        private const val keyProcessUtxoId = "keyProcessUtxoId"
        const val processUtxoLimit = 50
    }

    private val propertyDao = mixinDatabase.propertyDao()
    private val assetsExtraDao = mixinDatabase.assetsExtraDao()
    private val outputDao = mixinDatabase.outputDao()
    private val tokenDao = mixinDatabase.tokenDao()

    fun start() {
        startObserveUtxo()
    }

    fun stop() {
        stopObserveUtxo()
        utxoJob?.cancel()
    }

    private var utxoJob: Job? = null
    private val utxoObserver = object : InvalidationTracker.Observer("outputs") {
        override fun onInvalidated(tables: Set<String>) {
            runUtxoJob()
        }
    }

    private fun startObserveUtxo() {
        mixinDatabase.invalidationTracker.addObserver(utxoObserver)
    }

    private fun stopObserveUtxo() {
        mixinDatabase.invalidationTracker.removeObserver(utxoObserver)
    }

    private fun runUtxoJob() {
        if (utxoJob?.isActive == true) {
            return
        }
        utxoJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                run()
            } catch (e: Exception) {
                Timber.e(e)
                reportException(e)
                runUtxoJob()
            }
        }
    }

    private suspend fun run() {
        val changedAssetIds = ArrayList<String>()
        processUtxo(changedAssetIds)
        if (changedAssetIds.isNotEmpty()) {
            jobManager.addJobInBackground(CheckBalanceJob(changedAssetIds))
        }
    }

    private tailrec suspend fun processUtxo(changedAssetIds: ArrayList<String>, utxoId: String? = null) {
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
            var exists = tokenDao.checkExists(output.asset)
            if (exists == null) {
                // TODO new asset API?
                val r = assetService.getAssetByMixinIdSuspend(output.asset)
                if (!r.isSuccess || r.data == null) return // TODO
                val token = requireNotNull(r.data).toToken()
                exists = token.assetId
                tokenDao.insertSuspend(token)
            }
            val assetsExtra = assetsExtraDao.findByAssetId(output.asset)
            mixinDatabase.withTransaction {
                if (assetsExtra == null) {
                    assetsExtraDao.insertSuspend(AssetsExtra(exists, output.asset,false, output.amount, output.createdAt))
                } else {
                    val old = BigDecimal(assetsExtra.balance ?: "0")
                    val new = BigDecimal(output.amount).plus(old)
                    assetsExtraDao.updateBalanceByAssetId(exists, new.toPlainString(), output.createdAt)
                }
                propertyDao.updateValueByKey(keyProcessUtxoId, output.outputId)
            }
        }

        changedAssetIds.addAll(outputs.groupBy { it.asset }.keys.toSet())
        if (outputs.size <= processUtxoLimit) {
            processUtxo(changedAssetIds, outputs.last().outputId)
        }
    }
}
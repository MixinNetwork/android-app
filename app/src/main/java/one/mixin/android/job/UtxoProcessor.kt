package one.mixin.android.job

import androidx.room.InvalidationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.api.service.TokenService
import one.mixin.android.db.MixinDatabase
import one.mixin.android.util.reportException
import timber.log.Timber

class UtxoProcessor(
    private val mixinDatabase: MixinDatabase,
    private val jobManager: MixinJobManager,
    private val tokenService: TokenService,
    private val lifecycleScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "UtxoProcessor"
        private const val keyProcessUtxoId = "keyProcessUtxoId"
        const val processUtxoLimit = 50
    }

    private val propertyDao = mixinDatabase.propertyDao()
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
        val changedAssets = ArrayList<String>()
        processUtxo(changedAssets)
        if (changedAssets.isNotEmpty()) {
            jobManager.addJobInBackground(CheckBalanceJob(changedAssets))
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
        for (output in outputs) {
            val assetId = tokenDao.checkAssetExists(output.asset)
            if (assetId == null) {
                val resp = tokenService.getAssetByIdSuspend(output.asset)
                if (!resp.isSuccess || resp.data == null) {
                    // workaround not found asset
                    if (resp.error?.code == 404) {
                        continue
                    }
                }
                val token = requireNotNull(resp.data)
                tokenDao.insertSuspend(token)
            }
            propertyDao.updateValueByKey(keyProcessUtxoId, output.outputId)
        }

        changedAssetIds.addAll(outputs.groupBy { it.asset }.keys.toSet())
        if (outputs.size <= processUtxoLimit) {
            processUtxo(changedAssetIds, outputs.last().outputId)
        }
    }
}

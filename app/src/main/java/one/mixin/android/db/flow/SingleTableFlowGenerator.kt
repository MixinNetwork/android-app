package one.mixin.android.db.flow

import androidx.room.InvalidationTracker
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import one.mixin.android.util.PENDING_DB_THREAD
import timber.log.Timber

suspend fun <T> collectSingleTableFlow(
    invalidationTracker: InvalidationTracker,
    tableName: String,
    converter: () -> List<T>,
    collector: FlowCollector<List<T>>,
) = coroutineScope {
    val dataFlow by lazy {
        MutableSharedFlow<Long>(1, 1, BufferOverflow.DROP_OLDEST)
    }
    launch(PENDING_DB_THREAD) {
        dataFlow.map {
            converter()
        }.filter { it.isNotEmpty() }.collect(collector)
    }

    launch {
        callbackFlow {
            val observer = object : InvalidationTracker.Observer(tableName) {
                override fun onInvalidated(tables: Set<String>) {
                    Timber.e("send $tableName")
                    trySend(System.currentTimeMillis())
                }
            }
            invalidationTracker.addObserver(observer)
            awaitClose {
                invalidationTracker.removeObserver(observer)
            }
        }.collect {
            Timber.e("emit $tableName $it")
            dataFlow.emit(it)
        }
    }
}

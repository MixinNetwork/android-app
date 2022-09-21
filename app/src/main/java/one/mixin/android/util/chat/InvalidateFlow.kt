package one.mixin.android.util.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import timber.log.Timber

object InvalidateFlow {
    private val invalidateFlow by lazy {
        MutableSharedFlow<String>(0, 1, BufferOverflow.DROP_OLDEST)
    }

    fun emit(conversationId: String) {
        Timber.e("emit $conversationId")
        MixinApplication.get().applicationScope.launch {
            invalidateFlow.emit(conversationId)
        }
    }

    suspend fun collect(predicate: suspend (String) -> Boolean, collector: FlowCollector<String>) {
        invalidateFlow.filter(predicate).collect(collector)
    }
}

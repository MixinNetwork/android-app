package one.mixin.android.util.chat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication

object InvalidateFlow {
    private val invalidateFlow by lazy {
        MutableSharedFlow<String>(0, 1, BufferOverflow.DROP_OLDEST)
    }

    fun emit(conversationId: String) {
        MixinApplication.appScope.launch {
            invalidateFlow.emit(conversationId)
        }
    }

    suspend fun collect(collector: FlowCollector<String>) {
        invalidateFlow.collect(collector)
    }
}

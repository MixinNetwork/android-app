package one.mixin.android.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FlowTest {
    @Test
    fun testFlow() = runBlocking {
        val dropFlow by lazy {
            MutableSharedFlow<Int>(0, 1, BufferOverflow.DROP_OLDEST)
        }
        launch {
            dropFlow.collect {
                println("collect $it")
                delay(200)
                println("collect done")
            }
        }
        repeat(10) {
            delay(100)
            dropFlow.emit(it)
        }
        println("Done")
    }
}
package one.mixin.android.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.random.Random

class CoroutineTest {
    @Test
    fun testFlow() = runBlocking {
        val dropFlow by lazy {
            MutableSharedFlow<Int>(0, 1, BufferOverflow.DROP_OLDEST)
        }

        val job = launch {
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

        job.cancel()
        println("Done")
    }

    @Test
    fun testMockBarrier() = runBlocking {
        coroutineScope {
            (0..10).map { i ->
                async(Dispatchers.IO) {
                    var failed = true
                    var retryCount = 0
                    while (failed) {
                        val r = Random.nextInt(4)
                        delay(r * 1000L)

                        failed = Random.nextBoolean()
                        if (!failed) {
                            println("$i delay $r success")
                            return@async
                        }
                        retryCount++
                        println("$i delay $r failed, retry $retryCount")
                    }
                }
            }.awaitAll()
        }

        return@runBlocking
    }
}

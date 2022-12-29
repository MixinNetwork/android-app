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
import java.util.concurrent.atomic.AtomicInteger
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

    @Test
    fun testDataFlow() = runBlocking {
        val emitCount = AtomicInteger(0)
        val collectCount = AtomicInteger(0)
        val dropFlow by lazy {
            MutableSharedFlow<Int>(0, 1, BufferOverflow.DROP_OLDEST)
        }

        launch {
            dropFlow.collect {
                println("collect $it")
                // Slow processing
                val delay = Random.nextLong(100, 500)
                delay(delay)
                collectCount.addAndGet(1)
                println("collect($it - $collectCount) delay $delay done\n")
            }
        }

        repeat(10) { it ->
            val value = it + 1
            // fast or slow data
            val delay = Random.nextLong(1, 10).let {
                if (it <= 7) { // 70% of fast data
                    it * 10
                } else {
                    it * 100
                }
            }
            delay(delay)
            emitCount.addAndGet(1)
            println("emit($value - $emitCount) delay  $delay")
            dropFlow.emit(value)
        }

        println("Done")
    }
}

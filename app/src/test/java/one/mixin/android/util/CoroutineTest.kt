package one.mixin.android.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.random.Random

class CoroutineTest {
    @Test
    fun testFlow() =
        runBlocking {
            val dropFlow by lazy {
                MutableSharedFlow<Int>(0, 1, BufferOverflow.DROP_OLDEST)
            }

            val job =
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

            job.cancel()
            println("Done")
        }

    @Test
    fun testMockBarrier() =
        runBlocking {
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

    private val channel = Channel<Int>()

    private val SINGLE_LISTEN_THREAD by lazy {
        Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_LISTEN_THREAD") }.asCoroutineDispatcher()
    }

    private val SINGLE_PROCESS_THREAD by lazy {
        Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_PROCESS_THREAD") }.asCoroutineDispatcher()
    }

    @Test
    fun `test process coroutine`(): Unit =
        runBlocking {
            launch {
                for (i in channel) {
                    println("${Thread.currentThread().name} Receive: $i")
                    list.add(i)
                    if (list.size > 1) {
                        val str = list.toString()
                        list.clear()
                        launch(SINGLE_PROCESS_THREAD) {
                            processList(str)
                        }
                    }
                }
            }
            launch {
                repeat(5) {
                    delay(5)
                    channel.send(it)
                }
            }
            delay(500)
            launch {
                launch(SINGLE_PROCESS_THREAD) {
                    processList(list.toString())
                }
            }
            delay(1000)
            SINGLE_LISTEN_THREAD.close()
            SINGLE_PROCESS_THREAD.close()
            println("DONE")
        }

    private val list = mutableListOf<Int>()

    private suspend fun processList(str: String) {
        println("${Thread.currentThread().name} processList")
        delay(3)
        println("${Thread.currentThread().name} Print $str")
    }
}

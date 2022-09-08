package one.mixin.android.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import java.util.concurrent.Executors

class CoroutineTest {

    private val SINGLE_THREAD by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    private val SINGLE_THREAD_1 by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    @Test
    fun testCoroutine() = runBlocking {
        val start = System.currentTimeMillis()
        repeat(10) {
            withContext(SINGLE_THREAD) {
                work1()
                delay(10)
                withContext(SINGLE_THREAD_1) {
                    work2()
                }
                println(System.currentTimeMillis() - start)
            }
        }
    }

    @Test
    fun testCoroutine1() = runBlocking {
        val start = System.currentTimeMillis()
        repeat(10) {
            withContext(Dispatchers.IO) {
                work1()
                delay(10)
                withContext(SINGLE_THREAD) {
                    work2()
                }
                println(System.currentTimeMillis() - start)
            }
        }
    }

    private suspend fun work1() {
        delay(10)
    }

    private suspend fun work2() {
        delay(50)
    }
}

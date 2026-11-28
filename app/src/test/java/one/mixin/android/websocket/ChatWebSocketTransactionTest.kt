package one.mixin.android.websocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ChatWebSocketTransactionTest {
    @Test
    fun dispatchWebSocketTransactionInvokesCallbackOnce() {
        val transactions = ConcurrentHashMap<String, WebSocketTransaction>()
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val id = "tx-1"
        transactions[id] =
            WebSocketTransaction(
                id,
                object : TransactionCallbackSuccess {
                    override fun success(data: BlazeMessage) {
                        successCount.incrementAndGet()
                    }
                },
                object : TransactionCallbackError {
                    override fun error(data: BlazeMessage?) {
                        errorCount.incrementAndGet()
                    }
                },
            )
        val message = BlazeMessage(id = id, action = CREATE_MESSAGE, params = null)

        dispatchWebSocketTransaction(transactions, message, isError = false)
        dispatchWebSocketTransaction(transactions, message, isError = false)

        assertEquals(1, successCount.get())
        assertEquals(0, errorCount.get())
        assertFalse(transactions.containsKey(id))
    }

    @Test
    fun concurrentClearAndDispatchNeverThrowsAndNeverDoubleInvokes() {
        val iterations = 500
        val pool = Executors.newFixedThreadPool(3)
        try {
            repeat(iterations) { idx ->
                val transactions = ConcurrentHashMap<String, WebSocketTransaction>()
                val errorCount = AtomicInteger(0)
                val id = "tx-$idx"
                transactions[id] =
                    WebSocketTransaction(
                        id,
                        object : TransactionCallbackSuccess {
                            override fun success(data: BlazeMessage) = Unit
                        },
                        object : TransactionCallbackError {
                            override fun error(data: BlazeMessage?) {
                                errorCount.incrementAndGet()
                            }
                        },
                    )
                val message = BlazeMessage(id = id, action = CREATE_MESSAGE, params = null)
                val startGate = CountDownLatch(1)
                val doneGate = CountDownLatch(3)
                val errors = ConcurrentHashMap.newKeySet<Throwable>()

                pool.submit {
                    try {
                        startGate.await()
                        dispatchWebSocketTransaction(transactions, message, isError = true)
                    } catch (t: Throwable) {
                        errors.add(t)
                    } finally {
                        doneGate.countDown()
                    }
                }
                pool.submit {
                    try {
                        startGate.await()
                        dispatchWebSocketTransaction(transactions, message, isError = true)
                    } catch (t: Throwable) {
                        errors.add(t)
                    } finally {
                        doneGate.countDown()
                    }
                }
                pool.submit {
                    try {
                        startGate.await()
                        transactions.clear()
                    } catch (t: Throwable) {
                        errors.add(t)
                    } finally {
                        doneGate.countDown()
                    }
                }

                startGate.countDown()
                doneGate.await(5, TimeUnit.SECONDS)
                assertEquals(emptySet<Throwable>(), errors)
                val count = errorCount.get()
                assert(count == 0 || count == 1) { "Expected callback count 0 or 1, got $count" }
            }
        } finally {
            pool.shutdownNow()
        }
    }
}

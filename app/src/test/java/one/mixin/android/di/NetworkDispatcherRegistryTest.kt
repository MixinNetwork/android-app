package one.mixin.android.di

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NetworkDispatcherRegistryTest {
    @Test
    fun cancelAllCancelsInFlightCallsAcrossAllFiveClients() {
        val registry = NetworkDispatcherRegistry()
        val startedLatch = CountDownLatch(5)
        val releaseLatch = CountDownLatch(1)
        val cancelledCalls = mutableListOf<Call>()

        val clients =
            List(5) {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        startedLatch.countDown()
                        releaseLatch.await(5, TimeUnit.SECONDS)
                        if (chain.call().isCanceled()) {
                            throw IOException("Canceled")
                        }
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("{}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    .build()
                    .also { registry.register(it.dispatcher) }
            }

        try {
            clients.forEachIndexed { index, client ->
                val call = client.newCall(Request.Builder().url("https://example.com/$index").build())
                cancelledCalls.add(call)
                call.enqueue(
                    object : Callback {
                        override fun onFailure(
                            call: Call,
                            e: IOException,
                        ) = Unit

                        override fun onResponse(
                            call: Call,
                            response: Response,
                        ) {
                            response.close()
                        }
                    },
                )
            }

            assertTrue(startedLatch.await(5, TimeUnit.SECONDS))
            registry.cancelAll()

            cancelledCalls.forEach { call ->
                assertTrue("Expected call to be canceled", call.isCanceled())
            }
            clients.forEach { client ->
                assertEquals(0, client.dispatcher.queuedCallsCount())
            }
        } finally {
            releaseLatch.countDown()
            clients.forEach { it.dispatcher.executorService.shutdownNow() }
        }
    }
}

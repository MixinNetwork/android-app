package one.mixin.android.util.image

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class MixinImageLoaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `reuses disk cache despite no-cache response`() {
        assertSecondLoad(
            cacheControl = "no-cache",
            expectedRequestCount = 1,
            expectedDataSource = DataSource.DISK,
            expectDiskEntry = true,
        )
    }

    @Test
    fun `reuses disk cache when response has no freshness lifetime`() {
        assertSecondLoad(
            cacheControl = null,
            expectedRequestCount = 1,
            expectedDataSource = DataSource.DISK,
            expectDiskEntry = true,
        )
    }

    @Test
    fun `does not persist no-store response`() {
        assertSecondLoad(
            cacheControl = "no-store",
            expectedRequestCount = 2,
            expectedDataSource = DataSource.NETWORK,
            expectDiskEntry = false,
        )
    }

    private fun assertSecondLoad(
        cacheControl: String?,
        expectedRequestCount: Int,
        expectedDataSource: DataSource,
        expectDiskEntry: Boolean,
    ) {
        val requestCount = AtomicInteger()
        val client =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    requestCount.incrementAndGet()
                    val now = System.currentTimeMillis()
                    val response =
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "image/png")
                            .sentRequestAtMillis(now)
                            .receivedResponseAtMillis(now)
                            .body(PNG.toResponseBody("image/png".toMediaType()))
                    if (cacheControl != null) {
                        response.header("Cache-Control", cacheControl)
                    }
                    response.build()
                }
                .build()
        val diskCache =
            DiskCache.Builder()
                .directory(
                    temporaryFolder.newFolder("image-cache-${cacheControl ?: "none"}").toOkioPath(),
                )
                .maxSizeBytes(1024 * 1024)
                .build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = newMixinImageLoader(context, { client }, diskCache)

        try {
            runBlocking {
                val request =
                    ImageRequest.Builder(context)
                        .data("https://images.example.test/avatar.png")
                        .size(1, 1)
                        .build()

                val first = imageLoader.execute(request)
                assertTrue(first is SuccessResult)
                assertEquals(DataSource.NETWORK, (first as SuccessResult).dataSource)

                imageLoader.memoryCache?.clear()

                val second = imageLoader.execute(request)
                assertTrue(second is SuccessResult)
                assertEquals(expectedRequestCount, requestCount.get())
                val secondResult = second as SuccessResult
                assertEquals(expectedDataSource, secondResult.dataSource)

                if (expectDiskEntry) {
                    val copiedFile = temporaryFolder.newFile("cached-image.png")
                    imageLoader.withDiskCacheFile(secondResult) { cachedFile ->
                        cachedFile.copyTo(copiedFile, overwrite = true)
                    }
                    assertTrue(copiedFile.length() > 0)
                    assertTrue(diskCache.remove(checkNotNull(secondResult.diskCacheKey)))
                } else {
                    assertEquals(0L, diskCache.size)
                }
            }
        } finally {
            imageLoader.shutdown()
            diskCache.shutdown()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    private companion object {
        val PNG =
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
            )
    }
}

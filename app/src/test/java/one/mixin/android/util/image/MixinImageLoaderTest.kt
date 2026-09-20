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

    @Test
    fun `reuses disk cache despite stale max-age for non-svg`() {
        assertSecondLoad(
            cacheControl = "max-age=0",
            expectedRequestCount = 1,
            expectedDataSource = DataSource.DISK,
            expectDiskEntry = true,
        )
    }

    @Test
    fun `refetches svg with stale max-age`() {
        assertSecondLoad(
            cacheControl = "max-age=0",
            expectedRequestCount = 2,
            expectedDataSource = DataSource.NETWORK,
            expectDiskEntry = true,
            svg = true,
        )
    }

    @Test
    fun `reuses svg with fresh max-age`() {
        assertSecondLoad(
            cacheControl = "max-age=60",
            expectedRequestCount = 1,
            expectedDataSource = DataSource.DISK,
            expectDiskEntry = true,
            svg = true,
        )
    }

    private fun assertSecondLoad(
        cacheControl: String?,
        expectedRequestCount: Int,
        expectedDataSource: DataSource,
        expectDiskEntry: Boolean,
        svg: Boolean = false,
    ) {
        val contentType = if (svg) "image/svg+xml" else "image/png"
        val body = if (svg) SVG else PNG
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
                            .header("Content-Type", contentType)
                            .sentRequestAtMillis(now)
                            .receivedResponseAtMillis(now)
                            .body(body.toResponseBody(contentType.toMediaType()))
                    if (cacheControl != null) {
                        response.header("Cache-Control", cacheControl)
                    }
                    response.build()
                }
                .build()
        val diskCache =
            DiskCache.Builder()
                .directory(
                    temporaryFolder.newFolder().toOkioPath(),
                )
                .maxSizeBytes(1024 * 1024)
                .build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = newMixinImageLoader(context, { client }, diskCache)

        try {
            runBlocking {
                val request =
                    ImageRequest.Builder(context)
                        .data(if (svg) "https://images.example.test/sparkline.svg?duration=24H" else "https://images.example.test/avatar.png")
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
                    val copiedFile = temporaryFolder.newFile()
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
        val SVG =
            """<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"><path d="M0 0h1v1H0z"/></svg>""".toByteArray()

        val PNG =
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
            )
    }
}

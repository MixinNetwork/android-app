package one.mixin.android.util

import android.app.Application
import android.content.ClipData
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.birbit.android.jobqueue.CancelReason
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.job.ConvertDataJob
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MessageBuilder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.vo.absolutePath
import one.mixin.android.websocket.DataMessagePayload
import one.mixin.android.websocket.VideoMessagePayload
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ShareHelperTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val granted = mutableSetOf<Uri>()
    private lateinit var context: Context
    private lateinit var provider: ShareProvider
    private val uri = Uri.parse("content://external.share.provider/item")

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(application) {
            override fun checkUriPermission(uri: Uri, pid: Int, uid: Int, modeFlags: Int) =
                if (uri in granted && modeFlags == Intent.FLAG_GRANT_READ_URI_PERMISSION) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        }
        provider = ShareProvider(temporaryFolder.newFile().apply { writeText("shared bytes") })
        registerProvider(uri.authority!!, context.applicationInfo.uid + 1)
        granted.add(uri)
    }

    @Test
    fun publicTextIgnoresInternalCapabilities() = runBlocking {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "public text")
            .putExtra(ForwardActivity.ARGS_ACTION, ForwardAction.Bot(userId = "recipient"))
            .putParcelableArrayListExtra(ForwardActivity.ARGS_MESSAGES, arrayListOf(ForwardMessage(ForwardCategory.Audio, "private payload")))
            .putExtra(ForwardActivity.ARGS_TO_CONVERSATION, "conversation")
        val result = parse(intent)
        assertEquals(listOf(ForwardMessage(ShareCategory.Text, "public text")), result)
        assertNull(parse(Intent().putExtras(intent)))
    }

    @Test
    fun rejectsLocalFilesOwnProvidersAndUserPrefixedAuthoritiesBeforeOpening() = runBlocking {
        val ownAuthority = "${context.packageName}.provider"
        registerProvider(ownAuthority, context.applicationInfo.uid)
        val otherOwnedAuthority = "owned.share.provider"
        registerProvider(otherOwnedAuthority, context.applicationInfo.uid)
        assertEquals("Provider UID", context.applicationInfo.uid, context.packageManager.resolveContentProvider(otherOwnedAuthority, 0)!!.applicationInfo.uid)
        listOf(
            Uri.fromFile(temporaryFolder.newFile()),
            Uri.parse("content://$ownAuthority/root/private"),
            Uri.parse("content://0@$ownAuthority/root/private"),
            Uri.parse("content://0%40$ownAuthority/root/private"),
            Uri.parse("content://0@${ownAuthority.uppercase()}/root/private"),
            Uri.parse("content://$otherOwnedAuthority/private"),
            Uri.parse("CONTENT://$ownAuthority/root/private"),
            Uri.parse("android.resource://${context.packageName}/1"),
        ).forEach { forbidden ->
            granted.add(forbidden)
            assertRejected(shareIntent(forbidden))
            assertEquals("Opened $forbidden", 0, provider.openCount)
            assertEquals("Queried $forbidden", 0, provider.queryCount)
        }
    }

    @Test
    fun requiresReadGrantAndMatchingClipData() = runBlocking {
        assertRejected(shareIntent(uri).apply { flags = 0 })
        granted.clear()
        assertRejected(shareIntent(uri))
        granted.add(uri)
        assertRejected(shareIntent(uri).apply { clipData = ClipData.newRawUri("different", Uri.parse("content://external.share.provider/other")) })
        assertEquals(0, provider.openCount)
    }

    @Test
    fun stagesResolverBytesWithoutFollowingProviderDataPath() = runBlocking {
        provider.privatePath = temporaryFolder.newFile().apply { writeText("private bytes") }.absolutePath
        provider.name = "../../private.gif"
        provider.mime = "image/gif"
        val result = parse(shareIntent(uri, "image/gif"))!!.single()
        assertEquals(ShareCategory.Image, result.category)
        val data = GsonHelper.customGson.fromJson(result.content, ShareImageData::class.java)
        val stagedFile = File(Uri.parse(data.url).path!!)
        assertEquals("shared bytes", stagedFile.readText())
        assertNotEquals(provider.privatePath, stagedFile.absolutePath)
        assertFalse(provider.queriedDataColumn)
        assertNull(data.attachmentExtra)
    }

    @Test
    fun supportsClipOnlyMediaAndMultipleStreams() = runBlocking {
        val video = parse(shareIntent(uri, "video/mp4").apply { removeExtra(Intent.EXTRA_STREAM) })!!.single()
        assertEquals(ForwardCategory.Video, video.category)
        assertEquals("shared bytes", File(Uri.parse(GsonHelper.customGson.fromJson(video.content, VideoMessagePayload::class.java).url).path!!).readText())
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri, uri))
            .apply { clipData = ClipData.newRawUri("share", uri) }
        val files = parse(intent)!!
        assertEquals(2, files.size)
        files.forEach {
            val data = GsonHelper.customGson.fromJson(it.content, DataMessagePayload::class.java)
            assertEquals(ForwardCategory.Data, it.category)
            assertEquals("shared bytes", File(Uri.parse(data.url).path!!).readText())
            assertEquals(12L, data.fileSize)
        }
    }

    @Test
    fun rejectsOversizedListsAndCleansFailedImports() = runBlocking {
        val tooMany = Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/png")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(List(33) { uri }))
        assertRejected(tooMany)
        assertEquals(0, provider.openCount)
        provider.failOpen = true
        val directory = temporaryFolder.newFolder()
        val result = runCatching { ShareHelper.get().generateForwardMessageList(context, shareIntent(uri), directory) }
        assertTrue(result.isFailure)
        assertFalse(directory.exists())
    }

    @Test
    fun boundsActualBytesWithoutTrustingMetadata() = runBlocking {
        val output = ByteArrayOutputStream()
        assertEquals(3L, copyShareContent(ByteArrayInputStream(byteArrayOf(1, 2, 3)), output, 3))
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertTrue(runCatching { copyShareContent(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), ByteArrayOutputStream(), 3) }.isFailure)
    }

    @Test
    fun eachJobOwnsItsSourceAfterSharePageCleanup() {
        val directory = File(File(context.cacheDir, ShareHelper.STAGING_DIRECTORY), UUID.randomUUID().toString()).apply { mkdirs() }
        val source = File(directory, "source.mp4").apply { writeText("shared bytes") }
        val first = ShareHelper.retainJobSource(context, source.toUri())
        val second = ShareHelper.retainJobSource(context, source.toUri())
        directory.deleteRecursively()
        assertNotEquals(first, second)
        assertEquals("shared bytes", File(first.path!!).readText())
        ShareHelper.releaseJobSource(context, first)
        assertFalse(File(first.path!!).exists())
        assertEquals("shared bytes", File(second.path!!).readText())
        ShareHelper.releaseJobSource(context, second)
        val ordinary = temporaryFolder.newFile()
        assertEquals(ordinary.toUri(), ShareHelper.retainJobSource(context, ordinary.toUri()))
        ShareHelper.releaseJobSource(context, ordinary.toUri())
        assertTrue(ordinary.exists())
    }

    @Test
    fun queuedCancellationKeepsAttachmentSourceAvailableForRetry() {
        MixinApplication.appContext = context
        val directory = File(File(context.cacheDir, ShareHelper.STAGING_DIRECTORY), UUID.randomUUID().toString()).apply { mkdirs() }
        val shared = File(directory, "shared.bin").apply { writeText("shared bytes") }
        val source = ShareHelper.retainJobSource(context, shared.toUri())
        directory.deleteRecursively()
        try {
            val message = MessageBuilder(
                "message-id",
                "conversation-id",
                "sender-id",
                MessageCategory.PLAIN_DATA.name,
                MessageStatus.SENDING.name,
                "2026-09-07T00:00:00Z",
            )
                .setMediaUrl(source.toString())
                .setName("shared.bin")
                .setMediaMimeType("application/octet-stream")
                .setMediaSize(12L)
                .build()

            one.mixin.android.job.BaseJob::class.java.getDeclaredMethod("onCancel", Int::class.javaPrimitiveType, Throwable::class.java).apply {
                isAccessible = true
            }.invoke(ConvertDataJob(message), CancelReason.CANCELLED_WHILE_RUNNING, null)

            val retry = SendAttachmentMessageJob(message)
            val retryUri = Uri.parse(retry.message.absolutePath(context))
            assertEquals(source, retryUri)
            context.contentResolver.openInputStream(retryUri)!!.bufferedReader().use {
                assertEquals("shared bytes", it.readText())
            }
        } finally {
            ShareHelper.releaseJobSource(context, source)
        }
    }

    private fun shareIntent(uri: Uri, type: String = "application/octet-stream") =
        Intent(Intent.ACTION_SEND).setType(type)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .apply { clipData = ClipData.newRawUri("share", uri) }

    private suspend fun parse(intent: Intent) =
        ShareHelper.get().generateForwardMessageList(context, intent, temporaryFolder.newFolder())

    private suspend fun assertRejected(intent: Intent) {
        assertTrue(runCatching { parse(intent) }.isFailure)
    }

    private fun registerProvider(authority: String, uid: Int) {
        val info = ProviderInfo().apply {
            this.authority = authority
            packageName = if (uid == context.applicationInfo.uid) context.packageName else "external.share"
            name = "$authority.ShareProvider"
            applicationInfo = ApplicationInfo().apply {
                this.uid = uid
                packageName = if (uid == context.applicationInfo.uid) context.packageName else "external.share"
            }
        }
        shadowOf(context.packageManager).addOrUpdateProvider(info)
        provider.attachInfo(context, info)
        ShadowContentResolver.registerProviderInternal(authority, provider)
    }

    private class ShareProvider(private val file: File) : ContentProvider() {
        var name = "shared.bin"
        var mime = "application/octet-stream"
        var privatePath = "unused"
        var queriedDataColumn = false
        var openCount = 0
        var queryCount = 0
        var failOpen = false

        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            queryCount++
            val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, "_data")
            queriedDataColumn = queriedDataColumn || "_data" in columns
            return MatrixCursor(columns).apply { addRow(columns.map { if (it == "_data") privatePath else name }) }
        }

        override fun getType(uri: Uri) = mime

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            openCount++
            if (failOpen) throw IOException("read failed")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}

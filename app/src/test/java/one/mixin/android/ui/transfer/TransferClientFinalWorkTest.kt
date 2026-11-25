package one.mixin.android.ui.transfer

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.transfer.status.TransferStatus
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import one.mixin.android.vo.Account
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransferClientFinalWorkTest {
    private val accountId = UUID.randomUUID().toString()
    private val peerDeviceId = "peer-device-1"

    private lateinit var manager: TransferSessionManager
    private lateinit var status: TransferStatusLiveData
    private lateinit var transcriptMessageDao: TranscriptMessageDao
    private lateinit var client: TransferClient
    private lateinit var authorization: TransferAuthorization

    @Before
    fun setUp() {
        val account = mock(Account::class.java)
        `when`(account.userId).thenReturn(accountId)
        setField(Session, "self", account)

        manager = TransferSessionManager()
        val session = manager.initiateSession(TransferDirection.RESTORE_FROM_PHONE, accountId)
        manager.beginPairingConfirmation(session.sessionId, accountId, peerDeviceId, 123456)
        authorization = manager.confirmAndConsumePairing(session.sessionId, accountId, peerDeviceId)!!

        status = TransferStatusLiveData().apply { value = TransferStatus.PROCESSING }
        transcriptMessageDao = mock(TranscriptMessageDao::class.java)
        client =
            TransferClient(
                ApplicationProvider.getApplicationContext<Application>(),
                mock(MixinDatabase::class.java),
                mock(AssetDao::class.java),
                mock(ConversationDao::class.java),
                mock(ConversationExtDao::class.java),
                mock(ExpiredMessageDao::class.java),
                mock(MessageDao::class.java),
                mock(ParticipantDao::class.java),
                mock(PinMessageDao::class.java),
                mock(SnapshotDao::class.java),
                mock(StickerDao::class.java),
                transcriptMessageDao,
                mock(UserDao::class.java),
                mock(AppDao::class.java),
                mock(MessageMentionDao::class.java),
                mock(RemoteMessageStatusDao::class.java),
                mock(FtsDatabase::class.java),
                status,
                Json,
                CoroutineScope(Dispatchers.Unconfined),
                mock(MixinJobManager::class.java),
                manager,
            )
        setField(client, "currentAuthorization", authorization)
        setField(client, "receivedFinish", true)
        // exit(true) sets quit before finalWork runs
        setField(client, "quit", true)
    }

    @After
    fun tearDown() {
        setField(Session, "self", null)
    }

    @Test
    fun `attachment processing failure marks status as error instead of staying in processing`() {
        val cacheRoot = client.context.externalCacheDir ?: client.context.cacheDir
        val attachmentDir = File(File(cacheRoot, authorization.sessionId), "attachment").apply { mkdirs() }
        File(attachmentDir, UUID.randomUUID().toString()).writeText("attachment")
        `when`(transcriptMessageDao.findAttachmentMessage(anyString()))
            .thenThrow(IllegalStateException("attachment failed"))

        invokeFinalWork()

        assertEquals(TransferStatus.ERROR, status.value)
        assertEquals(TransferSessionState.CANCELLED, manager.getSessionState(authorization.sessionId))
    }

    private fun invokeFinalWork() {
        val method = TransferClient::class.java.getDeclaredMethod("finalWork")
        method.isAccessible = true
        method.invoke(client)
    }

    private fun setField(
        target: Any,
        name: String,
        value: Any?,
    ) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }
}

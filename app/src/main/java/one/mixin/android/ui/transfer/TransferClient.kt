package one.mixin.android.ui.transfer

import android.database.SQLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
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
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.extension.createAtToLong
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.ui.transfer.status.TransferStatus
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.CURRENT_TRANSFER_VERSION
import one.mixin.android.ui.transfer.vo.TransferCommand
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.compatible.TransferMessageMention
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.min
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject

class TransferClient @Inject internal constructor(
    val assetDao: AssetDao,
    val conversationDao: ConversationDao,
    val conversationExtDao: ConversationExtDao,
    val expiredMessageDao: ExpiredMessageDao,
    val messageDao: MessageDao,
    val participantDao: ParticipantDao,
    val pinMessageDao: PinMessageDao,
    val snapshotDao: SnapshotDao,
    val stickerDao: StickerDao,
    val transcriptMessageDao: TranscriptMessageDao,
    val userDao: UserDao,
    val appDao: AppDao,
    val messageMentionDao: MessageMentionDao,
    val remoteMessageStatusDao: RemoteMessageStatusDao,
    val ftsDatabase: FtsDatabase,
    val status: TransferStatusLiveData,
    private val serializationJson: Json,
) {

    private var socket: Socket? = null
    private var quit = false
    private var count = 0L
    private var startTime = 0L
    private var currentType: String? = null
        set(value) {
            if (field != value){
                field = value
                Timber.e("Current type: $field")
            }
        }
    private var currentId: String? = null // Save the currently inserted primary key id

    private var deviceId: String? = null

    val protocol = TransferProtocol()

    private val syncChannel = Channel<ByteArray>()

    suspend fun connectToServer(ip: String, port: Int, commandData: TransferCommand) =
        withContext(SINGLE_SOCKET_THREAD) {
            try {
                status.value = TransferStatus.CONNECTING
                val socket = Socket(ip, port)
                this@TransferClient.socket = socket
                status.value = TransferStatus.WAITING_FOR_VERIFICATION
                // send connect command
                sendCommand(socket.outputStream, commandData)
                launch(Dispatchers.IO) { listen(socket.inputStream, socket.outputStream) }
                launch(Dispatchers.IO) {
                    for (byteArray in syncChannel) {
                        processJson(byteArray)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.ERROR) {
                    status.value = TransferStatus.ERROR
                }
                exit()
            }
        }

    private var total = 0L

    private suspend fun listen(inputStream: InputStream, outputStream: OutputStream) {
        do {
            status.value = TransferStatus.SYNCING
            val result = try {
                protocol.read(inputStream)
            } catch (e: EOFException) {
                if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.ERROR) {
                    status.value = TransferStatus.ERROR
                }
                Timber.e(e)
                exit()
                null
            }
            when (result) {
                is TransferCommand -> {
                    when (result.action) {
                        TransferCommandAction.START.value -> {
                            if (result.version != CURRENT_TRANSFER_VERSION) {
                                Timber.e("Version does not support")
                                exit()
                                return
                            }
                            startTime = System.currentTimeMillis()
                            this.total = result.total ?: 0L
                        }

                        TransferCommandAction.PUSH.value, TransferCommandAction.PULL.value -> {
                            Timber.e("action ${result.action}")
                        }

                        TransferCommandAction.FINISH.value -> {
                            status.value = TransferStatus.FINISHED
                            sendCommand(outputStream, TransferCommand(TransferCommandAction.FINISH.value))
                            delay(100)
                            exit(true)
                            Timber.e("It takes a total of ${System.currentTimeMillis() - startTime} milliseconds to synchronize ${this.total} data")
                        }

                        else -> {
                        }
                    }
                }
                is ByteArray -> {
                    syncChannel.send(result)
                    progress(outputStream)
                }
                else -> {
                    // read file
                    progress(outputStream)
                }
            }
        } while (!quit)
    }

    private var lastTime = 0L

    private var lastProgress = 0f
    private fun progress(outputStream: OutputStream) {
        if (quit || total <= 0 || status.value == TransferStatus.ERROR) return
        val progress = min((count++) / total.toFloat() * 100, 100f)
        if (lastProgress != progress && System.currentTimeMillis() - lastTime > 300) {
            sendCommand(
                outputStream,
                TransferCommand(TransferCommandAction.PROGRESS.value, progress = progress),
            )
            lastProgress = progress
            lastTime = System.currentTimeMillis()
            RxBus.publish(DeviceTransferProgressEvent(progress))
            Timber.e("Device transfer $progress")
        }
    }

    private val mutableList: MutableList<Message> = mutableListOf()

    private fun processJson(byteArray: ByteArray) {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val transferData = serializationJson.decodeFromStream<TransferData<JsonElement>>(byteArrayInputStream)
        currentType = transferData.type
        when (transferData.type) {
            TransferDataType.CONVERSATION.value -> {
                val conversation = serializationJson.decodeFromJsonElement<Conversation>(transferData.data)
                conversationDao.insertIgnore(conversation)
                currentId = conversation.conversationId
            }

            TransferDataType.PARTICIPANT.value -> {
                val participant = serializationJson.decodeFromJsonElement<Participant>(transferData.data)
                participantDao.insertIgnore(participant)
                currentId = "${participant.conversationId}+${participant.userId}"
            }

            TransferDataType.USER.value -> {
                val user = serializationJson.decodeFromJsonElement<User>(transferData.data)
                userDao.insertIgnore(user)
                currentId = user.userId
            }

            TransferDataType.APP.value -> {
                val app = serializationJson.decodeFromJsonElement<App>(transferData.data)
                appDao.insertIgnore(app)
                currentId = app.appId
            }

            TransferDataType.ASSET.value -> {
                val asset = serializationJson.decodeFromJsonElement<Asset>(transferData.data)
                assetDao.insertIgnore(asset)
                currentId = asset.assetId
            }

            TransferDataType.SNAPSHOT.value -> {
                val snapshot = serializationJson.decodeFromJsonElement<Snapshot>(transferData.data)
                snapshotDao.insertIgnore(snapshot)
                currentId = snapshot.snapshotId
            }

            TransferDataType.STICKER.value -> {
                val sticker = serializationJson.decodeFromJsonElement<Sticker>(transferData.data)
                sticker.lastUseAt?.let {
                    try {
                        sticker.lastUseAt = it.createAtToLong().toString()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
                stickerDao.insertIgnore(sticker)
                currentId = sticker.stickerId
            }

            TransferDataType.PIN_MESSAGE.value -> {
                val pinMessage = serializationJson.decodeFromJsonElement<PinMessage>(transferData.data)
                pinMessageDao.insertIgnore(pinMessage)
                currentId = pinMessage.messageId
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                val transcriptMessage = serializationJson.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                transcriptMessageDao.insertIgnore(transcriptMessage)
                currentId = "${transcriptMessage.transcriptId}+${transcriptMessage.messageId}"
            }

            TransferDataType.MESSAGE.value -> {
                val message = serializationJson.decodeFromJsonElement<Message>(transferData.data)
                if (messageDao.findMessageIdById(message.messageId) == null) {
                    mutableList.add(message)
                    if (mutableList.size >= 1000) {
                        insertMessages(mutableList)

                        mutableList.clear()
                    }
                    ftsDatabase.insertOrReplaceMessageFts4(message)
                }
            }

            TransferDataType.MESSAGE_MENTION.value -> {
                if (mutableList.isNotEmpty()) {
                    insertMessages(mutableList)

                    mutableList.clear()
                }
                val messageMention =
                    serializationJson.decodeFromJsonElement<TransferMessageMention>(transferData.data).let {
                        val messageContent =
                            messageDao.findMessageContentById(it.conversationId, it.messageId)
                                ?: return
                        val mentionData = parseMentionData(messageContent, userDao) ?: return
                        MessageMention(it.messageId, it.conversationId, mentionData, it.hasRead)
                    }
                messageMentionDao.insertIgnoreReturn(messageMention)
                currentId = messageMention.messageId
            }

            TransferDataType.EXPIRED_MESSAGE.value -> {
                if (mutableList.isNotEmpty()) {
                    insertMessages(mutableList)

                    mutableList.clear()
                }
                val expiredMessage =
                    serializationJson.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                expiredMessageDao.insertIgnore(expiredMessage)
                currentId = expiredMessage.messageId
            }

            else -> {
                Timber.e("No support ${transferData.type}")
            }
        }
        byteArrayInputStream.close()
    }

    private fun finalWork() {
        if (mutableList.isNotEmpty()) {
            insertMessages(mutableList)
        }
        conversationDao.getAllConversationId().forEach { conversationId ->
            conversationDao.refreshLastMessageId(conversationId)
            remoteMessageStatusDao.updateConversationUnseen(conversationId)
        }
        conversationExtDao.getAllConversationId().forEach { conversationId ->
            conversationExtDao.refreshCountByConversationId(conversationId)
        }
        mutableList.clear()
    }

    fun exit(finished: Boolean = false) = MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
        try {
            if (!finished) {
                Timber.e("DeviceId: $deviceId type: $currentType id: $currentId current-time:${System.currentTimeMillis()}")
            } else {
                Timber.e("Finish exit ${System.currentTimeMillis() - startTime}/1000 s")
            }
            if (socket != null) {
                quit = true
                socket?.close()
                socket = null
            }
            finalWork()
        } catch (e: Exception) {
            Timber.e("Exit client ${e.message}")
        }
    }

    private fun sendCommand(
        outputStream: OutputStream,
        command: TransferCommand,
    ) {
        try {
            protocol.write(outputStream, TransferProtocol.TYPE_COMMAND, serializationJson.encodeToString(TransferCommand.serializer(), command))
            outputStream.flush()
        } catch (e: SocketException) {
            exit()
            if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.ERROR) {
                status.value = TransferStatus.ERROR
            }
        }
    }

    private fun insertMessages(messages: List<Message>) {
        val writableDatabase = MixinDatabase.getWritableDatabase() ?: return

        val sql =
            "INSERT OR IGNORE INTO messages (id, conversation_id, user_id, category, content, media_url, media_mime_type, media_size, media_duration, media_width, media_height, media_hash, thumb_image, thumb_url, media_key, media_digest, media_status, status, created_at, action, participant_id, snapshot_id, hyperlink, name, album_id, sticker_id, shared_user_id, media_waveform, media_mine_type, quote_message_id, quote_content, caption) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        val statement = writableDatabase.compileStatement(sql)

        writableDatabase.beginTransaction()
        try {
            for (message in messages) {
                statement.bindString(1, message.messageId)
                statement.bindString(2, message.conversationId)
                statement.bindString(3, message.userId)
                statement.bindString(4, message.category)

                val content = message.content
                if (content != null) {
                    statement.bindString(5, content)
                } else {
                    statement.bindNull(5)
                }

                val mediaUrl = message.mediaUrl
                if (mediaUrl != null) {
                    statement.bindString(6, mediaUrl)
                } else {
                    statement.bindNull(6)
                }

                val mediaMimeType = message.mediaMimeType
                if (mediaMimeType != null) {
                    statement.bindString(7, mediaMimeType)
                } else {
                    statement.bindNull(7)
                }

                val mediaSize = message.mediaSize
                if (mediaSize != null) {
                    statement.bindLong(8, mediaSize)
                } else {
                    statement.bindNull(8)
                }

                val mediaDuration = message.mediaDuration
                if (mediaDuration != null) {
                    statement.bindString(9, mediaDuration)
                } else {
                    statement.bindNull(9)
                }

                val mediaWidth = message.mediaWidth
                if (mediaWidth != null) {
                    statement.bindLong(10, mediaWidth.toLong())
                } else {
                    statement.bindNull(10)
                }

                val mediaHeight = message.mediaHeight
                if (mediaHeight != null) {
                    statement.bindLong(11, mediaHeight.toLong())
                } else {
                    statement.bindNull(11)
                }

                val mediaHash = message.mediaHash
                if (mediaHash != null) {
                    statement.bindString(12, mediaHash)
                } else {
                    statement.bindNull(12)
                }

                val thumbImage = message.thumbImage
                if (thumbImage != null) {
                    statement.bindString(13, thumbImage)
                } else {
                    statement.bindNull(13)
                }

                val thumbUrl = message.thumbUrl
                if (thumbUrl != null) {
                    statement.bindString(14, thumbUrl)
                } else {
                    statement.bindNull(14)
                }

                val mediaKey = message.mediaKey
                if (mediaKey != null) {
                    statement.bindBlob(15, mediaKey)
                } else {
                    statement.bindNull(15)
                }

                val mediaDigest = message.mediaDigest
                if (mediaDigest != null) {
                    statement.bindBlob(16, mediaDigest)
                } else {
                    statement.bindNull(16)
                }

                val mediaStatus = message.mediaStatus
                if (mediaStatus == null) {
                    statement.bindNull(17)
                } else {
                    statement.bindString(17, mediaStatus)
                }
                statement.bindString(18, message.status)
                statement.bindString(19, message.createdAt)
                val action = message.action
                if (action == null) {
                    statement.bindNull(20)
                } else {
                    statement.bindString(20, action)
                }

                val participantId = message.participantId
                if (participantId != null) {
                    statement.bindString(21, participantId)
                } else {
                    statement.bindNull(21)
                }

                val snapshotId = message.snapshotId
                if (snapshotId != null) {
                    statement.bindString(22, snapshotId)
                } else {
                    statement.bindNull(22)
                }

                val hyperlink = message.hyperlink
                if (hyperlink != null) {
                    statement.bindString(23, hyperlink)
                } else {
                    statement.bindNull(23)
                }

                val name = message.name
                if (name != null) {
                    statement.bindString(24, name)
                } else {
                    statement.bindNull(24)
                }

                val albumId = message.albumId
                if (albumId != null) {
                    statement.bindString(25, albumId)
                } else {
                    statement.bindNull(25)
                }

                val stickerId = message.stickerId
                if (stickerId != null) {
                    statement.bindString(26, stickerId)
                } else {
                    statement.bindNull(26)
                }

                val sharedUserId = message.sharedUserId
                if (sharedUserId != null) {
                    statement.bindString(27, sharedUserId)
                } else {
                    statement.bindNull(27)
                }

                val mediaWaveform = message.mediaWaveform
                if (mediaWaveform != null) {
                    statement.bindBlob(28, mediaWaveform)
                } else {
                    statement.bindNull(28)
                }

                statement.bindNull(29)

                val quoteMessageId = message.quoteMessageId
                if (quoteMessageId != null) {
                    statement.bindString(30, quoteMessageId)
                } else {
                    statement.bindNull(30)
                }

                val quoteContent = message.quoteContent
                if (quoteContent != null) {
                    statement.bindString(31, quoteContent)
                } else {
                    statement.bindNull(31)
                }

                val caption = message.caption
                if (caption != null) {
                    statement.bindString(32, caption)
                } else {
                    statement.bindNull(32)
                }

                statement.executeInsert()
            }
            writableDatabase.setTransactionSuccessful()
            currentId = messages.last().messageId
        } catch (e: SQLException) {
            Timber.e(e)
        } finally {
            writableDatabase.endTransaction()
            statement.close()
        }
    }
}

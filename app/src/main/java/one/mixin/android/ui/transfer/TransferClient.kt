package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferMessage
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.toMessage
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import timber.log.Timber
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import javax.inject.Inject

class TransferClient@Inject internal constructor(
    val assetDao: AssetDao,
    val conversationDao: ConversationDao,
    val expiredMessageDao: ExpiredMessageDao,
    val messageDao: MessageDao,
    val participantDao: ParticipantDao,
    val pinMessageDao: PinMessageDao,
    val snapshotDao: SnapshotDao,
    val stickerDao: StickerDao,
    val transcriptMessageDao: TranscriptMessageDao,
    val userDao: UserDao,
) {

    private lateinit var socket: Socket
    private var quit = false

    private val gson by lazy {
        GsonHelper.customGson
    }

    private var count = 0

    private val inputStream by lazy {
        socket.getInputStream()
    }

    private val outputStream by lazy {
        socket.getOutputStream()
    }

    fun sendMessage(message: String) {
        protocol.write(outputStream, message)
        outputStream.flush()
    }

    val protocol = TransferProtocol()

    fun connectToServer(ip: String, port: Int, commandData: TransferCommandData) {
        try {
            socket = Socket(ip, port)
            sendMessage(gson.toJson(TransferSendData(TransferDataType.COMMAND.value, commandData)))
            run()
        } catch (e: UnknownHostException) {
            Timber.e(e)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun run() {
        MixinApplication.get().applicationScope.launch(Dispatchers.IO) {
            try {
                do {
                    if (inputStream.available() <= 0) delay(1000)
                    val content = protocol.read(inputStream)
                    if (content == "FINISH") {
                        // finishListener("Finish Synchronized $count messages, ${System.currentTimeMillis() - startTime} ms")
                        exit()
                    } else if (content.startsWith("file")) {
                        // do noting
                        count++
                    } else {
                        Timber.e("sync $content")
                        val transferData = gson.fromJson(content, TransferData::class.java)

                        when (transferData.type) {
                            TransferDataType.COMMAND.value -> {
                                // Todo handle command
                            }
                            TransferDataType.MESSAGE.value -> {
                                val message = gson.fromJson(transferData.data, TransferMessage::class.java)
                                messageDao.insert(message.toMessage())
                                Timber.e("Message ID: ${message.messageId}")
                                count++
                            }
                            TransferDataType.PARTICIPANT.value -> {
                                val participant = gson.fromJson(transferData.data, Participant::class.java)
                                participantDao.insert(participant)
                                Timber.e("Participant ID: ${participant.conversationId} ${participant.userId}")
                                count++
                            }
                            TransferDataType.USER.value -> {
                                val user = gson.fromJson(transferData.data, User::class.java)
                                userDao.insert(user)
                                Timber.e("User ID: ${user.userId}")
                                count++
                            }
                            TransferDataType.CONVERSATION.value -> {
                                val conversation = gson.fromJson(transferData.data, Conversation::class.java)
                                // Only use Upsert
                                conversationDao.upsert(conversation)
                                Timber.e("Conversation ID: ${conversation.conversationId}")
                                count++
                            }
                            TransferDataType.SNAPSHOT.value -> {
                                val snapshot = gson.fromJson(transferData.data, Snapshot::class.java)
                                snapshotDao.insert(snapshot)
                                Timber.e("Snapshot ID: ${snapshot.snapshotId}")
                                count++
                            }
                            TransferDataType.STICKER.value -> {
                                val sticker = gson.fromJson(transferData.data, Sticker::class.java)
                                stickerDao.insert(sticker)
                                Timber.e("Sticker ID: ${sticker.stickerId}")
                                count++
                            }
                            TransferDataType.ASSET.value -> {
                                val asset = gson.fromJson(transferData.data, Asset::class.java)
                                assetDao.insert(asset)
                                Timber.e("Asset ID: ${asset.assetId}")
                                count++
                            }
                            TransferDataType.PIN_MESSAGE.value -> {
                                val pinMessage = gson.fromJson(transferData.data, PinMessage::class.java)
                                pinMessageDao.insert(pinMessage)
                                Timber.e("PinMessage ID: ${pinMessage.messageId}")
                                count++
                            }
                            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                                val transcriptMessage = gson.fromJson(transferData.data, TranscriptMessage::class.java)
                                transcriptMessageDao.insert(transcriptMessage)
                                Timber.e("Transcript ID: ${transcriptMessage.messageId}")
                                count++
                            }
                            else -> {
                                Timber.e("No support $content")
                            }
                        }
                    }
                } while (!quit)
            } catch (e: SocketException) {
                // Todo
                Timber.e(e)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun exit() {
        quit = true
        inputStream.close()
        outputStream.close()
        socket.close()
    }
}

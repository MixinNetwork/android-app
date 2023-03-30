package one.mixin.android.ui.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
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
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User
import timber.log.Timber
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException

class TransferClient(private val finishListener: (String) -> Unit) {

    private lateinit var socket: Socket
    private var quit = false
    private var startTime = System.currentTimeMillis()
    private val db by lazy {
        MixinDatabase.getDatabase(MixinApplication.appContext)
    }

    private val userDao by lazy {
        db.userDao()
    }

    private val snapshotDao by lazy {
        db.snapshotDao()
    }

    private val conversationDao by lazy {
        db.conversationDao()
    }

    private val messageDao by lazy {
        db.messageDao()
    }

    private val participantDao by lazy {
        db.participantDao()
    }

    private val assData by lazy {
        db.assetDao()
    }

    private val stickerDao by lazy {
        db.stickerDao()
    }

    private val assetDao by lazy {
        db.assetDao()
    }

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

    fun connectToServer(ip: String, port: Int,commandData: TransferCommandData) {
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
                        finishListener("Finish Synchronized $count messages, ${System.currentTimeMillis() - startTime} ms")
                        exit()
                    } else if(content.startsWith("file")){
                        // do noting
                    } else {
                        Timber.e("sync $content")
                        val transferData = gson.fromJson(content, TransferData::class.java)

                        when (transferData.type) {
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

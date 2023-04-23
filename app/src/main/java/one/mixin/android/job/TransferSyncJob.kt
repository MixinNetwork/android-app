package one.mixin.android.job

import android.app.ActivityManager
import androidx.core.content.getSystemService
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import one.mixin.android.MixinApplication
import one.mixin.android.extension.createAtToLong
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferMessage
import one.mixin.android.ui.transfer.vo.TransferMessageMention
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.toMessage
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
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

class TransferSyncJob(private val filePath: String) :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "transfer_sync"
    }

    override fun onRun(): Unit = runBlocking {
        Timber.e("run $filePath")
        try {
            val file = File(filePath)
            val activityManager = MixinApplication.appContext.getSystemService<ActivityManager>()
            val runtime = Runtime.getRuntime()
            val messageList = mutableListOf<Message>()
            if (file.exists() && file.length() > 0) {
                file.inputStream().use { input ->
                    while (input.available() > 0) {
                        while (activityManager != null && isMemoryLow(activityManager, runtime)) {
                            delay(1000)
                        }
                        val sizeData = ByteArray(4)
                        input.read(sizeData)
                        val data = ByteArray(byteArrayToInt(sizeData))
                        input.read(data)
                        val content = String(data, UTF_8)
                        processJson(content, messageList)
                    }
                }
            }
            if (messageList.isNotEmpty()) {
                messageDao.insertList(messageList)
            }
            file.delete()
            Timber.e("delete $filePath")
        } catch (e: Exception) {
            Timber.e("skip $filePath ${e.message}")
        }
    }

    private fun processJson(content: String, messageList: MutableList<Message>) {
        val transferData = serializationJson.decodeFromString<TransferSendData<JsonElement>>(content)
        when (transferData.type) {
            TransferDataType.CONVERSATION.value -> {
                val conversation = serializationJson.decodeFromJsonElement<Conversation>(transferData.data)
                conversationDao.insertIgnore(conversation)
                Timber.e("Conversation ID: ${conversation.conversationId}")
            }

            TransferDataType.PARTICIPANT.value -> {
                val participant = serializationJson.decodeFromJsonElement<Participant>(transferData.data)
                participantDao.insertIgnore(participant)
                Timber.e("Participant ID: ${participant.conversationId} ${participant.userId}")
            }

            TransferDataType.USER.value -> {
                val user = serializationJson.decodeFromJsonElement<User>(transferData.data)
                userDao.insertIgnore(user)
                Timber.e("User ID: ${user.userId}")
            }

            TransferDataType.APP.value -> {
                Timber.e("$content ${transferData.data}")
                val app = serializationJson.decodeFromJsonElement<App>(transferData.data)
                appDao.insertIgnore(app)
                Timber.e("App ID: ${app.appId}")
            }

            TransferDataType.ASSET.value -> {
                val asset = serializationJson.decodeFromJsonElement<Asset>(transferData.data)
                assetDao.insertIgnore(asset)
                Timber.e("Asset ID: ${asset.assetId}")
            }

            TransferDataType.SNAPSHOT.value -> {
                val snapshot = serializationJson.decodeFromJsonElement<Snapshot>(transferData.data)
                snapshotDao.insertIgnore(snapshot)
                Timber.e("Snapshot ID: ${snapshot.snapshotId}")
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
                Timber.e("Sticker ID: ${sticker.stickerId}")
            }

            TransferDataType.PIN_MESSAGE.value -> {
                val pinMessage = serializationJson.decodeFromJsonElement<PinMessage>(transferData.data)
                pinMessageDao.insertIgnore(pinMessage)
                Timber.e("PinMessage ID: ${pinMessage.messageId}")
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                val transcriptMessage = serializationJson.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                transcriptMessageDao.insertIgnore(transcriptMessage)
                Timber.e("Transcript ID: ${transcriptMessage.messageId}")
            }

            TransferDataType.MESSAGE.value -> {
                val message = serializationJson.decodeFromJsonElement<TransferMessage>(transferData.data)
                if (messageDao.findMessageIdById(message.messageId) == null) {
                    processMessage(message.toMessage(), messageList)
                }
                Timber.e("Message ID: ${message.messageId}")
            }

            TransferDataType.MESSAGE_MENTION.value -> {
                val messageMention =
                    serializationJson.decodeFromJsonElement<TransferMessageMention>(transferData.data).let {
                        val mention = it.mentions
                        if (mention != null) {
                            MessageMention(it.messageId, it.conversationId, mention, it.hasRead)
                        } else {
                            val messageContent =
                                messageDao.findMessageContentById(it.conversationId, it.messageId)
                                    ?: return
                            val mentionData = parseMentionData(messageContent, userDao) ?: return
                            MessageMention(it.messageId, it.conversationId, mentionData, it.hasRead)
                        }
                    }
                val rowId = messageMentionDao.insertIgnoreReturn(messageMention)
                Timber.e("MessageMention ID: $rowId ${messageMention.messageId}")
            }

            TransferDataType.EXPIRED_MESSAGE.name -> {
                val expiredMessage =
                    serializationJson.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                expiredMessageDao.insertIgnore(expiredMessage)
                Timber.e("ExpiredMessage ID: ${expiredMessage.messageId}")
            }

            else -> {
                Timber.e("No support $content")
            }
        }
    }

    private fun processMessage(message: Message, list: MutableList<Message>) {
        list.add(message)
        ftsDatabase.insertOrReplaceMessageFts4(message)
        if (list.size == 100) {
            messageDao.insertList(list)
            list.clear()
        }
        Timber.e("Message ID: ${message.messageId}")
    }

    private fun byteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        for (i in byteArray.indices) {
            result = result shl 8
            result = result or (byteArray[i].toInt() and 0xff)
        }
        return result
    }

    /**
     * Check if the memory is low and the app may be killed.
     *
     * @param activityManager The ActivityManager object to get memory info.
     * @param runtime The Runtime object to get app memory usage info.
     * @return true if memory is low and the app may be killed, false otherwise.
     */
    fun isMemoryLow(activityManager: ActivityManager, runtime: Runtime): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemory = memoryInfo.availMem
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        // Calculate memory utilization.
        val memoryUtilization = usedMemory.toFloat() / availableMemory.toFloat()

        // Check if memory is low and the app may be killed.
        val isLowMemory = memoryUtilization > 0.7f && usedMemory > (maxMemory * 0.8f)
        if (isLowMemory) {
            Timber.e("Available memory: $availableMemory")
            Timber.e("Used memory: $usedMemory")
            Timber.e("Max memory: $maxMemory")
            Timber.e("Memory utilization: $memoryUtilization")
        }

        return isLowMemory
    }
}

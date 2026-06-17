package one.mixin.android.ui.transfer

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import one.mixin.android.Constants
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
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.di.ApplicationScope
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.extension.copy
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.moveTo
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.job.AsyncProcessTransferAttachmentFileJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.transfer.status.TransferStatus
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.CURRENT_TRANSFER_VERSION
import one.mixin.android.ui.transfer.vo.TransferCommand
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferData
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.compatible.TransferMessageMention
import one.mixin.android.util.NetworkUtils
import one.mixin.android.util.SINGLE_SOCKET_THREAD
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.util.reportException
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
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.min
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@ExperimentalSerializationApi
class TransferClient
    @Inject
    internal constructor(
        val context: Application,
        val appDatabase: MixinDatabase,
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
        @ApplicationScope
        private val applicationScope: CoroutineScope,
        private val jobManager: MixinJobManager,
    ) {
        lateinit var protocol: TransferProtocol
        lateinit var aesKey: SecretKeySpec

        companion object {
            private const val MAX_FILE_SIZE = 5242880 // 5M
        }

        private var socket: Socket? = null
        private var quit = false
        private var receiveCount = 0L
        private var processCount = 0L
        private var startTime = 0L
        private var currentType: String? = null
            set(value) {
                if (field != value) {
                    field = value
                    Timber.e("Current type: $field")
                }
            }

        private var deviceId: String? = null

        private val syncChannel = Channel<ByteArray>()

        private val transferInserter by lazy {
            TransferInserter(appDatabase)
        }

        private val singleTransferThread by lazy {
            Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_TRANSFER_THREAD") }.asCoroutineDispatcher()
        }

        private val singleTransferFileThread by lazy {
            Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_TRANSFER_FILE_THREAD") }.asCoroutineDispatcher()
        }

        suspend fun connectToServer(
            ip: String,
            port: Int,
            commandData: TransferCommand,
            secretBytes: ByteArray,
        ) =
            withContext(SINGLE_SOCKET_THREAD) {
                try {
                    protocol = TransferProtocol(serializationJson, secretBytes)
                    aesKey = SecretKeySpec(secretBytes.sliceArray(0..31), "AES")
                    NetworkUtils.printWifiInfo(MixinApplication.appContext)
                    status.value = TransferStatus.CONNECTING
                    val socket = Socket(ip, port)
                    this@TransferClient.socket = socket
                    status.value = TransferStatus.WAITING_FOR_VERIFICATION
                    // send connect command
                    sendCommand(socket.outputStream, commandData)
                    launch(Dispatchers.IO) { listen(socket.inputStream, socket.outputStream) }
                    launch(Dispatchers.IO) {
                        for (byteArray in syncChannel) {
                            writeBytes(byteArray)
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

        private suspend fun listen(
            inputStream: InputStream,
            outputStream: OutputStream,
        ) {
            do {
                val result =
                    try {
                        protocol.read(inputStream)
                    } catch (e: EOFException) {
                        if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.PROCESSING && status.value != TransferStatus.ERROR) {
                            status.value = TransferStatus.ERROR
                            exit() // If it is not finished, exit.
                        }
                        quit = true
                        Timber.e(e)
                        NetworkUtils.printWifiInfo(MixinApplication.appContext)
                        null
                    } catch (e: Exception) {
                        // Report exception and stop transfer
                        quit = true
                        if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.PROCESSING && status.value != TransferStatus.ERROR) {
                            status.value = TransferStatus.ERROR
                            exit()
                        }
                        Timber.e(e)
                        reportException(e)
                        null
                    }
                status.value = TransferStatus.SYNCING
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
                                this.deviceId = result.deviceId
                                protocol.setCachePath(getAttachmentPath())
                                Timber.e("Transfer start:$startTime total:$total")
                            }

                            TransferCommandAction.PUSH.value, TransferCommandAction.PULL.value -> {
                                Timber.e("action ${result.action}")
                            }

                            TransferCommandAction.FINISH.value -> {
                                status.value = TransferStatus.PROCESSING
                                sendCommand(outputStream, TransferCommand(TransferCommandAction.FINISH.value))
                                delay(100)
                                exit(true)
                                Timber.e("It takes a total of ${System.currentTimeMillis() - startTime} milliseconds to synchronize $receiveCount / ${this.total} data")
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
            val progress = min((receiveCount++) / total.toFloat() * 100, 100f)
            if (lastProgress != progress && System.currentTimeMillis() - lastTime > 300) {
                sendCommand(outputStream, TransferCommand(TransferCommandAction.PROGRESS.value, progress = progress))
                lastProgress = progress
                lastTime = System.currentTimeMillis()
                RxBus.publish(DeviceTransferProgressEvent(progress))
                Timber.e("Device receive $progress")
            }
        }

        private fun processProgress() {
            processCount++
            if (total <= 0 || status.value != TransferStatus.PROCESSING) return
            val processProgress = min(processCount * 100f / receiveCount, 100f)
            if (System.currentTimeMillis() - lastTime > 300) {
                lastTime = System.currentTimeMillis()
                RxBus.publish(DeviceTransferProgressEvent(processProgress))
                Timber.e("Device process $processProgress $processCount/$receiveCount")
            }
        }

        private val mutableList: MutableList<Message> = mutableListOf()

        private fun decrypt(ciphertext: ByteArray): ByteArray {
            val iv = ciphertext.sliceArray(0..15)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
            return cipher.doFinal(ciphertext.sliceArray(16 until ciphertext.size))
        }

        private fun processJson(byteArray: ByteArray) {
            val byteArrayInputStream = ByteArrayInputStream(byteArray)
            val transferData = serializationJson.decodeFromStream<TransferData<JsonElement>>(byteArrayInputStream)
            currentType = transferData.type
            when (transferData.type) {
                TransferDataType.CONVERSATION.value -> {
                    val conversation = serializationJson.decodeFromJsonElement<Conversation>(transferData.data)
                    transferInserter.insertIgnore(conversation)
                    conversationExtDao.deleteConversationById(conversation.conversationId)
                }

                TransferDataType.PARTICIPANT.value -> {
                    val participant = serializationJson.decodeFromJsonElement<Participant>(transferData.data)
                    transferInserter.insertIgnore(participant)
                }

                TransferDataType.USER.value -> {
                    val user = serializationJson.decodeFromJsonElement<User>(transferData.data)
                    transferInserter.insertIgnore(user)
                }

                TransferDataType.APP.value -> {
                    val app = serializationJson.decodeFromJsonElement<App>(transferData.data)
                    transferInserter.insertIgnore(app)
                }

                TransferDataType.ASSET.value -> {
                    val asset = serializationJson.decodeFromJsonElement<Asset>(transferData.data)
                    transferInserter.insertIgnore(asset)
                }

                TransferDataType.TOKEN.value -> {
                    val token = serializationJson.decodeFromJsonElement<Token>(transferData.data)
                    transferInserter.insertIgnore(token)
                }

                TransferDataType.SNAPSHOT.value -> {
                    val snapshot = serializationJson.decodeFromJsonElement<Snapshot>(transferData.data)
                    transferInserter.insertIgnore(snapshot)
                }

                TransferDataType.SAFE_SNAPSHOT.value -> {
                    val safeSnapshot = serializationJson.decodeFromJsonElement<SafeSnapshot>(transferData.data)
                    transferInserter.insertIgnore(safeSnapshot)
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
                    transferInserter.insertIgnore(sticker)
                }

                TransferDataType.PIN_MESSAGE.value -> {
                    val pinMessage = serializationJson.decodeFromJsonElement<PinMessage>(transferData.data)
                    transferInserter.insertIgnore(pinMessage)
                }

                TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                    val transcriptMessage = serializationJson.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                    transferInserter.insertIgnore(transcriptMessage)
                }

                TransferDataType.MESSAGE.value -> {
                    val message = serializationJson.decodeFromJsonElement<Message>(transferData.data)
                    if (messageDao.findMessageIdById(message.messageId) == null) {
                        mutableList.add(message)
                        if (mutableList.size >= 1000) {
                            transferInserter.insertMessages(mutableList)
                            mutableList.clear()
                        }
                        ftsDatabase.insertOrReplaceMessageFts4(message)
                    }
                }

                TransferDataType.MESSAGE_MENTION.value -> {
                    if (mutableList.isNotEmpty()) {
                        transferInserter.insertMessages(mutableList)
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
                    transferInserter.insertIgnore(messageMention)
                }

                TransferDataType.EXPIRED_MESSAGE.value -> {
                    if (mutableList.isNotEmpty()) {
                        transferInserter.insertMessages(mutableList)
                        mutableList.clear()
                    }
                    val expiredMessage =
                        serializationJson.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                    transferInserter.insertIgnore(expiredMessage)
                }

                else -> {
                    Timber.e("No support ${transferData.type}")
                }
            }
            processProgress()
            byteArrayInputStream.close()
        }

        private var isFinal = false

        private fun finalWork() {
            synchronized(this) {
                if (isFinal) return
                transferInserter.insertMessages(mutableList)
                currentOutputStream?.close()
                if (status.value != TransferStatus.ERROR) {
                    currentFile?.let { file ->
                        processDataFile(file)
                    }
                    if (mutableList.isNotEmpty()) {
                        transferInserter.insertMessages(mutableList)
                        mutableList.clear()
                    }
                    // Final db work
                    conversationDao.getAllConversationId().forEach { conversationId ->
                        conversationDao.refreshLastMessageId(conversationId)
                        remoteMessageStatusDao.updateConversationUnseen(conversationId)
                    }
                    conversationExtDao.getAllConversationId().forEach { conversationId ->
                        conversationExtDao.refreshCountByConversationId(conversationId)
                    }
                    processAttachmentFile(getAttachmentPath())
                } else {
                    jobManager.addJobInBackground(AsyncProcessTransferAttachmentFileJob(getAttachmentPath()))
                }
                isFinal = true
            }
        }

        fun exit(finished: Boolean = false) =
            applicationScope.launch(SINGLE_SOCKET_THREAD) {
                synchronized(this) {
                    try {
                        if (socket == null) return@launch
                        launch(SINGLE_SOCKET_THREAD) {
                            socket?.close()
                            socket = null
                        }
                        quit = true
                        if (!finished) {
                            Timber.e("DeviceId: $deviceId type: $currentType id: ${transferInserter.primaryId} start-time:$startTime current-time:${System.currentTimeMillis()}")
                            NetworkUtils.printWifiInfo(MixinApplication.appContext)
                        } else {
                            launch {
                                PropertyHelper.deleteKeyValue(Constants.Account.PREF_TRANSFER_SCENE)
                            }
                            Timber.e("Finish exit ${(System.currentTimeMillis() - startTime) / 1000}s")
                        }
                        launch(singleTransferThread) {
                            finalWork()
                        }
                    } catch (e: Exception) {
                        Timber.e("Exit client ${e.message}")
                    }
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

        private val cachePath by lazy {
            File("${(context.externalCacheDir ?: context.cacheDir).absolutePath}${File.separator}$deviceId").apply {
                this.mkdirs()
            }
        }

        private fun getAttachmentPath(): File {
            return File("${cachePath.absolutePath}${File.separator}attachment").also {
                it.mkdirs()
            }
        }

        private var index: Int = 0

        private fun getCacheFile(index: Int): File {
            cachePath.mkdirs()
            return File(cachePath, "$index.cache")
        }

        private var currentFile: File? = null
        private var currentOutputStream: OutputStream? = null
        private var lastName = ""

        private suspend fun writeBytes(bytes: ByteArray) =
            withContext(singleTransferFileThread) {
                val file =
                    currentFile ?: getCacheFile(++index).also {
                        currentOutputStream?.close()
                        currentFile = it
                        currentOutputStream = it.outputStream()
                    }
                if (file.length() + bytes.size > MAX_FILE_SIZE) {
                    applicationScope.launch(singleTransferThread) {
                        processDataFile(file)
                    }
                    currentFile = null
                    currentOutputStream?.close()
                    currentOutputStream = null
                } else {
                    if (lastName != file.name) {
                        Timber.e("Write bytes: ${file.name}")
                        lastName = file.name
                    }
                    currentOutputStream?.write(bytes)
                }
            }

        private fun processDataFile(file: File) { // Read files, parse data
            synchronized(this) {
                try {
                    if (status.value == TransferStatus.ERROR) return
                    Timber.e("Process file ${file.absolutePath} ${Thread.currentThread().name}")
                    if (file.exists() && file.length() > 0) {
                        file.inputStream().use { input ->
                            while (input.available() > 0) {
                                val sizeData = ByteArray(4)
                                input.read(sizeData)
                                val data = ByteArray(byteArrayToInt(sizeData))
                                input.read(data)
                                processJson(decrypt(data))
                            }
                        }
                    }
                    file.delete()
                    Timber.e("Delete ${file.absolutePath}")
                } catch (e: Exception) {
                    Timber.e("Skip ${file.absolutePath} ${e.message}")
                }
            }
        }

        private fun processAttachmentFile(folder: File) { // Processing attachment files
            folder.walkTopDown().forEach { f ->
                if (status.value == TransferStatus.ERROR) return
                if (f.isFile && f.length() > 0) {
                    val messageId = f.name
                    if (messageId.isUUID()) {
                        processProgress()
                        val transferMessage = transcriptMessageDao.findAttachmentMessage(messageId)
                        transferMessage?.mediaUrl?.let { mediaUrl ->
                            val dir = context.getTranscriptDirPath()
                            if (!dir.exists()) {
                                dir.mkdirs()
                            }
                            f.copy(File(dir, mediaUrl))
                        }
                        val message = messageDao.findAttachmentMessage(messageId)
                        if (message?.mediaUrl != null) {
                            val extensionName = message.mediaUrl.getExtensionName()
                            val outFile =
                                if (message.isImage()) {
                                    context.getImagePath().createImageTemp(
                                        message.conversationId,
                                        message.messageId,
                                        extensionName?.run { ".$extensionName" } ?: ".jpg",
                                    )
                                } else if (message.isAudio()) {
                                    context.getAudioPath().createAudioTemp(
                                        message.conversationId,
                                        message.messageId,
                                        extensionName ?: "ogg",
                                    )
                                } else if (message.isVideo()) {
                                    context.getVideoPath().createVideoTemp(
                                        message.conversationId,
                                        message.messageId,
                                        extensionName ?: "mp4",
                                    )
                                } else {
                                    context.getDocumentPath().createDocumentTemp(
                                        message.conversationId,
                                        message.messageId,
                                        extensionName ?: "",
                                    )
                                }
                            f.moveTo(outFile)
                        } else {
                            // Unable to Mapping to data, delete file
                            f.delete()
                        }
                    }
                }
            }
            folder.deleteRecursively()
            if (status.value != TransferStatus.ERROR) {
                status.value = TransferStatus.FINISHED
            }
        }

        private fun byteArrayToInt(byteArray: ByteArray): Int {
            var result = 0
            for (i in byteArray.indices) {
                result = result shl 8
                result = result or (byteArray[i].toInt() and 0xff)
            }
            return result
        }

        private fun intToByteArray(intValue: Int): ByteArray {
            val byteBuffer = ByteBuffer.allocate(4)
            byteBuffer.putInt(intValue)
            return byteBuffer.array()
        }
    }

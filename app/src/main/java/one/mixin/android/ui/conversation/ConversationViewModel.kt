package one.mixin.android.ui.conversation

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.FIXED_LOAD_SIZE
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.MixinApplication
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.DisappearRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.deserialize
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putString
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.ConvertVideoJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshStickerAlbumJob
import one.mixin.android.job.RefreshStickerAndRelatedAlbumJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.RemoveStickersJob
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendGiphyJob
import one.mixin.android.job.SendMessageJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.ui.common.message.SendMessageHelper
import one.mixin.android.util.Attachment
import one.mixin.android.util.ControlledRunner
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.SINGLE_DRAFT_THREAD
import one.mixin.android.util.chat.FastComputableLiveData
import one.mixin.android.util.chat.FastLivePagedListBuilder
import one.mixin.android.vo.AppCap
import one.mixin.android.vo.AppItem
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.PinMessageData
import one.mixin.android.vo.PinMessageMinimal
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.vo.StickerAlbumOrder
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.encryptedCategory
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.generateForwardMessage
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.giphy.Image
import one.mixin.android.vo.isEncrypted
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isVideo
import one.mixin.android.webrtc.SelectItem
import one.mixin.android.websocket.AudioMessagePayload
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.PinAction
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.websocket.VideoMessagePayload
import one.mixin.android.widget.gallery.MimeType
import java.io.File
import java.util.UUID
import javax.inject.Inject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@HiltViewModel
class ConversationViewModel
@Inject
internal constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val jobManager: MixinJobManager,
    private val assetRepository: AssetRepository,
    private val accountRepository: AccountRepository,
    private val messenger: SendMessageHelper,
    private val cleanMessageHelper: CleanMessageHelper
) : ViewModel() {

    fun getMessages(conversationId: String, firstKeyToLoad: Int = 0): FastComputableLiveData<PagedList<MessageItem>> {
        val pagedListConfig = PagedList.Config.Builder()
            .setPrefetchDistance(PAGE_SIZE * 2)
            .setPageSize(PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build()

        return FastLivePagedListBuilder(
            conversationRepository.getMessages(
                conversationId,
                if (firstKeyToLoad > FIXED_LOAD_SIZE) {
                    // Multiple Page Size, round up
                    ((firstKeyToLoad + PAGE_SIZE - 1) / PAGE_SIZE) * PAGE_SIZE
                } else {
                    FIXED_LOAD_SIZE
                }
            ),
            pagedListConfig
        ).setInitialLoadKey(firstKeyToLoad).build()
    }

    suspend fun indexUnread(conversationId: String) =
        conversationRepository.indexUnread(conversationId) ?: 0

    suspend fun findFirstUnreadMessageId(conversationId: String, offset: Int): String? =
        conversationRepository.findFirstUnreadMessageId(conversationId, offset)

    suspend fun getConversationDraftById(id: String) = withContext(SINGLE_DRAFT_THREAD) {
        conversationRepository.getConversationDraftById(id)
    }

    fun getConversationById(id: String) = conversationRepository.getConversationById(id)

    suspend fun getConversation(id: String) = withContext(Dispatchers.IO) {
        conversationRepository.getConversation(id)
    }

    fun findUserById(conversationId: String): LiveData<User> =
        userRepository.findUserById(conversationId)

    fun sendTextMessage(conversationId: String, sender: User, content: String, encryptCategory: EncryptCategory, isSilent: Boolean? = null) {
        messenger.sendTextMessage(viewModelScope, conversationId, sender, content, encryptCategory, isSilent)
    }

    // todo
    fun sendTranscriptMessage(conversationId: String, messageId: String?, sender: User, transcriptMessages: List<TranscriptMessage>, encryptCategory: EncryptCategory) {
        messenger.sendTranscriptMessage(messageId ?: UUID.randomUUID().toString(), conversationId, sender, transcriptMessages, encryptCategory)
    }

    fun sendPostMessage(conversationId: String, sender: User, content: String, encryptCategory: EncryptCategory) {
        messenger.sendPostMessage(conversationId, sender, content, encryptCategory)
    }

    fun sendAppCardMessage(conversationId: String, sender: User, content: String) {
        messenger.sendAppCardMessage(conversationId, sender, content)
    }

    fun sendReplyTextMessage(
        conversationId: String,
        sender: User,
        content: String,
        replyMessage: MessageItem,
        encryptCategory: EncryptCategory,
        isSilentMessage: Boolean? = null
    ) {
        messenger.sendReplyTextMessage(conversationId, sender, content, replyMessage, encryptCategory, isSilentMessage)
    }

    fun sendAttachmentMessage(conversationId: String, sender: User, attachment: Attachment, encryptCategory: EncryptCategory, replyMessage: MessageItem? = null) {
        messenger.sendAttachmentMessage(conversationId, sender, attachment, encryptCategory, replyMessage)
    }

    fun sendAudioMessage(
        conversationId: String,
        messageId: String,
        sender: User,
        file: File,
        duration: Long,
        waveForm: ByteArray,
        encryptCategory: EncryptCategory,
        replyMessage: MessageItem? = null
    ) {
        messenger.sendAudioMessage(conversationId, messageId, sender, file, duration, waveForm, encryptCategory, replyMessage)
    }

    suspend fun sendAudioMessage(
        conversationId: String,
        sender: User,
        audioMessagePayload: AudioMessagePayload,
        encryptCategory: EncryptCategory,
        replyMessage: MessageItem? = null
    ) {
        val messageId = audioMessagePayload.messageId
        val duration = audioMessagePayload.duration
        val waveForm = audioMessagePayload.waveForm
        withContext(Dispatchers.IO) {
            val inputStream =
                MixinApplication.appContext.contentResolver.openInputStream(audioMessagePayload.url.toUri())
                    ?: return@withContext
            val audioFile = MixinApplication.get().getAudioPath()
                .createAudioTemp(conversationId, audioMessagePayload.messageId, "ogg")
            audioFile.copyFromInputStream(inputStream)
            messenger.sendAudioMessage(conversationId, messageId, sender, audioFile, duration, waveForm, encryptCategory, replyMessage)
        }
    }

    fun sendStickerMessage(
        conversationId: String,
        sender: User,
        transferStickerData: StickerMessagePayload,
        encryptCategory: EncryptCategory
    ) {
        messenger.sendStickerMessage(conversationId, sender, transferStickerData, encryptCategory)
    }

    fun sendContactMessage(conversationId: String, sender: User, shareUserId: String, encryptCategory: EncryptCategory, replyMessage: MessageItem? = null) {
        viewModelScope.launch {
            val user = userRepository.suspendFindUserById(shareUserId)
            messenger.sendContactMessage(conversationId, sender, shareUserId, user?.fullName, encryptCategory, replyMessage)
        }
    }

    fun sendVideoMessage(
        conversationId: String,
        senderId: String,
        uri: Uri,
        encryptCategory: EncryptCategory,
        messageId: String? = null,
        createdAt: String? = null,
        replyMessage: MessageItem? = null
    ) {
        messenger.sendVideoMessage(conversationId, senderId, uri, encryptCategory, messageId, createdAt, replyMessage)
    }

    fun sendVideoMessage(
        conversationId: String,
        senderId: String,
        videoMessagePayload: VideoMessagePayload,
        encryptCategory: EncryptCategory,
        replyMessage: MessageItem? = null
    ) {
        val uri = videoMessagePayload.url.toUri()
        val messageId = videoMessagePayload.messageId
        val createdAt = videoMessagePayload.createdAt
        messenger.sendVideoMessage(conversationId, senderId, uri, encryptCategory, messageId, createdAt, replyMessage)
    }

    fun sendRecallMessage(conversationId: String, sender: User, list: List<MessageItem>) {
        messenger.sendRecallMessage(conversationId, sender, list)
    }

    suspend fun sendPinMessage(
        conversationId: String,
        sender: User,
        action: PinAction,
        list: Collection<PinMessageData>
    ) {
        if (list.isEmpty()) return
        withContext(Dispatchers.IO) {
            if (action == PinAction.PIN) {
                conversationRepository.insertPinMessages(
                    list.map {
                        PinMessage(
                            it.messageId, it.conversationId,
                            nowInUtc()
                        )
                    }
                )
            } else if (action == PinAction.UNPIN) {
                conversationRepository.deletePinMessageByIds(list.map { it.messageId })
            }
            messenger.sendPinMessage(
                conversationId, sender, action,
                list.map {
                    PinMessageMinimal(it.messageId, it.type, it.content)
                }
            )
        }
    }

    fun sendLiveMessage(
        conversationId: String,
        sender: User,
        transferLiveData: LiveMessagePayload,
        encryptCategory: EncryptCategory
    ) {
        messenger.sendLiveMessage(conversationId, sender, transferLiveData, encryptCategory)
    }

    fun sendGiphyMessage(
        conversationId: String,
        senderId: String,
        image: Image,
        encryptCategory: EncryptCategory,
        previewUrl: String
    ) {
        messenger.sendGiphyMessage(conversationId, senderId, image, encryptCategory, previewUrl)
    }

    fun sendLocationMessage(conversationId: String, senderId: String, location: LocationPayload, encryptCategory: EncryptCategory) {
        messenger.sendLocationMessage(conversationId, senderId, location, encryptCategory)
    }

    fun sendImageMessage(
        conversationId: String,
        sender: User,
        uri: Uri,
        encryptCategory: EncryptCategory,
        mime: String? = null,
        replyMessage: MessageItem? = null,
    ): Int {
        return messenger.sendImageMessage(conversationId, sender, uri, encryptCategory, mime, replyMessage)
    }

    fun updateRelationship(request: RelationshipRequest) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request))
    }

    fun observeParticipantsCount(conversationId: String) =
        conversationRepository.observeParticipantsCount(conversationId)

    fun findParticipantById(conversationId: String, userId: String): Participant? =
        conversationRepository.findParticipantById(conversationId, userId)

    fun initConversation(conversationId: String, recipient: User, sender: User) {
        val createdAt = nowInUtc()
        val conversation = createConversation(
            conversationId,
            ConversationCategory.CONTACT.name,
            recipient.userId,
            ConversationStatus.START.ordinal
        )
        val participants = arrayListOf(
            Participant(conversationId, sender.userId, "", createdAt),
            Participant(conversationId, recipient.userId, "", createdAt)
        )
        conversationRepository.syncInsertConversation(conversation, participants)
    }

    fun getUserById(userId: String) =
        Observable.just(userId).subscribeOn(Schedulers.io())
            .map { userRepository.getUserById(it) }.observeOn(AndroidSchedulers.mainThread())!!

    fun cancel(id: String, conversationId: String) = viewModelScope.launch(Dispatchers.IO) {
        jobManager.cancelJobByMixinJobId(id) {
            viewModelScope.launch {
                conversationRepository.updateMediaStatusSuspend(MediaStatus.CANCELED.name, id, conversationId)
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun retryUpload(id: String, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(id)?.let {
                if (it.isVideo() && it.mediaSize != null && it.mediaSize == 0L) {
                    try {
                        conversationRepository.updateMediaStatus(MediaStatus.PENDING.name, it.id, it.conversationId)
                        jobManager.addJobInBackground(
                            ConvertVideoJob(
                                it.conversationId,
                                it.userId,
                                Uri.parse(it.mediaUrl),
                                when {
                                    it.isSignal() -> EncryptCategory.SIGNAL
                                    it.isEncrypted() -> EncryptCategory.ENCRYPTED
                                    else -> EncryptCategory.PLAIN
                                },
                                it.id,
                                it.createdAt
                            )
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else if (it.isImage() && it.mediaMimeType == MimeType.GIF.toString() && it.mediaUrl?.startsWith("http") == true) { // un-downloaded GIPHY
                    val category = when {
                        it.isSignal() -> MessageCategory.SIGNAL_IMAGE
                        it.isEncrypted() -> MessageCategory.ENCRYPTED_IMAGE
                        else -> MessageCategory.PLAIN_IMAGE
                    }.name
                    try {
                        jobManager.addJobInBackground(
                            SendGiphyJob(
                                it.conversationId, it.userId, it.mediaUrl, it.mediaWidth!!, it.mediaHeight!!,
                                it.mediaSize ?: 0L, category, it.id, it.thumbImage ?: "", it.createdAt
                            )
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else {
                    conversationRepository.updateMediaStatus(MediaStatus.PENDING.name, it.id, it.conversationId)
                    jobManager.addJobInBackground(SendAttachmentMessageJob(it))
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun retryDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(id)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }
    }

    fun markMessageRead(conversationId: String, isBubbled: Boolean) {
        if (isBubbled.not()) {
            notificationManager.cancel(conversationId.hashCode())
        }
        MixinApplication.appScope.launch {
            conversationRepository.markMessageRead(conversationId)
        }
    }

    suspend fun getFriends(): List<User> = userRepository.getFriends()

    suspend fun findFriendsNotBot() = userRepository.findFriendsNotBot()

    suspend fun successConversationList() = conversationRepository.successConversationList()

    fun findContactUsers() = userRepository.findContactUsers()

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun deleteMessages(list: List<MessageItem>) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            cleanMessageHelper.deleteMessageItems(list)
        }
    }

    fun observeSystemAddedAlbums() = accountRepository.observeSystemAddedAlbums()

    fun observeSystemAlbums() = accountRepository.observeSystemAlbums()

    fun observeSystemAlbumsAndStickers() = accountRepository.observeSystemAlbumsAndStickers()

    suspend fun getPersonalAlbums() = accountRepository.getPersonalAlbums()

    fun observeStickers(id: String) = accountRepository.observeStickers(id)

    fun observeSystemStickersByAlbumId(id: String) = accountRepository.observeSystemStickersByAlbumId(id)

    suspend fun findStickersByAlbumId(albumId: String) = accountRepository.findStickersByAlbumId(albumId)

    suspend fun findStickerById(stickerId: String) = accountRepository.findStickerById(stickerId)

    fun observeStickerById(stickerId: String) = accountRepository.observeStickerById(stickerId)

    suspend fun findAlbumById(albumId: String) = accountRepository.findAlbumById(albumId)

    suspend fun findStickerSystemAlbumId(stickerId: String) = accountRepository.findStickerSystemAlbumId(stickerId)

    fun observeAlbumById(albumId: String) = accountRepository.observeAlbumById(albumId)

    fun observeSystemAlbumById(albumId: String) = accountRepository.observeSystemAlbumById(albumId)

    fun refreshStickerAndRelatedAlbum(stickerId: String) {
        jobManager.addJobInBackground(RefreshStickerAndRelatedAlbumJob(stickerId))
    }

    suspend fun updateAlbumOrders(orders: List<StickerAlbumOrder>) = withContext(Dispatchers.IO) {
        accountRepository.updateAlbumOrders(orders)
    }

    suspend fun updateAlbumAdded(stickerAlbumAdded: StickerAlbumAdded) = accountRepository.updateAlbumAdded(stickerAlbumAdded)

    suspend fun findMaxOrder() = accountRepository.findMaxOrder()

    fun observePersonalStickers() = accountRepository.observePersonalStickers()

    fun recentStickers() = accountRepository.recentUsedStickers()

    fun updateStickerUsedAt(stickerId: String) {
        viewModelScope.launch {
            accountRepository.updateUsedAt(stickerId, System.currentTimeMillis().toString())
        }
    }

    fun getBottomApps(conversationId: String, guestId: String?): LiveData<List<AppItem>>? {
        return if (guestId == null) {
            Transformations.map(
                conversationRepository.getGroupAppsByConversationId(conversationId)
            ) { list ->
                list.filter {
                    it.capabilities?.contains(AppCap.GROUP.name) == true
                }
            }
        } else {
            val accountId = Session.getAccountId() ?: return null
            conversationRepository.getFavoriteAppsByUserId(guestId, accountId)
        }
    }

    suspend fun findAppById(id: String) = userRepository.findAppById(id)

    fun assetItemsWithBalance(): LiveData<List<AssetItem>> =
        assetRepository.assetItemsWithBalance()

    fun addStickerAsync(stickerAddRequest: StickerAddRequest) =
        accountRepository.addStickerAsync(stickerAddRequest)

    fun addStickerLocal(sticker: Sticker, albumId: String) =
        accountRepository.addStickerLocal(sticker, albumId)

    fun removeStickers(ids: List<String>) {
        jobManager.addJobInBackground(RemoveStickersJob(ids))
    }

    fun refreshStickerAlbums() {
        jobManager.addJobInBackground(RefreshStickerAlbumJob())
    }

    suspend fun findMessageIndex(conversationId: String, messageId: String) =
        conversationRepository.findMessageIndex(conversationId, messageId)

    private fun createReadSessionMessage(list: List<MessageMinimal>, conversationId: String) {
        Session.getExtensionSessionId()?.let {
            list.map {
                createAckJob(
                    CREATE_MESSAGE,
                    BlazeAckMessage(it.id, MessageStatus.READ.name),
                    conversationId
                )
            }.let {
                conversationRepository.insertList(it)
            }
        }
    }

    fun trendingGifs(limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.trendingGifs(limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun searchGifs(query: String, limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.searchGifs(query, limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun observeAddress(addressId: String) = assetRepository.observeAddress(addressId)

    fun updateRecentUsedBots(
        defaultSharedPreferences: SharedPreferences,
        userId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val botsString =
            defaultSharedPreferences.getString(Constants.Account.PREF_RECENT_USED_BOTS, null)
        if (botsString != null) {
            var botsList = botsString.split("=")
            if (botsList.size == 1 && !botsList[0].isUUID()) {
                getPreviousVersionBotsList(defaultSharedPreferences)?.let {
                    botsList = it
                }
            }
            if (botsList.isEmpty()) {
                defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_USED_BOTS, userId)
                return@launch
            }

            val arr = botsList.filter { it != userId }
                .toMutableList()
                .also {
                    if (it.size >= Constants.RECENT_USED_BOTS_MAX_COUNT) {
                        it.dropLast(1)
                    }
                    it.add(0, userId)
                }
            defaultSharedPreferences.putString(
                Constants.Account.PREF_RECENT_USED_BOTS,
                arr.joinToString("=")
            )
        } else {
            defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_USED_BOTS, userId)
        }
    }

    private fun getPreviousVersionBotsList(defaultSharedPreferences: SharedPreferences): List<String>? {
        defaultSharedPreferences.getString(
            Constants.Account.PREF_RECENT_USED_BOTS,
            null
        )?.let { botsString ->
            return botsString.deserialize<Array<String>>()?.toList()
        } ?: return null
    }

    suspend fun findLastMessage(conversationId: String) =
        conversationRepository.findLastMessage(conversationId)

    suspend fun findUnreadMessageByMessageId(
        conversationId: String,
        userId: String,
        messageId: String
    ) = conversationRepository.findUnreadMessageByMessageId(conversationId, userId, messageId)

    suspend fun isSilence(conversationId: String, userId: String) =
        conversationRepository.isSilence(conversationId, userId) == 0

    fun refreshUser(userId: String, forceRefresh: Boolean) {
        jobManager.addJobInBackground(RefreshUserJob(listOf(userId), forceRefresh = forceRefresh))
    }

    fun downloadAttachment(message: Message) {
        jobManager.addJobInBackground(AttachmentDownloadJob(message))
    }

    suspend fun suspendFindUserById(userId: String) = withContext(Dispatchers.IO) {
        userRepository.suspendFindUserById(userId)
    }

    suspend fun getSortMessagesByIds(messages: Set<MessageItem>): ArrayList<ForwardMessage> {
        return withContext(Dispatchers.IO) {
            val list = ArrayList<ForwardMessage>()
            val sortMessages = conversationRepository.getSortMessagesByIds(messages.map { it.messageId })
            for (m in sortMessages) {
                val forwardMessage = generateForwardMessage(m)
                forwardMessage?.let { fm -> list.add(fm) }
            }
            return@withContext list
        }
    }

    suspend fun getAnnouncementByConversationId(conversationId: String) = conversationRepository.getAnnouncementByConversationId(conversationId)

    private val searchControlledRunner = ControlledRunner<List<User>>()

    suspend fun fuzzySearchUser(conversationId: String, keyword: String?): List<User> {
        return withContext(Dispatchers.IO) {
            searchControlledRunner.cancelPreviousThenRun {
                if (keyword.isNullOrEmpty()) {
                    userRepository.suspendGetGroupParticipants(conversationId)
                } else {
                    userRepository.fuzzySearchGroupUser(conversationId, keyword)
                }
            }
        }
    }

    suspend fun findUserByIdentityNumberSuspend(identityNumber: String): User? {
        return userRepository.findUserByIdentityNumberSuspend(identityNumber)
            ?: handleMixinResponse(
                invokeNetwork = {
                    userRepository.searchSuspend(identityNumber)
                },
                successBlock = { response ->
                    response.data?.let {
                        withContext(Dispatchers.IO) {
                            userRepository.insertUser(it)
                        }
                    }
                    return@handleMixinResponse response.data
                }
            )
    }

    suspend fun fuzzySearchBotGroupUser(conversationId: String, keyword: String?): List<User> {
        return withContext(Dispatchers.IO) {
            searchControlledRunner.cancelPreviousThenRun {
                if (keyword.isNullOrEmpty() || keyword.isEmpty()) {
                    userRepository.getFriends()
                } else {
                    userRepository.fuzzySearchBotGroupUser(
                        conversationId,
                        keyword
                    )
                }
            }
        }
    }

    fun getUnreadMentionMessageByConversationId(conversationId: String) = conversationRepository.getUnreadMentionMessageByConversationId(conversationId)

    suspend fun markMentionRead(messageId: String, conversationId: String) {
        conversationRepository.markMentionRead(messageId, conversationId)
    }

    suspend fun findLatestTrace(opponentId: String?, destination: String?, tag: String?, amount: String, assetId: String) =
        assetRepository.findLatestTrace(opponentId, destination, tag, amount, assetId)

    suspend fun checkData(selectItem: SelectItem, callback: suspend (String, EncryptCategory) -> Unit) {
        withContext(Dispatchers.IO) {
            if (selectItem.conversationId != null) {
                val conversation = conversationRepository.getConversation(selectItem.conversationId)
                if (conversation != null) {
                    if (conversation.isGroupConversation()) {
                        withContext(Dispatchers.Main) {
                            callback(conversation.conversationId, EncryptCategory.SIGNAL)
                        }
                    } else {
                        userRepository.findContactByConversationId(selectItem.conversationId)?.let { user ->
                            withContext(Dispatchers.Main) {
                                callback(conversation.conversationId, user.encryptedCategory())
                            }
                        }
                    }
                }
            } else if (selectItem.userId != null) {
                userRepository.findForwardUserById(selectItem.userId)?.let { user ->
                    val conversation = conversationRepository.findContactConversationByOwnerId(user.userId)
                    if (conversation == null) {
                        val createdAt = nowInUtc()
                        val conversationId = generateConversationId(Session.getAccountId()!!, user.userId)
                        val participants = arrayListOf(
                            Participant(conversationId, Session.getAccountId()!!, "", createdAt),
                            Participant(conversationId, user.userId, "", createdAt)
                        )
                        conversationRepository.syncInsertConversation(
                            createConversation(
                                conversationId,
                                ConversationCategory.CONTACT.name,
                                user.userId,
                                ConversationStatus.START.ordinal
                            ),
                            participants
                        )
                        withContext(Dispatchers.Main) {
                            callback(conversationId, user.encryptedCategory())
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            callback(conversation.conversationId, user.encryptedCategory())
                        }
                    }
                }
            }
        }
    }

    suspend fun findMessageById(messageId: String) = conversationRepository.suspendFindMessageById(messageId)

    fun sendMessage(message: Message) {
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    suspend fun processTranscript(transcriptMessages: List<TranscriptMessage>): List<TranscriptMessage> {
        withContext(Dispatchers.IO) {
            transcriptMessages.forEach { transcript ->
                if (transcript.quoteContent != null) {
                    val quoteMessage = try {
                        GsonHelper.customGson.fromJson(transcript.quoteContent, QuoteMessageItem::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    transcript.quoteContent = GsonHelper.customGson.toJson(quoteMessage)
                }
            }
        }
        return transcriptMessages
    }

    suspend fun getTranscripts(transcriptId: String, messageId: String? = null): List<TranscriptMessage> =
        withContext(Dispatchers.IO) {
            val transcripts = conversationRepository.getTranscriptsById(transcriptId)
            if (messageId != null) {
                transcripts.forEach { t -> t.transcriptId = messageId }
            }
            return@withContext transcripts
        }

    fun getLastPinMessages(conversationId: String) =
        conversationRepository.getLastPinMessages(conversationId)

    fun countPinMessages(conversationId: String) =
        conversationRepository.countPinMessages(conversationId)

    suspend fun findPinMessageById(messageId: String) = withContext(Dispatchers.IO) {
        conversationRepository.findPinMessageById(messageId)
    }

    suspend fun createConversation(conversationId: String, userId: String) = withContext(Dispatchers.IO) {
        val conversation = conversationRepository.getConversation(conversationId)
        if (conversation == null) {
            val request = ConversationRequest(
                conversationId = conversationId,
                category = ConversationCategory.CONTACT.name,
                participants = listOf(ParticipantRequest(userId, ""))
            )
            val response = conversationRepository.createSuspend(request)
            if (response.isSuccess) {
                val data = response.data
                if (data != null) {
                    conversationRepository.insertOrUpdateConversation(data)
                    return@withContext true
                }
            }
        }
        return@withContext false
    }

    suspend fun disappear(conversationId: String, duration: Long) =
        conversationRepository.disappear(conversationId, DisappearRequest(duration))

    suspend fun updateConversationExpireIn(conversationId: String, expireIn: Long?) =
        conversationRepository.updateConversationExpireIn(conversationId, expireIn)
}

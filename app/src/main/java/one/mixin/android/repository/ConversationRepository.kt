package one.mixin.android.repository

import android.annotation.SuppressLint
import android.os.CancellationSignal
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.Constants.DB_DELETE_THRESHOLD
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.ConversationCircleRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.AppDao
import one.mixin.android.db.CircleConversationDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.MessageProvider
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.batchMarkReadAndTake
import one.mixin.android.db.deleteMessage
import one.mixin.android.db.deleteMessageByConversationId
import one.mixin.android.db.insertNoReplace
import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.joinStar
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.replaceQuotationMark
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.job.AttachmentDeleteJob
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MessageDeleteJob
import one.mixin.android.job.MessageFtsDeleteJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.TranscriptDeleteJob
import one.mixin.android.session.Session
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.MessageMentionStatus
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.createAckJob
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository
@Inject
internal constructor(
    private val appDatabase: MixinDatabase,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val circleConversationDao: CircleConversationDao,
    private val participantDao: ParticipantDao,
    private val messageMentionDao: MessageMentionDao,
    private val participantSessionDao: ParticipantSessionDao,
    private val appDao: AppDao,
    private val jobDao: JobDao,
    private val transcriptMessageDao: TranscriptMessageDao,
    private val pinMessageDao: PinMessageDao,
    private val conversationService: ConversationService,
    private val userService: UserService,
    private val jobManager: MixinJobManager
) {

    @SuppressLint("RestrictedApi")
    fun getMessages(conversationId: String, unreadCount: Int, countable: Boolean) =
        MessageProvider.getMessages(conversationId, appDatabase, unreadCount, countable)

    suspend fun getChatMessages(conversationId: String, offset: Int, limit: Int): List<MessageItem> = messageDao.getChatMessages(conversationId, offset, limit)

    fun conversations(circleId: String?): DataSource.Factory<Int, ConversationItem> = if (circleId == null) {
        MessageProvider.getConversations(appDatabase)
    } else {
        MessageProvider.observeConversationsByCircleId(circleId, appDatabase)
    }

    suspend fun successConversationList(): List<ConversationMinimal> = conversationDao.successConversationList()

    suspend fun insertConversation(conversation: Conversation, participants: List<Participant>) =
        withContext(SINGLE_DB_THREAD) {
            appDatabase.runInTransaction {
                conversationDao.insert(conversation)
                participantDao.insertList(participants)
            }
        }

    fun syncInsertConversation(conversation: Conversation, participants: List<Participant>) {
        appDatabase.runInTransaction {
            conversationDao.insert(conversation)
            participantDao.insertList(participants)
        }
    }

    fun getConversationById(conversationId: String): LiveData<Conversation> =
        conversationDao.getConversationById(conversationId)

    fun findConversationById(conversationId: String): Observable<Conversation> =
        Observable.just(conversationId).map {
            conversationDao.findConversationById(conversationId)
        }

    fun searchConversationById(conversationId: String) =
        conversationDao.searchConversationById(conversationId)

    fun findMessageById(messageId: String) = messageDao.findMessageById(messageId)

    suspend fun suspendFindMessageById(messageId: String) = messageDao.suspendFindMessageById(messageId)

    suspend fun saveDraft(conversationId: String, draft: String) =
        conversationDao.saveDraft(conversationId, draft)

    fun getConversation(conversationId: String) =
        conversationDao.findConversationById(conversationId)

    suspend fun fuzzySearchMessage(query: String, limit: Int, cancellationSignal: CancellationSignal): List<SearchMessageItem> =
        MessageProvider.fuzzySearchMessage(query.joinStar().replaceQuotationMark(), limit, appDatabase, cancellationSignal)

    fun fuzzySearchMessageDetail(query: String, conversationId: String, cancellationSignal: CancellationSignal) =
        MessageProvider.fuzzySearchMessageDetail(query.joinStar().replaceQuotationMark(), conversationId, appDatabase, cancellationSignal)

    suspend fun fuzzySearchChat(query: String, cancellationSignal: CancellationSignal): List<ChatMinimal> =
        MessageProvider.fuzzySearchChat(query, appDatabase, cancellationSignal)

    suspend fun indexUnread(conversationId: String) =
        conversationDao.indexUnread(conversationId)

    suspend fun conversationZeroClear(conversationId: String) = conversationDao.conversationZeroClear(conversationId)

    suspend fun indexMediaMessages(
        conversationId: String,
        messageId: String,
        excludeLive: Boolean
    ): Int = if (excludeLive) {
        messageDao.indexMediaMessagesExcludeLive(conversationId, messageId)
    } else {
        messageDao.indexMediaMessages(conversationId, messageId)
    }

    fun getMediaMessages(
        conversationId: String,
        index: Int,
        excludeLive: Boolean
    ): LiveData<PagedList<MessageItem>> {
        val dataSource = if (excludeLive) {
            messageDao.getMediaMessagesExcludeLive(conversationId)
        } else {
            messageDao.getMediaMessages(conversationId)
        }
        val config = PagedList.Config.Builder()
            .setPrefetchDistance(MediaPagerActivity.PAGE_SIZE)
            .setPageSize(MediaPagerActivity.PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build()
        return LivePagedListBuilder(dataSource, config)
            .setInitialLoadKey(index)
            .build()
    }

    suspend fun getMediaMessage(conversationId: String, messageId: String) =
        messageDao.getMediaMessage(conversationId, messageId)

    suspend fun getConversationIdIfExistsSync(recipientId: String) =
        conversationDao.getConversationIdIfExistsSync(recipientId)

    fun getUnreadMessage(conversationId: String, accountId: String, limit: Int): List<MessageMinimal> {
        return messageDao.getUnreadMessage(conversationId, accountId, limit)
    }

    suspend fun updateCodeUrl(conversationId: String, codeUrl: String) =
        conversationDao.updateCodeUrl(conversationId, codeUrl)

    fun getGroupParticipants(conversationId: String) =
        participantDao.getParticipants(conversationId)

    suspend fun getParticipantsWithoutBot(conversationId: String) =
        participantDao.getParticipantsWithoutBot(conversationId)

    fun observeGroupParticipants(conversationId: String) =
        participantDao.observeGroupParticipants(conversationId)

    fun fuzzySearchGroupParticipants(conversationId: String, username: String, identityNumber: String) =
        participantDao.fuzzySearchGroupParticipants(conversationId, username, identityNumber)

    suspend fun updateMediaStatusSuspend(status: String, messageId: String) =
        messageDao.updateMediaStatusSuspend(status, messageId)

    suspend fun updateConversationPinTimeById(conversationId: String, circleId: String?, pinTime: String?) =
        withContext(SINGLE_DB_THREAD) {
            if (circleId == null) {
                conversationDao.updateConversationPinTimeById(conversationId, pinTime)
            } else {
                circleConversationDao.updateConversationPinTimeById(conversationId, circleId, pinTime)
            }
        }

    fun getGroupAppsByConversationId(conversationId: String) =
        appDao.getGroupAppsByConversationId(conversationId)

    fun getFavoriteAppsByUserId(guestId: String, masterId: String) =
        appDao.getGroupAppsByConversationId(guestId, masterId).map { list ->
            list.distinctBy { app ->
                app.appId
            }
        }

    suspend fun updateAnnouncement(conversationId: String, announcement: String) =
        conversationDao.updateConversationAnnouncement(conversationId, announcement)

    fun getLimitParticipants(conversationId: String, limit: Int) =
        participantDao.getLimitParticipants(conversationId, limit)

    fun findParticipantById(conversationId: String, userId: String) =
        participantDao.findParticipantById(conversationId, userId)

    suspend fun getParticipantsCount(conversationId: String) =
        participantDao.getParticipantsCount(conversationId)

    fun observeParticipantsCount(conversationId: String) =
        participantDao.observeParticipantsCount(conversationId)

    suspend fun getConversationStorageUsage(): List<ConversationStorageUsage> = conversationDao.getConversationStorageUsage()

    fun getMediaByConversationIdAndCategory(conversationId: String, signalCategory: String, plainCategory: String, encryptedCategory: String) =
        messageDao.getMediaByConversationIdAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory)

    suspend fun findMessageIndex(conversationId: String, messageId: String) =
        messageDao.findMessageIndex(conversationId, messageId)

    fun findUnreadMessagesSync(conversationId: String, accountId: String) =
        messageDao.findUnreadMessagesSync(conversationId, accountId)

    suspend fun batchMarkReadAndTake(conversationId: String, userId: String, rowId: String) {
        messageDao.batchMarkReadAndTake(conversationId, userId, rowId)
    }

    fun findContactConversationByOwnerId(ownerId: String): Conversation? {
        return conversationDao.findContactConversationByOwnerId(ownerId)
    }

    fun insertList(it: List<Job>) {
        jobDao.insertList(it)
    }

    fun refreshConversation(conversationId: String): Boolean {
        try {
            val call = conversationService.getConversation(conversationId).execute()
            val response = call.body()
            if (response != null && response.isSuccess) {
                response.data?.let { conversationData ->
                    insertOrUpdateConversation(conversationData)
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun insertOrUpdateConversation(data: ConversationResponse) {
        var ownerId: String = data.creatorId
        if (data.category == ConversationCategory.CONTACT.name) {
            ownerId = data.participants.find { it.userId != Session.getAccountId() }!!.userId
        }
        var c = conversationDao.findConversationById(data.conversationId)
        if (c == null) {
            val builder = ConversationBuilder(data.conversationId, data.createdAt, ConversationStatus.SUCCESS.ordinal)
            c = builder.setOwnerId(ownerId)
                .setCategory(data.category)
                .setName(data.name)
                .setIconUrl(data.iconUrl)
                .setAnnouncement(data.announcement)
                .setMuteUntil(data.muteUntil)
                .setCodeUrl(data.codeUrl).build()
            conversationDao.insert(c)
            if (!c.announcement.isNullOrBlank()) {
                RxBus.publish(GroupEvent(data.conversationId))
                MixinApplication.appContext.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(data.conversationId, true)
            }
        } else {
            val status = if (data.participants.find { Session.getAccountId() == it.userId } != null) {
                ConversationStatus.SUCCESS.ordinal
            } else {
                ConversationStatus.QUIT.ordinal
            }
            conversationDao.updateConversation(
                data.conversationId,
                ownerId,
                data.category,
                data.name,
                data.announcement,
                data.muteUntil,
                data.createdAt,
                status
            )
            if (data.announcement.isNotBlank() && c.announcement != data.announcement) {
                RxBus.publish(GroupEvent(data.conversationId))
                MixinApplication.appContext.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(data.conversationId, true)
            }
        }
    }

    suspend fun findFirstUnreadMessageId(conversationId: String, offset: Int): String? =
        messageDao.findFirstUnreadMessageId(conversationId, offset)

    suspend fun findLastMessage(conversationId: String) = messageDao.findLastMessage(conversationId)

    suspend fun findUnreadMessageByMessageId(
        conversationId: String,
        userId: String,
        messageId: String
    ) = messageDao.findUnreadMessageByMessageId(conversationId, userId, messageId)

    suspend fun isSilence(conversationId: String, userId: String): Int =
        messageDao.isSilence(conversationId, userId) ?: 0

    suspend fun findNextAudioMessage(conversationId: String, createdAt: String, messageId: String) =
        messageDao.findNextAudioMessage(conversationId, createdAt, messageId)

    fun getMediaMessagesExcludeLive(conversationId: String) =
        messageDao.getMediaMessagesExcludeLive(conversationId)

    fun getAudioMessages(conversationId: String) = messageDao.getAudioMessages(conversationId)

    fun getPostMessages(conversationId: String) = messageDao.getPostMessages(conversationId)

    fun getLinkMessages(conversationId: String) = messageDao.getLinkMessages(conversationId)

    fun getFileMessages(conversationId: String) = messageDao.getFileMessages(conversationId)

    suspend fun getSortMessagesByIds(ids: List<String>) = messageDao.getSortMessagesByIds(ids)

    suspend fun getAllParticipants() = participantDao.getAllParticipants()

    suspend fun insertParticipantSession(ps: List<ParticipantSession>) =
        participantSessionDao.insertListSuspend(ps)

    suspend fun getAnnouncementByConversationId(conversationId: String) = conversationDao.getAnnouncementByConversationId(conversationId)

    fun getUnreadMentionMessageByConversationId(conversationId: String) = messageMentionDao.getUnreadMentionMessageByConversationId(conversationId)

    suspend fun markMentionRead(messageId: String, conversationId: String) {
        messageMentionDao.suspendMarkMentionRead(messageId)
        withContext(Dispatchers.IO) {
            jobDao.insertNoReplace(createAckJob(CREATE_MESSAGE, BlazeAckMessage(messageId, MessageMentionStatus.MENTION_READ.name), conversationId))
        }
    }

    suspend fun updateCircles(conversationId: String?, userId: String?, requests: List<ConversationCircleRequest>): MixinResponse<List<CircleConversation>> {
        return if (userId != null) {
            userService.updateCircles(userId, requests)
        } else {
            conversationService.updateCircles(conversationId!!, requests)
        }
    }

    fun observeAllConversationUnread() = conversationDao.observeAllConversationUnread()

    suspend fun muteSuspend(id: String, request: ConversationRequest): MixinResponse<ConversationResponse> = conversationService.muteSuspend(id, request)

    fun updateGroupMuteUntil(conversationId: String, muteUntil: String) = conversationDao.updateGroupMuteUntil(conversationId, muteUntil)

    fun updateMediaStatus(status: String, id: String) = messageDao.updateMediaStatus(status, id)

    suspend fun getConversationNameById(cid: String) = conversationDao.getConversationNameById(cid)

    // DELETE
    fun deleteMediaMessageByConversationAndCategory(conversationId: String, signalCategory: String, plainCategory: String, encryptedCategory: String) {
        val count = messageDao.countDeleteMediaMessageByConversationAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory)
        repeat((count / DB_DELETE_LIMIT) + 1) {
            messageDao.deleteMediaMessageByConversationAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory, DB_DELETE_LIMIT)
        }
    }

    suspend fun deleteMessageByConversationId(conversationId: String, deleteConversation: Boolean = false) {
        messageDao.findAllMediaPathByConversationId(conversationId).let { list ->
            if (list.isNotEmpty()) {
                jobManager.addJobInBackground(AttachmentDeleteJob(* list.toTypedArray()))
            }
        }
        val deleteMentionCount = messageMentionDao.countDeleteMessageByConversationId(conversationId)
        if (deleteMentionCount > DB_DELETE_THRESHOLD) {
            jobManager.addJobInBackground(MessageDeleteJob(conversationId, true))
        } else {
            val deleteTimes = deleteMentionCount / DB_DELETE_LIMIT + 1
            repeat(deleteTimes) {
                messageMentionDao.deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
            }
        }
        val deleteCount = messageDao.countDeleteMessageByConversationId(conversationId)
        if (deleteCount > DB_DELETE_THRESHOLD) {
            jobManager.addJobInBackground(MessageDeleteJob(conversationId, deleteConversation = deleteConversation))
        } else {
            val deleteTimes = deleteCount / DB_DELETE_LIMIT + 1
            jobManager.addJobInBackground(
                MessageFtsDeleteJob(
                    messageDao.getMessageIdsByConversationId(
                        conversationId
                    )
                )
            )
            repeat(deleteTimes) {
                if (!deleteConversation) {
                    appDatabase.deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
                }
            }
            if (deleteConversation) {
                conversationDao.deleteConversationById(conversationId)
            }
        }
    }

    fun deleteMessage(id: String, mediaUrl: String? = null, forceDelete: Boolean = true) {
        if (!mediaUrl.isNullOrBlank() && forceDelete) {
            jobManager.addJobInBackground(AttachmentDeleteJob(mediaUrl))
        }
        appDatabase.deleteMessage(id)
    }

    fun deleteTranscriptByMessageId(messageId: String) {
        jobManager.addJobInBackground(TranscriptDeleteJob(listOf(messageId)))
    }

    suspend fun findTranscriptIdByConversationId(conversationId: String) = messageDao.findTranscriptIdByConversationId(conversationId)

    suspend fun deleteConversationById(conversationId: String) {
        deleteMessageByConversationId(conversationId, true)
    }

    fun create(request: ConversationRequest) = conversationService.create(request)

    fun participants(id: String, action: String, requests: List<ParticipantRequest>) =
        conversationService.participants(id, action, requests)

    fun findTranscriptMessageItemById(transcriptId: String) = transcriptMessageDao.getTranscriptMessages(transcriptId)

    fun getPinMessages(conversationId: String) = pinMessageDao.getPinMessages(conversationId)

    suspend fun findTranscriptMessageIndex(transcriptId: String, messageId: String) = transcriptMessageDao.findTranscriptMessageIndex(transcriptId, messageId)

    suspend fun findPinMessageIndex(transcriptId: String, messageId: String) = pinMessageDao.findPinMessageIndex(transcriptId, messageId)

    suspend fun getTranscriptMediaMessage(transcriptId: String) = withContext(Dispatchers.IO) {
        transcriptMessageDao.getTranscriptMediaMessage(transcriptId)
    }

    suspend fun indexTranscriptMediaMessages(transcriptId: String, messageId: String) = withContext(Dispatchers.IO) {
        transcriptMessageDao.indexTranscriptMediaMessages(transcriptId, messageId)
    }

    suspend fun getTranscriptsById(transcriptId: String) = transcriptMessageDao.getTranscriptsById(transcriptId)

    suspend fun getTranscriptById(transcriptId: String, messageId: String) = transcriptMessageDao.getTranscriptById(transcriptId, messageId)

    fun updateTranscriptMediaStatus(transcriptId: String, messageId: String, status: String) = transcriptMessageDao.updateMediaStatus(transcriptId, messageId, status)

    fun getMediaSizeTotalById(conversationId: String) = transcriptMessageDao.getMediaSizeTotalById(conversationId)

    fun countTranscriptById(conversationId: String) = transcriptMessageDao.countTranscriptByConversationId(conversationId)

    suspend fun hasUploadedAttachmentSuspend(transcriptId: String) = transcriptMessageDao.hasUploadedAttachmentSuspend(transcriptId)

    fun refreshConversationById(conversationId: String) = conversationDao.refreshConversationById(conversationId)

    suspend fun getAndSyncConversation(conversationId: String): Conversation? = withContext(Dispatchers.IO) {
        val conversation = conversationDao.getConversationByIdSuspend(conversationId)
        val localData = participantDao.getRealParticipants(conversationId)
        if (conversation != null) return@withContext conversation

        return@withContext handleMixinResponse(
            invokeNetwork = {
                conversationService.findConversationSuspend(conversationId)
            },
            switchContext = Dispatchers.IO,
            successBlock = { response ->
                response.data?.let { data ->
                    val participants = mutableListOf<Participant>()
                    val conversationUserIds = mutableListOf<String>()
                    if (!data.participants.any { p -> p.userId == Session.getAccountId() }) return@handleMixinResponse null
                    insertOrUpdateConversation(data)
                    for (p in data.participants) {
                        val item = Participant(conversationId, p.userId, p.role, p.createdAt!!)
                        if (p.role == ParticipantRole.OWNER.name) {
                            participants.add(0, item)
                        } else {
                            participants.add(item)
                        }
                        conversationUserIds.add(p.userId)
                    }

                    participantDao.replaceAll(data.conversationId, participants)
                    data.participantSessions?.let {
                        syncParticipantSession(conversationId, it)
                    }

                    if (conversationUserIds.isNotEmpty()) {
                        jobManager.addJobInBackground(
                            RefreshUserJob(
                                conversationUserIds,
                                conversationId
                            )
                        )
                    }
                    if (participants.size != localData.size || conversationUserIds.isNotEmpty()) {
                        jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                    }
                    return@handleMixinResponse conversationDao.findConversationById(conversationId)
                }
            }
        )
    }

    private fun syncParticipantSession(conversationId: String, data: List<UserSession>) {
        participantSessionDao.deleteByStatus(conversationId)
        val remote = data.map {
            ParticipantSession(conversationId, it.userId, it.sessionId, publicKey = it.publicKey)
        }
        if (remote.isEmpty()) {
            participantSessionDao.deleteByConversationId(conversationId)
            return
        }
        val local = participantSessionDao.getParticipantSessionsByConversationId(conversationId)
        if (local.isEmpty()) {
            participantSessionDao.insertList(remote)
            return
        }
        val common = remote.intersect(local)
        val remove = local.minus(common)
        val add = remote.minus(common)
        if (remove.isNotEmpty()) {
            participantSessionDao.deleteList(remove)
        }
        if (add.isNotEmpty()) {
            participantSessionDao.insertList(add)
        }
    }

    fun getLastPinMessages(conversationId: String) =
        pinMessageDao.getLastPinMessages(conversationId)

    fun countPinMessages(conversationId: String) =
        pinMessageDao.countPinMessages(conversationId)

    fun insertPinMessages(pinMessages: List<PinMessage>) {
        pinMessages.forEach { message ->
            pinMessageDao.insert(message)
        }
    }

    fun deletePinMessageByIds(messageIds: List<String>) {
        pinMessageDao.deleteByIds(messageIds)
    }

    fun insertMessage(message: Message) {
        messageDao.insert(message)
    }

    suspend fun findPinMessageById(messageId: String) = pinMessageDao.findPinMessageById(messageId)

    suspend fun getPinMessageMinimals(conversationId: String) = pinMessageDao.getPinMessageMinimals(conversationId)

    fun syncMention(messageId: String, pinMessageId: String) {
        messageMentionDao.findMessageMentionById(messageId)?.let { mention ->
            messageMentionDao.insert(
                MessageMention(
                    pinMessageId,
                    mention.conversationId,
                    mention.mentions,
                    true
                )
            )
        }
    }

    suspend fun findSameConversations(selfId: String, userId: String) = conversationDao.findSameConversations(selfId, userId)
}

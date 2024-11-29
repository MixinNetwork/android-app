package one.mixin.android.repository

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
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.ConversationCircleRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.DisappearRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.AppDao
import one.mixin.android.db.CircleConversationDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.joinStar
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.replaceQuotationMark
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.job.RefreshUserJob
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
import one.mixin.android.vo.GroupInfo
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository
    @Inject
    internal constructor(
        private val databaseProvider: DatabaseProvider,
        private val messageDao: MessageDao,
        private val conversationDao: ConversationDao,
        private val conversationExtDao: ConversationExtDao,
        private val circleConversationDao: CircleConversationDao,
        private val participantDao: ParticipantDao,
        private val messageMentionDao: MessageMentionDao,
        private val participantSessionDao: ParticipantSessionDao,
        private val appDao: AppDao,
        private val jobDao: JobDao,
        private val transcriptMessageDao: TranscriptMessageDao,
        private val pinMessageDao: PinMessageDao,
        private val remoteMessageStatusDao: RemoteMessageStatusDao,
        private val conversationService: ConversationService,
        private val userService: UserService,
        private val jobManager: MixinJobManager,
        private val ftsDbHelper: FtsDatabase,
    ) {
        suspend fun getChatMessages(
            conversationId: String,
            offset: Int,
            limit: Int,
        ): List<MessageItem> = messageDao.getChatMessages(conversationId, offset, limit)

        fun observeConversations(circleId: String?): DataSource.Factory<Int, ConversationItem> =
            if (circleId == null) {
                DataProvider.observeConversations(databaseProvider.getMixinDatabase())
            } else {
                DataProvider.observeConversationsByCircleId(circleId, databaseProvider.getMixinDatabase())
            }

        suspend fun successConversationList(): List<ConversationMinimal> =
            conversationDao.successConversationList()

        suspend fun insertConversation(
            conversation: Conversation,
            participants: List<Participant>,
        ) =
            withContext(SINGLE_DB_THREAD) {
                databaseProvider.getMixinDatabase().runInTransaction {
                    conversationDao.upsert(conversation)
                    participantDao.insertList(participants)
                }
            }

        fun syncInsertConversation(
            conversation: Conversation,
            participants: List<Participant>,
        ) {
            databaseProvider.getMixinDatabase().runInTransaction {
                conversationDao.upsert(conversation)
                participantDao.insertList(participants)
            }
        }

        fun getConversationInfoById(
            conversationId: String,
            userId: String,
        ): LiveData<GroupInfo?> =
            conversationDao.getConversationInfoById(conversationId, userId)

        fun getConversationById(conversationId: String): LiveData<Conversation> =
            conversationDao.getConversationById(conversationId)

        fun findConversationById(conversationId: String): Observable<Conversation> =
            Observable.just(conversationId).map {
                conversationDao.findConversationById(conversationId)
            }

        suspend fun getConversationDraftById(conversationId: String): String? =
            conversationDao.getConversationDraftById(conversationId)

        fun findMessageById(messageId: String) = messageDao.findMessageById(messageId)

        suspend fun suspendFindMessageById(messageId: String) = messageDao.suspendFindMessageById(messageId)

        fun getConversation(conversationId: String) =
            conversationDao.findConversationById(conversationId)

        suspend fun fuzzySearchMessage(
            query: String,
            limit: Int,
            cancellationSignal: CancellationSignal,
        ): List<SearchMessageItem> =
            if (query.isBlank()) {
                emptyList<SearchMessageItem>()
            } else {
                val queryString = query.joinStar().replaceQuotationMark()
                DataProvider.fuzzySearchMessage(ftsDbHelper, queryString, databaseProvider.getMixinDatabase(), cancellationSignal)
            }

        fun fuzzySearchMessageDetail(
            query: String,
            conversationId: String,
            cancellationSignal: CancellationSignal,
        ): DataSource.Factory<Int, SearchMessageDetailItem> {
            val queryString = query.joinStar().replaceQuotationMark()
            return DataProvider.fuzzySearchMessageDetail(ftsDbHelper, queryString, conversationId, databaseProvider.getMixinDatabase(), cancellationSignal)
        }

        suspend fun fuzzySearchChat(
            query: String,
            cancellationSignal: CancellationSignal,
        ): List<ChatMinimal> =
            DataProvider.fuzzySearchChat(query, databaseProvider.getMixinDatabase(), cancellationSignal)

        suspend fun indexUnread(conversationId: String) =
            conversationDao.indexUnread(conversationId)

        suspend fun indexMediaMessages(
            conversationId: String,
            messageId: String,
            excludeLive: Boolean,
        ): Int =
            if (excludeLive) {
                messageDao.indexMediaMessagesExcludeLive(conversationId, messageId)
            } else {
                messageDao.indexMediaMessages(conversationId, messageId)
            }

        fun getMediaMessages(
            conversationId: String,
            index: Int,
            excludeLive: Boolean,
        ): LiveData<PagedList<MessageItem>> {
            val dataSource =
                if (excludeLive) {
                    messageDao.getMediaMessagesExcludeLive(conversationId)
                } else {
                    messageDao.getMediaMessages(conversationId)
                }
            val config =
                PagedList.Config.Builder()
                    .setPrefetchDistance(MediaPagerActivity.PAGE_SIZE)
                    .setPageSize(MediaPagerActivity.PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build()
            return LivePagedListBuilder(dataSource, config)
                .setInitialLoadKey(index)
                .build()
        }

        suspend fun getMediaMessage(
            conversationId: String,
            messageId: String,
        ) =
            messageDao.getMediaMessage(conversationId, messageId)

        suspend fun getConversationIdIfExistsSync(recipientId: String) =
            conversationDao.getConversationIdIfExistsSync(recipientId)

        fun getUnreadMessage(
            conversationId: String,
            accountId: String,
            limit: Int,
        ): List<MessageMinimal> {
            return messageDao.getUnreadMessage(conversationId, accountId, limit)
        }

        suspend fun updateCodeUrl(
            conversationId: String,
            codeUrl: String,
        ) =
            conversationDao.updateCodeUrl(conversationId, codeUrl)

        fun getGroupParticipants(conversationId: String) =
            participantDao.getParticipants(conversationId)

        suspend fun getParticipantsWithoutBot(conversationId: String) =
            participantDao.getParticipantsWithoutBot(conversationId)

        fun observeGroupParticipants(conversationId: String) =
            participantDao.observeGroupParticipants(conversationId)

        fun fuzzySearchGroupParticipants(
            conversationId: String,
            username: String,
            identityNumber: String,
        ) =
            participantDao.fuzzySearchGroupParticipants(conversationId, username, identityNumber)

        suspend fun updateMediaStatusSuspend(
            status: String,
            messageId: String,
            conversationId: String,
        ) {
            messageDao.updateMediaStatusSuspend(status, messageId)
            MessageFlow.update(conversationId, messageId)
        }

        suspend fun updateConversationPinTimeById(
            conversationId: String,
            circleId: String?,
            pinTime: String?,
        ) {
            if (circleId == null) {
                conversationDao.updateConversationPinTimeById(conversationId, pinTime)
            } else {
                circleConversationDao.updateConversationPinTimeById(conversationId, circleId, pinTime)
            }
        }

        fun getGroupAppsByConversationId(conversationId: String) =
            appDao.getGroupAppsByConversationId(conversationId)

        fun getFavoriteAppsByUserId(
            guestId: String,
            masterId: String,
        ) =
            appDao.getGroupAppsByConversationId(guestId, masterId).map { list ->
                list.distinctBy { app ->
                    app.appId
                }
            }

        suspend fun updateAnnouncement(
            conversationId: String,
            announcement: String,
        ) =
            conversationDao.updateConversationAnnouncement(conversationId, announcement)

        suspend fun updateConversationExpireIn(
            conversationId: String,
            expireIn: Long?,
        ) =
            conversationDao.updateConversationExpireIn(conversationId, expireIn)

        fun refreshCountByConversationId(conversationId: String) = conversationExtDao.refreshCountByConversationId(conversationId)

        fun getLimitParticipants(
            conversationId: String,
            limit: Int,
        ) =
            participantDao.getLimitParticipants(conversationId, limit)

        fun findParticipantById(
            conversationId: String,
            userId: String,
        ) =
            participantDao.findParticipantById(conversationId, userId)

        suspend fun getParticipantsCount(conversationId: String) =
            participantDao.getParticipantsCount(conversationId)

        fun observeParticipantsCount(conversationId: String) =
            participantDao.observeParticipantsCount(conversationId)

        suspend fun getConversationStorageUsage(): List<ConversationStorageUsage> = conversationDao.getConversationStorageUsage()

        fun getMediaByConversationIdAndCategory(
            conversationId: String,
            signalCategory: String,
            plainCategory: String,
            encryptedCategory: String,
        ) =
            messageDao.getMediaByConversationIdAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory)

        suspend fun findMessageIndex(
            conversationId: String,
            messageId: String,
        ) =
            messageDao.findMessageIndex(conversationId, messageId)

        fun findUnreadMessagesSync(
            conversationId: String,
            accountId: String,
        ) =
            messageDao.findUnreadMessagesSync(conversationId, accountId)

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
                c =
                    builder.setOwnerId(ownerId)
                        .setCategory(data.category)
                        .setName(data.name)
                        .setIconUrl(data.iconUrl)
                        .setAnnouncement(data.announcement)
                        .setMuteUntil(data.muteUntil)
                        .setCodeUrl(data.codeUrl)
                        .setExpireIn(data.expireIn)
                        .build()
                conversationDao.upsert(c)
                if (!c.announcement.isNullOrBlank()) {
                    RxBus.publish(GroupEvent(data.conversationId))
                    MixinApplication.appContext.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(data.conversationId, true)
                }
            } else {
                val status =
                    if (data.participants.find { Session.getAccountId() == it.userId } != null) {
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
                    data.expireIn,
                    status,
                )
                if (data.announcement.isNotBlank() && c.announcement != data.announcement) {
                    RxBus.publish(GroupEvent(data.conversationId))
                    MixinApplication.appContext.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(data.conversationId, true)
                }
            }
        }

        suspend fun findFirstUnreadMessageId(
            conversationId: String,
            offset: Int,
        ): String? =
            messageDao.findFirstUnreadMessageId(conversationId, offset)

        suspend fun findLastMessage(conversationId: String) = messageDao.findLastMessage(conversationId)

        suspend fun findUnreadMessageByMessageId(
            conversationId: String,
            userId: String,
            messageId: String,
        ) = messageDao.findUnreadMessageByMessageId(conversationId, userId, messageId)

        suspend fun isSilence(
            conversationId: String,
            userId: String,
        ): Int =
            messageDao.isSilence(conversationId, userId) ?: 0

        suspend fun findNextAudioMessage(
            conversationId: String,
            createdAt: String,
            messageId: String,
        ) =
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

        suspend fun updateCircles(
            conversationId: String?,
            userId: String?,
            requests: List<ConversationCircleRequest>,
        ): MixinResponse<List<CircleConversation>> {
            return if (userId != null) {
                userService.updateCircles(userId, requests)
            } else {
                conversationService.updateCircles(conversationId!!, requests)
            }
        }

        fun observeAllConversationUnread() = conversationDao.observeAllConversationUnread()

        suspend fun muteSuspend(
            id: String,
            request: ConversationRequest,
        ): MixinResponse<ConversationResponse> = conversationService.muteSuspend(id, request)

        suspend fun updateGroupMuteUntil(
            conversationId: String,
            muteUntil: String,
        ) = conversationDao.updateGroupMuteUntilSuspend(conversationId, muteUntil)

        fun updateMediaStatus(
            status: String,
            id: String,
            conversationId: String,
        ) {
            messageDao.updateMediaStatus(status, id)
            MessageFlow.update(conversationId, id)
        }

        suspend fun getConversationNameById(cid: String) = conversationDao.getConversationNameById(cid)

        // DELETE
        fun deleteMediaMessageByConversationAndCategory(
            conversationId: String,
            signalCategory: String,
            plainCategory: String,
            encryptedCategory: String,
        ) {
            val count = messageDao.countDeleteMediaMessageByConversationAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory)
            repeat((count / DB_DELETE_LIMIT) + 1) {
                val ids = messageDao.findMediaMessageByConversationAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory, DB_DELETE_LIMIT)
                messageDao.deleteMessageById(ids)
                MessageFlow.delete(conversationId, ids)
            }
        }

        suspend fun findTranscriptIdByConversationId(conversationId: String) = messageDao.findTranscriptIdByConversationId(conversationId)

        fun create(request: ConversationRequest) = conversationService.create(request)

        suspend fun createSuspend(request: ConversationRequest) = conversationService.createSuspend(request)

        fun participants(
            id: String,
            action: String,
            requests: List<ParticipantRequest>,
        ) =
            conversationService.participants(id, action, requests)

        fun findTranscriptMessageItemById(transcriptId: String) = transcriptMessageDao.getTranscriptMessages(transcriptId)

        fun getPinMessages(
            conversationId: String,
            count: Int,
        ) = DataProvider.getPinMessages(databaseProvider.getMixinDatabase(), conversationId, count)

        suspend fun findTranscriptMessageIndex(
            transcriptId: String,
            messageId: String,
        ) = transcriptMessageDao.findTranscriptMessageIndex(transcriptId, messageId)

        suspend fun findPinMessageIndex(
            transcriptId: String,
            messageId: String,
        ) = pinMessageDao.findPinMessageIndex(transcriptId, messageId)

        suspend fun getTranscriptMediaMessage(transcriptId: String) =
            withContext(Dispatchers.IO) {
                transcriptMessageDao.getTranscriptMediaMessage(transcriptId)
            }

        suspend fun indexTranscriptMediaMessages(
            transcriptId: String,
            messageId: String,
        ) =
            withContext(Dispatchers.IO) {
                transcriptMessageDao.indexTranscriptMediaMessages(transcriptId, messageId)
            }

        suspend fun getTranscriptsById(transcriptId: String) = transcriptMessageDao.getTranscriptsById(transcriptId)

        suspend fun getTranscriptById(
            transcriptId: String,
            messageId: String,
        ) = transcriptMessageDao.getTranscriptById(transcriptId, messageId)

        fun updateTranscriptMediaStatus(
            transcriptId: String,
            messageId: String,
            status: String,
        ) = transcriptMessageDao.updateMediaStatus(transcriptId, messageId, status)

        fun getMediaSizeTotalById(conversationId: String) = transcriptMessageDao.getMediaSizeTotalById(conversationId)

        fun countTranscriptById(conversationId: String) = transcriptMessageDao.countTranscriptByConversationId(conversationId)

        suspend fun hasUploadedAttachmentSuspend(transcriptId: String) = transcriptMessageDao.hasUploadedAttachmentSuspend(transcriptId)

        fun refreshConversationById(conversationId: String) = remoteMessageStatusDao.updateConversationUnseen(conversationId)

        suspend fun getAndSyncConversation(conversationId: String): Conversation? =
            withContext(Dispatchers.IO) {
                val conversation = conversationDao.getConversationByIdSuspend(conversationId)
                val localData = participantDao.getRealParticipants(conversationId)
                if (conversation != null) return@withContext conversation

                return@withContext handleMixinResponse(
                    invokeNetwork = {
                        conversationService.findConversationSuspend(conversationId)
                    },
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
                                        conversationId,
                                    ),
                                )
                            }
                            if (participants.size != localData.size || conversationUserIds.isNotEmpty()) {
                                jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                            }
                            return@handleMixinResponse conversationDao.findConversationById(conversationId)
                        }
                    },
                )
            }

        private fun syncParticipantSession(
            conversationId: String,
            data: List<UserSession>,
        ) {
            participantSessionDao.deleteByStatus(conversationId)
            val remote =
                data.map {
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
            databaseProvider.getMixinDatabase().insertMessage(message)
            MessageFlow.insert(message.conversationId, message.messageId)
        }

        suspend fun findPinMessageById(messageId: String) = pinMessageDao.findPinMessageById(messageId)

        suspend fun getPinMessageMinimals(conversationId: String) = pinMessageDao.getPinMessageMinimals(conversationId)

        fun syncMention(
            messageId: String,
            pinMessageId: String,
        ) {
            messageMentionDao.findMessageMentionById(messageId)?.let { mention ->
                messageMentionDao.insert(
                    MessageMention(
                        pinMessageId,
                        mention.conversationId,
                        mention.mentions,
                        true,
                    ),
                )
            }
        }

        suspend fun findSameConversations(
            selfId: String,
            userId: String,
        ) = conversationDao.findSameConversations(selfId, userId)

        suspend fun disappear(
            conversationId: String,
            disappearRequest: DisappearRequest,
        ) = conversationService.disappear(conversationId, disappearRequest)

        suspend fun exists(messageId: String) = messageDao.exists(messageId)

        fun findAudiosByConversationId(conversationId: String): DataSource.Factory<Int, MessageItem> =
            messageDao.findAudiosByConversationId(conversationId)

        suspend fun indexAudioByConversationId(
            messageId: String,
            conversationId: String,
        ): Int =
            messageDao.indexAudioByConversationId(messageId, conversationId)
    }

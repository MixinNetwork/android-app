package one.mixin.android.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.CONVERSATION_PAGE_SIZE
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.CircleConversationPayload
import one.mixin.android.api.request.CircleConversationRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.db.withTransaction
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.ConversationJob.Companion.TYPE_CREATE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.TranscriptDeleteJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.CircleOrder
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel
    @Inject
    internal constructor(
        private val messageRepository: ConversationRepository,
        private val userRepository: UserRepository,
        private val conversationRepository: ConversationRepository,
        private val tokenRepository: TokenRepository,
        private val jobManager: MixinJobManager,
        private val cleanMessageHelper: CleanMessageHelper,
    ) : ViewModel() {
        fun observeConversations(circleId: String?): LiveData<PagedList<ConversationItem>> {
            return LivePagedListBuilder(
                messageRepository.observeConversations(circleId),
                PagedList.Config.Builder()
                    .setPrefetchDistance(CONVERSATION_PAGE_SIZE * 2)
                    .setPageSize(CONVERSATION_PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build(),
            ).build()
        }

        suspend fun createGroupConversation(conversationId: String) {
            val c = messageRepository.getConversation(conversationId)
            c?.let {
                val participants = messageRepository.getGroupParticipants(conversationId)
                val mutableList = mutableListOf<Participant>()
                val createAt = nowInUtc()
                participants.mapTo(mutableList) { Participant(conversationId, it.userId, "", createAt) }
                val conversation =
                    Conversation(
                        c.conversationId, c.ownerId, c.category, c.name, c.iconUrl,
                        c.announcement, null, c.payType, createAt, null, null,
                        null, 0, ConversationStatus.START.ordinal, null,
                    )
                messageRepository.insertConversation(conversation, mutableList)

                val participantRequestList = mutableListOf<ParticipantRequest>()
                mutableList.mapTo(participantRequestList) { ParticipantRequest(it.userId, it.role) }
                val request =
                    ConversationRequest(
                        conversationId,
                        it.category!!,
                        it.name,
                        it.iconUrl,
                        it.announcement,
                        participantRequestList,
                    )
                jobManager.addJobInBackground(ConversationJob(request, type = TYPE_CREATE))
            }
        }

        suspend fun deleteConversation(conversationId: String) =
            withContext(Dispatchers.IO) {
                val ids = messageRepository.findTranscriptIdByConversationId(conversationId)
                if (ids.isNotEmpty()) {
                    jobManager.addJobInBackground(TranscriptDeleteJob(ids))
                }
                cleanMessageHelper.deleteMessageByConversationId(conversationId, true)
            }

        suspend fun updateConversationPinTimeById(
            conversationId: String,
            circleId: String?,
            pinTime: String?,
        ) = messageRepository.updateConversationPinTimeById(conversationId, circleId, pinTime)

        suspend fun mute(
            duration: Long,
            conversationId: String? = null,
            senderId: String? = null,
            recipientId: String? = null,
        ): MixinResponse<ConversationResponse> {
            require(conversationId != null || (senderId != null && recipientId != null)) {
                "error data"
            }
            if (conversationId != null) {
                val request = ConversationRequest(conversationId, ConversationCategory.GROUP.name, duration = duration)
                return conversationRepository.muteSuspend(conversationId, request)
            } else {
                var cid = messageRepository.getConversationIdIfExistsSync(recipientId!!)
                if (cid == null) {
                    cid = generateConversationId(senderId!!, recipientId)
                }
                val participantRequest = ParticipantRequest(recipientId, "")
                val request =
                    ConversationRequest(
                        cid,
                        ConversationCategory.CONTACT.name,
                        duration = duration,
                        participants = listOf(participantRequest),
                    )
                return conversationRepository.muteSuspend(cid, request)
            }
        }

        suspend fun updateGroupMuteUntil(
            conversationId: String,
            muteUntil: String,
        ) = conversationRepository.updateGroupMuteUntil(conversationId, muteUntil)

        suspend fun updateMuteUntil(
            id: String,
            muteUntil: String,
        ) = userRepository.updateMuteUntil(id, muteUntil)

        suspend fun suspendFindUserById(query: String) = userRepository.suspendFindUserById(query)

        suspend fun findFirstUnreadMessageId(
            conversationId: String,
            offset: Int,
        ): String? =
            conversationRepository.findFirstUnreadMessageId(conversationId, offset)

        fun observeAllCircleItem() = userRepository.observeAllCircleItem()

        suspend fun circleRename(
            circleId: String,
            name: String,
        ) = userRepository.circleRename(circleId, name)

        suspend fun deleteCircle(circleId: String) = userRepository.deleteCircle(circleId)

        suspend fun deleteCircleById(circleId: String) {
            withTransaction {
                userRepository.deleteCircleById(circleId)
                userRepository.deleteByCircleId(circleId)
            }
        }

        suspend fun insertCircle(circle: Circle) = userRepository.insertCircle(circle)

        suspend fun getFriends(): List<User> = userRepository.getFriends()

        suspend fun successConversationList(): List<ConversationMinimal> =
            conversationRepository.successConversationList()

        suspend fun findConversationItemByCircleId(circleId: String) = userRepository.findConversationItemByCircleId(circleId)

        suspend fun updateCircleConversations(
            id: String,
            circleConversationRequests: List<CircleConversationRequest>,
        ) =
            withContext(Dispatchers.IO) {
                userRepository.updateCircleConversations(id, circleConversationRequests)
            }

        suspend fun sortCircleConversations(list: List<CircleOrder>?) = userRepository.sortCircleConversations(list)

        suspend fun saveCircle(
            circleId: String,
            addCircleConversation: List<CircleConversation>?,
            removeCircleConversation: Set<CircleConversationPayload>,
        ) {
            addCircleConversation?.forEach { circleConversation ->
                userRepository.insertCircleConversation(circleConversation)
            }

            removeCircleConversation.forEach { cc ->
                userRepository.deleteCircleConversation(cc.conversationId, circleId)
            }
        }

        suspend fun findCircleConversationByCircleId(circleId: String) =
            userRepository.findCircleConversationByCircleId(circleId)

        fun observeAllConversationUnread() = conversationRepository.observeAllConversationUnread()

        suspend fun getCircleConversationCount(conversationId: String) = userRepository.getCircleConversationCount(conversationId)

        suspend fun findAppById(appId: String) = userRepository.findAppById(appId)

        suspend fun findTotalUSDBalance() = tokenRepository.findTotalUSDBalance()

        fun hasUnreadMessage(circleId: String) = userRepository.hasUnreadMessage(circleId)

        suspend fun createCircle(name: String) = userRepository.createCircle(name)

        suspend fun findCircleItemByCircleIdSuspend(circleId: String) = userRepository.findCircleItemByCircleIdSuspend(circleId)
    }

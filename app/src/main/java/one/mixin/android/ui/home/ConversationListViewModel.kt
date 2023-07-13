package one.mixin.android.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
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
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager,
    private val cleanMessageHelper: CleanMessageHelper,
) : ViewModel() {

    fun observeConversations(circleId: String?): LiveData<PagingData<ConversationItem>> {
        return Pager(
            PagingConfig(
                pageSize = CONVERSATION_PAGE_SIZE,
                enablePlaceholders = true,
                initialLoadSize = CONVERSATION_PAGE_SIZE * 2,
            ),
            initialKey = 0,
        ) {
            conversationRepository.observeConversations(circleId)
        }.liveData
    }

    fun createGroupConversation(conversationId: String) {
        val c = conversationRepository.getConversation(conversationId)
        c?.let {
            val participants = conversationRepository.getGroupParticipants(conversationId)
            val mutableList = mutableListOf<Participant>()
            val createAt = nowInUtc()
            participants.mapTo(mutableList) { Participant(conversationId, it.userId, "", createAt) }
            val conversation = Conversation(
                c.conversationId, c.ownerId, c.category, c.name, c.iconUrl,
                c.announcement, null, c.payType, createAt, null, null,
                null, 0, ConversationStatus.START.ordinal, null,
            )
            viewModelScope.launch {
                conversationRepository.insertConversation(conversation, mutableList)
            }

            val participantRequestList = mutableListOf<ParticipantRequest>()
            mutableList.mapTo(participantRequestList) { ParticipantRequest(it.userId, it.role) }
            val request = ConversationRequest(
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

    fun deleteConversation(conversationId: String) = viewModelScope.launch(Dispatchers.IO) {
        val ids = conversationRepository.findTranscriptIdByConversationId(conversationId)
        if (ids.isNotEmpty()) {
            jobManager.addJobInBackground(TranscriptDeleteJob(ids))
        }
        cleanMessageHelper.deleteMessageByConversationId(conversationId, true)
    }

    fun updateConversationPinTimeById(conversationId: String, circleId: String?, pinTime: String?) = viewModelScope.launch {
        conversationRepository.updateConversationPinTimeById(conversationId, circleId, pinTime)
    }

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
            var cid = conversationRepository.getConversationIdIfExistsSync(recipientId!!)
            if (cid == null) {
                cid = generateConversationId(senderId!!, recipientId)
            }
            val participantRequest = ParticipantRequest(recipientId, "")
            val request = ConversationRequest(
                cid,
                ConversationCategory.CONTACT.name,
                duration = duration,
                participants = listOf(participantRequest),
            )
            return conversationRepository.muteSuspend(cid, request)
        }
    }

    suspend fun updateGroupMuteUntil(conversationId: String, muteUntil: String) {
        withContext(Dispatchers.IO) {
            conversationRepository.updateGroupMuteUntil(conversationId, muteUntil)
        }
    }

    suspend fun updateMuteUntil(id: String, muteUntil: String) {
        withContext(Dispatchers.IO) {
            userRepository.updateMuteUntil(id, muteUntil)
        }
    }

    suspend fun suspendFindUserById(query: String) = userRepository.suspendFindUserById(query)

    fun observeAllCircleItem() = userRepository.observeAllCircleItem()

    suspend fun circleRename(circleId: String, name: String) = userRepository.circleRename(circleId, name)

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

    suspend fun updateCircleConversations(id: String, circleConversationRequests: List<CircleConversationRequest>) = withContext(Dispatchers.IO) {
        userRepository.updateCircleConversations(id, circleConversationRequests)
    }

    fun sortCircleConversations(list: List<CircleOrder>?) = viewModelScope.launch { userRepository.sortCircleConversations(list) }

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

    suspend fun findTotalUSDBalance() = assetRepository.findTotalUSDBalance()
}

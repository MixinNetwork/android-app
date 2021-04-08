package one.mixin.android.ui.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantAction
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.ConversationJob.Companion.TYPE_ADD
import one.mixin.android.job.ConversationJob.Companion.TYPE_CREATE
import one.mixin.android.job.ConversationJob.Companion.TYPE_REMOVE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantItem
import one.mixin.android.vo.User
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun getFriends() = userRepository.findFriends()

    suspend fun createGroupConversation(
        groupName: String,
        announcement: String,
        icon: String?,
        users: List<User>,
        sender: User
    ): Conversation = withContext(Dispatchers.IO) {
        val conversationId = UUID.randomUUID().toString()
        val createdAt = nowInUtc()
        val conversation = ConversationBuilder(conversationId, createdAt, 0)
            .setCategory(ConversationCategory.GROUP.name)
            .setName(groupName)
            .setAnnouncement(announcement)
            .setOwnerId(sender.userId)
            .setUnseenMessageCount(0)
            .build()
        val mutableList = mutableListOf<Participant>()
        users.mapTo(mutableList) { Participant(conversationId, it.userId, "", createdAt) }
        conversationRepository.insertConversation(conversation, mutableList)

        val participantRequestList = mutableListOf<ParticipantRequest>()
        mutableList.mapTo(participantRequestList) { ParticipantRequest(it.userId, it.role) }
        val request = ConversationRequest(
            conversationId,
            ConversationCategory.GROUP.name,
            groupName,
            icon,
            announcement,
            participantRequestList
        )
        jobManager.addJobInBackground(ConversationJob(request, type = TYPE_CREATE))

        conversation
    }

    fun getConversationStatusById(id: String) = conversationRepository.getConversationById(id)

    fun observeGroupParticipants(conversationId: String): LiveData<PagedList<ParticipantItem>> {
        return LivePagedListBuilder(
            conversationRepository.observeGroupParticipants(conversationId),
            PagedList.Config.Builder()
                .setPrefetchDistance(Constants.PAGE_SIZE * 2)
                .setPageSize(Constants.PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }

    fun fuzzySearchGroupParticipants(conversationId: String, query: String): LiveData<PagedList<ParticipantItem>> {
        val escapedQuery = query.trim().escapeSql()
        return LivePagedListBuilder(
            conversationRepository.fuzzySearchGroupParticipants(conversationId, escapedQuery, escapedQuery),
            PagedList.Config.Builder()
                .setPrefetchDistance(Constants.PAGE_SIZE * 2)
                .setPageSize(Constants.PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }

    fun getConversationById(conversationId: String) =
        conversationRepository.getConversationById(conversationId)

    fun deleteMessageByConversationId(conversationId: String) = viewModelScope.launch {
        conversationRepository.deleteMessageByConversationId(conversationId)
    }

    fun mute(conversationId: String, duration: Long) {
        jobManager.addJobInBackground(
            ConversationJob(
                conversationId = conversationId,
                request = ConversationRequest(conversationId, ConversationCategory.GROUP.name, duration = duration),
                type = ConversationJob.TYPE_MUTE
            )
        )
    }

    suspend fun modifyMember(conversationId: String, users: List<User>, type: Int, role: String = "") = withContext(Dispatchers.IO) {
        val participantRequests = mutableListOf<ParticipantRequest>()
        users.mapTo(participantRequests) {
            ParticipantRequest(it.userId, role)
        }
        val action = when (type) {
            TYPE_ADD -> {
                ParticipantAction.ADD.name
            }
            TYPE_REMOVE -> {
                ParticipantAction.REMOVE.name
            }
            else -> {
                ParticipantAction.ROLE.name
            }
        }
        try {
            val response = conversationRepository.participants(conversationId, action, participantRequests).execute().body()
            return@withContext response != null && response.isSuccess && response.data != null
        } catch (e: Exception) {
            return@withContext false
        }
    }

    fun findParticipantById(conversationId: String, userId: String) =
        conversationRepository.findParticipantById(conversationId, userId)

    fun getGroupParticipants(conversationId: String) =
        conversationRepository.getGroupParticipants(conversationId)
}

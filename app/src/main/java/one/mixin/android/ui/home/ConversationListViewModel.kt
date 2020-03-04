package one.mixin.android.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import javax.inject.Inject
import kotlinx.coroutines.launch
import one.mixin.android.Constants.CONVERSATION_PAGE_SIZE
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.ConversationJob.Companion.TYPE_CREATE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.generateConversationId

class ConversationListViewModel @Inject
internal constructor(
    private val messageRepository: ConversationRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun observeConversations(): LiveData<PagedList<ConversationItem>> {
        return LivePagedListBuilder(
            messageRepository.conversations(), PagedList.Config.Builder()
            .setPrefetchDistance(CONVERSATION_PAGE_SIZE * 2)
            .setPageSize(CONVERSATION_PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build()
        ).build()
    }

    fun createGroupConversation(conversationId: String) {
        val c = messageRepository.getConversation(conversationId)
        c?.let {
            val participants = messageRepository.getGroupParticipants(conversationId)
            val mutableList = mutableListOf<Participant>()
            val createAt = nowInUtc()
            participants.mapTo(mutableList) { Participant(conversationId, it.userId, "", createAt) }
            val conversation = Conversation(c.conversationId, c.ownerId, c.category, c.name, c.iconUrl,
                c.announcement, null, c.payType, createAt, null, null,
                null, 0, ConversationStatus.START.ordinal, null)
            viewModelScope.launch {
                messageRepository.insertConversation(conversation, mutableList)
            }

            val participantRequestList = mutableListOf<ParticipantRequest>()
            mutableList.mapTo(participantRequestList) { ParticipantRequest(it.userId, it.role) }
            val request = ConversationRequest(conversationId, it.category!!, it.name, it.iconUrl,
                it.announcement, participantRequestList)
            jobManager.addJobInBackground(ConversationJob(request, type = TYPE_CREATE))
        }
    }

    fun deleteConversation(conversationId: String) = viewModelScope.launch {
        messageRepository.deleteConversationById(conversationId)
    }

    fun updateConversationPinTimeById(conversationId: String, pinTime: String?) = viewModelScope.launch {
        messageRepository.updateConversationPinTimeById(conversationId, pinTime)
    }

    fun mute(senderId: String, recipientId: String, duration: Long) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            var conversationId = messageRepository.getConversationIdIfExistsSync(recipientId)
            if (conversationId == null) {
                conversationId = generateConversationId(senderId, recipientId)
            }
            val participantRequest = ParticipantRequest(recipientId, "")
            jobManager.addJobInBackground(ConversationJob(ConversationRequest(conversationId,
                ConversationCategory.CONTACT.name, duration = duration, participants = listOf(participantRequest)),
                recipientId = recipientId, type = ConversationJob.TYPE_MUTE))
        }
    }

    fun mute(conversationId: String, duration: Long) {
        jobManager.addJobInBackground(ConversationJob(conversationId = conversationId,
            request = ConversationRequest(conversationId, ConversationCategory.GROUP.name, duration = duration),
            type = ConversationJob.TYPE_MUTE))
    }
}

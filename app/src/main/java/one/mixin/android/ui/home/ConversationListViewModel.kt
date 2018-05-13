package one.mixin.android.ui.home

import android.arch.lifecycle.ViewModel
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.ConversationJob.Companion.TYPE_CREATE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import javax.inject.Inject

class ConversationListViewModel @Inject
internal constructor(
    private val messageRepository: ConversationRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {
    var conversations = messageRepository.conversation()

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
            messageRepository.insertConversation(conversation, mutableList)

            val participantRequestList = mutableListOf<ParticipantRequest>()
            mutableList.mapTo(participantRequestList) { ParticipantRequest(it.userId, it.role) }
            val request = ConversationRequest(conversationId, it.category!!, it.name, it.iconUrl,
                it.announcement, participantRequestList)
            jobManager.addJobInBackground(ConversationJob(request, type = TYPE_CREATE))
        }
    }

    fun deleteConversation(conversationId: String) {
        messageRepository.deleteConversationById(conversationId)
    }

    fun updateConversationPinTimeById(conversationId: String, pinTime: String?) {
        messageRepository.updateConversationPinTimeById(conversationId, pinTime)
    }
}
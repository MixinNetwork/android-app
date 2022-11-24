package one.mixin.android.ui.call

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject
internal constructor(
    private val userRepository: UserRepository,
    private val conversationRepo: ConversationRepository,
    private val jobManager: MixinJobManager,
) : ViewModel() {

    suspend fun findMultiCallUsersByIds(conversationId: String, ids: Set<String>) =
        userRepository.findMultiCallUsersByIds(conversationId, ids)

    suspend fun findSelfCallUser(conversationId: String, userId: String) =
        userRepository.findSelfCallUser(conversationId, userId)

    suspend fun getConversationNameById(cid: String) = conversationRepo.getConversationNameById(cid)

    suspend fun suspendFindUserById(userId: String) = userRepository.suspendFindUserById(userId)

    fun refreshConversation(conversationId: String) {
        jobManager.addJobInBackground(RefreshConversationJob(conversationId, true))
    }
}

package one.mixin.android.ui.call

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject
internal constructor(
    private val userRepository: UserRepository,
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    suspend fun findMultiCallUsersByIds(conversationId: String, ids: Set<String>) =
        userRepository.findMultiCallUsersByIds(conversationId, ids)

    suspend fun findSelfCallUser(conversationId: String, userId: String) =
        userRepository.findSelfCallUser(conversationId, userId)

    fun observeConversationNameById(cid: String) = conversationRepo.observeConversationNameById(cid)

    suspend fun suspendFindUserById(userId: String) = userRepository.suspendFindUserById(userId)
}

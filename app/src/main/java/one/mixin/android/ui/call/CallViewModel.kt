package one.mixin.android.ui.call

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository

class CallViewModel
@ViewModelInject
internal constructor(
    private val userRepository: UserRepository,
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    suspend fun findMultiUsersByIds(ids: Set<String>) = userRepository.findMultiUsersByIds(ids)

    fun observeConversationNameById(cid: String) = conversationRepo.observeConversationNameById(cid)
}

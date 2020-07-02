package one.mixin.android.ui.call

import androidx.lifecycle.ViewModel
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

class CallViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    suspend fun findMultiUsersByIds(ids: Set<String>) = userRepository.findMultiUsersByIds(ids)

    fun observeConversationNameById(cid: String) = conversationRepo.observeConversationNameById(cid)
}

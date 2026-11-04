package one.mixin.android.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.service.ConversationService
import one.mixin.android.repository.ConversationRepository
import javax.inject.Inject

@HiltViewModel
class InviteViewModel
    @Inject
    internal constructor(
        private val conversationService: ConversationService,
        private val conversationRepository: ConversationRepository,
    ) : ViewModel() {
        suspend fun rotate(conversationId: String): MixinResponse<ConversationResponse> =
            conversationService.rotate(conversationId)

        suspend fun findConversation(conversationId: String): MixinResponse<ConversationResponse> =
            conversationService.findConversationSuspend(conversationId)

        fun updateCodeUrl(
            conversationId: String,
            codeUrl: String,
        ) =
            viewModelScope.launch {
                conversationRepository.updateCodeUrl(conversationId, codeUrl)
            }

        fun getConversation(conversationId: String) = conversationRepository.getConversationById(conversationId)
    }

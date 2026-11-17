package one.mixin.android.ui.home

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.Constants.CIRCLE.CIRCLE_NAME
import one.mixin.android.Constants.CIRCLE.CONVERSATION_FILTER
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.ConversationCircleItem
import javax.inject.Inject

sealed interface ConversationFilter {
    data object All : ConversationFilter

    data object Contacts : ConversationFilter

    data object Groups : ConversationFilter

    data object Bots : ConversationFilter

    data object Strangers : ConversationFilter

    data class Circle(val circleId: String, val name: String) : ConversationFilter
}

@HiltViewModel
class HomeFilterViewModel
    @Inject
    internal constructor(
        private val conversationRepository: ConversationRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val preferences = MixinApplication.appContext.defaultSharedPreferences

        private val _filter = MutableStateFlow(restoreFilter())
        val filter: StateFlow<ConversationFilter> = _filter.asStateFlow()

        fun effectiveFilter(context: Context): ConversationFilter {
            val selected = filter.value
            return if (!context.shouldShowHomeRail() && selected !is ConversationFilter.Circle) ConversationFilter.All else selected
        }

        fun select(filter: ConversationFilter) {
            if (_filter.value == filter) return
            _filter.value = filter
            val circle = filter as? ConversationFilter.Circle
            preferences.edit {
                putString(
                    CONVERSATION_FILTER,
                    when (filter) {
                        ConversationFilter.All -> "all"
                        ConversationFilter.Contacts -> "contacts"
                        ConversationFilter.Groups -> "groups"
                        ConversationFilter.Bots -> "bots"
                        ConversationFilter.Strangers -> "strangers"
                        is ConversationFilter.Circle -> "circle"
                    },
                )
                putString(CIRCLE_ID, circle?.circleId)
                putString(CIRCLE_NAME, circle?.name)
            }
        }

        private fun restoreFilter(): ConversationFilter =
            when (preferences.getString(CONVERSATION_FILTER, null)) {
                "contacts" -> ConversationFilter.Contacts
                "groups" -> ConversationFilter.Groups
                "bots" -> ConversationFilter.Bots
                "strangers" -> ConversationFilter.Strangers
                else ->
                    preferences.getString(CIRCLE_ID, null)?.takeIf { it.isNotBlank() }?.let {
                        ConversationFilter.Circle(it, preferences.getString(CIRCLE_NAME, null) ?: "Mixin")
                    } ?: ConversationFilter.All
            }

        fun observeCircles(): LiveData<List<ConversationCircleItem>> = userRepository.observeAllCircleItem()

        // Number of conversations under the filter that have unseen messages.
        fun observeUnseenCount(filter: ConversationFilter): LiveData<Int> = conversationRepository.observeUnseenCount(filter)
    }

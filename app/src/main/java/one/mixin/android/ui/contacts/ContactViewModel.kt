package one.mixin.android.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.ConversationJob.Companion.TYPE_MUTE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import javax.inject.Inject

class ContactViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val conversationRepository: ConversationRepository,
    private var jobManager: MixinJobManager
) : ViewModel() {

    fun getFriends(): LiveData<List<User>> = userRepository.findFriends()

    fun findContacts(): LiveData<List<User>> = userRepository.findContacts()

    fun findSelf(): LiveData<User?> = userRepository.findSelf()

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    fun search(query: String): Observable<MixinResponse<User>> =
        accountRepository.search(query).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

    fun mute(senderId: String, recipientId: String, duration: Long) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            var conversationId = conversationRepository.getConversationIdIfExistsSync(recipientId)
            if (conversationId == null) {
                conversationId = generateConversationId(senderId, recipientId)
            }
            val participantRequest = ParticipantRequest(recipientId, "")
            jobManager.addJobInBackground(
                ConversationJob(
                    ConversationRequest(
                        conversationId,
                        ConversationCategory.CONTACT.name, duration = duration, participants = listOf(participantRequest)
                    ),
                    recipientId = recipientId, type = TYPE_MUTE
                )
            )
        }
    }
}

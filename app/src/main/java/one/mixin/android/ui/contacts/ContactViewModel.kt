package one.mixin.android.ui.contacts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.AppExecutors
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.job.ConversationJob
import one.mixin.android.job.ConversationJob.Companion.TYPE_MUTE
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.Account
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

    fun findSelf(): LiveData<User?> = userRepository.findSelf()

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) {
        userRepository.upsert(user)
    }

    fun search(query: String): Observable<MixinResponse<User>> =
        accountRepository.search(query).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun findUserById(id: String): LiveData<User> = userRepository.findUserById(id)

    fun updateRelationship(request: RelationshipRequest) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request))
    }

    fun redeem(code: String): Observable<MixinResponse<Account>> =
        accountRepository.redeem(code).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun mute(senderId: String, recipientId: String, duration: Long) {
        AppExecutors().diskIO().execute {
            var conversationId = conversationRepository.getConversationIdIfExistsSync(recipientId)
            if (conversationId == null) {
                conversationId = generateConversationId(senderId, recipientId)
            }
            val participantRequest = ParticipantRequest(recipientId, "")
            jobManager.addJobInBackground(ConversationJob(ConversationRequest(conversationId,
                ConversationCategory.CONTACT.name, duration = duration, participants = listOf(participantRequest)),
                recipientId = recipientId, type = TYPE_MUTE))
        }
    }
}
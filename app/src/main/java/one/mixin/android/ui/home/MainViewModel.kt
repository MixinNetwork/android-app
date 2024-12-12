package one.mixin.android.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.session.Session
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
internal constructor(
    private val databaseProvider: DatabaseProvider,
    private val jobManager: MixinJobManager,
    private val accountService: AccountService,
    private val userService: UserService,
    private val conversationService: ConversationService,
) : ViewModel() {

    fun updateSession(request: SessionRequest) = accountService.updateSession(request)

    fun deviceCheck() = accountService.deviceCheck()

    fun syncOwnerByConversationId(conversationId: String): User? {
        val conversation = databaseProvider.getMixinDatabase().conversationDao().findConversationById(conversationId)
        if (conversation == null) {
            return null
        }
        var user = databaseProvider.getMixinDatabase().userDao().findOwnerByConversationId(conversationId)
        if (user == null) {
            val response = userService.getUserById(conversation.ownerId!!).execute().body()
            if (response != null && response.isSuccess) {
                response.data?.let { u ->
                    runBlocking { databaseProvider.getMixinDatabase().userDao().upsert(u) }
                    user = u
                }
            }
        }
        return user
    }

    fun syncConverstion(conversationId: String): Conversation? {
        var conversation = databaseProvider.getMixinDatabase().conversationDao().findConversationById(conversationId)
        if (conversation != null) return conversation
        val response =
            conversationService.getConversation(conversationId).execute().body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                var ownerId: String = data.creatorId
                if (data.category == ConversationCategory.CONTACT.name) {
                    ownerId =
                        data.participants.find { p -> p.userId != Session.getAccountId() }!!.userId
                } else if (data.category == ConversationCategory.GROUP.name) {
                    ownerId = data.creatorId
                }
                var c = databaseProvider.getMixinDatabase().conversationDao().findConversationById(data.conversationId)
                if (c == null) {
                    c =
                        Conversation(
                            data.conversationId,
                            ownerId,
                            data.category,
                            data.name,
                            data.iconUrl,
                            data.announcement,
                            data.codeUrl,
                            "",
                            data.createdAt,
                            null,
                            null,
                            null,
                            0,
                            ConversationStatus.SUCCESS.ordinal,
                            null,
                        )
                    conversation = c
                    databaseProvider.getMixinDatabase().conversationDao().upsert(c)
                } else {
                    databaseProvider.getMixinDatabase().conversationDao().updateConversation(
                        data.conversationId,
                        ownerId,
                        data.category,
                        data.name,
                        data.announcement,
                        data.muteUntil,
                        data.createdAt,
                        data.expireIn,
                        ConversationStatus.SUCCESS.ordinal,
                    )
                }

                val participants = mutableListOf<Participant>()
                val userIdList = mutableListOf<String>()
                for (p in data.participants) {
                    val item =
                        Participant(conversationId, p.userId, p.role, p.createdAt!!)
                    if (p.role == ParticipantRole.OWNER.name) {
                        participants.add(0, item)
                    } else {
                        participants.add(item)
                    }

                    val u = databaseProvider.getMixinDatabase().userDao().findUser(p.userId)
                    if (u == null) {
                        userIdList.add(p.userId)
                    }
                }
                if (userIdList.isNotEmpty()) {
                    jobManager.addJobInBackground(RefreshUserJob(userIdList))
                }
                databaseProvider.getMixinDatabase().participantDao().insertList(participants)
            }
        }
        return conversation
    }

    suspend fun refreshUser(id: String): User? {
        val user = databaseProvider.getMixinDatabase().userDao().suspendFindUserById(id)
        if (user != null) return user

        return handleMixinResponse(
            invokeNetwork = {
                userService.getUserByIdSuspend(id)
            },
            successBlock = {
                it.data?.let { u ->
                    databaseProvider.getMixinDatabase().userDao().upsert(u)
                    return@handleMixinResponse u
                }
            },
        )
    }
}
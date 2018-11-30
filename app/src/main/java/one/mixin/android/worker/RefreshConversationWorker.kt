package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import one.mixin.android.RxBus
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertConversation
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.di.worker.AndroidWorkerInjector
import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.enqueueOneTimeRequest
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.job.MixinJobManager
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import javax.inject.Inject

class RefreshConversationWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {

    @Inject
    lateinit var conversationApi: ConversationService
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var conversationDao: ConversationDao
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var jobManager: MixinJobManager

    companion object {
        const val CONVERSATION_ID = "conversation_id"
        const val PREFERENCES_CONVERSATION = "preferences_conversation"
    }

    override fun onRun(): Result {
        val conversationId = inputData.getString(CONVERSATION_ID) ?: return Result.FAILURE
        val call = conversationApi.getConversation(conversationId).execute()
        val response = call.body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                var ownerId: String = data.creatorId
                if (data.category == ConversationCategory.CONTACT.name) {
                    ownerId = data.participants.find { it.userId != Session.getAccountId() }!!.userId
                }
                var c = conversationDao.findConversationById(data.conversationId)
                if (c == null) {
                    val builder = ConversationBuilder(data.conversationId,
                        data.createdAt, ConversationStatus.SUCCESS.ordinal)
                    c = builder.setOwnerId(ownerId)
                        .setCategory(data.category)
                        .setName(data.name)
                        .setIconUrl(data.iconUrl)
                        .setAnnouncement(data.announcement)
                        .setCodeUrl(data.codeUrl).build()
                    if (c.announcement.isNullOrBlank()) {
                        RxBus.publish(GroupEvent(data.conversationId))
                        applicationContext.sharedPreferences(PREFERENCES_CONVERSATION)
                            .putBoolean(data.conversationId, true)
                    }
                    conversationDao.insertConversation(c)
                } else {
                    val status = if (data.participants.find { Session.getAccountId() == it.userId } != null) {
                        ConversationStatus.SUCCESS.ordinal
                    } else {
                        ConversationStatus.QUIT.ordinal
                    }
                    if (!data.announcement.isNullOrBlank() && c.announcement != data.announcement) {
                        RxBus.publish(GroupEvent(data.conversationId))
                        applicationContext.sharedPreferences(PREFERENCES_CONVERSATION)
                            .putBoolean(data.conversationId, true)
                    }
                    conversationDao.updateConversation(data.conversationId, ownerId, data.category, data.name,
                        data.announcement, data.muteUntil, data.createdAt, status)
                }

                val participants = mutableListOf<Participant>()
                val userIdList = mutableListOf<String>()
                for (p in data.participants) {
                    val item = Participant(conversationId, p.userId, p.role, p.createdAt!!)
                    if (p.role == ParticipantRole.OWNER.name) {
                        participants.add(0, item)
                    } else {
                        participants.add(item)
                    }

                    val u = userDao.findUser(p.userId)
                    if (u == null) {
                        userIdList.add(p.userId)
                    }
                }
                val local = participantDao.getRealParticipants(data.conversationId)
                val remoteIds = participants.map { it.userId }
                val needRemove = local.filter { !remoteIds.contains(it.userId) }
                if (needRemove.isNotEmpty()) {
                    participantDao.deleteList(needRemove)
                }
                participantDao.insertList(participants)
                if (userIdList.isNotEmpty()) {
                    WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshUserWorker>(
                        workDataOf(RefreshUserWorker.USER_IDS to userIdList.toTypedArray(),
                            RefreshUserWorker.CONVERSATION_ID to conversationId))
                } else {
                    WorkManager.getInstance().enqueueOneTimeRequest<GenerateAvatarWorker>(
                        workDataOf(GenerateAvatarWorker.GROUP_ID to conversationId))
                }
            }
            return Result.SUCCESS
        } else {
            return Result.FAILURE
        }
    }
}
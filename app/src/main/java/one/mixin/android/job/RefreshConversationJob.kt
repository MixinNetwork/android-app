package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.RxBus
import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole

class RefreshConversationJob(val conversationId: String)
    : MixinJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy("refresh_conversation")
    .requireNetwork().persist(), conversationId) {

    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshConversationJob"
        const val PREFERENCES_CONVERSATION = "preferences_conversation"
    }

    override fun onAdded() {
        jobManager.saveJob(this)
    }

    override fun onRun() {
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
                        RxBus.getInstance().post(GroupEvent(data.conversationId))
                        applicationContext.sharedPreferences(PREFERENCES_CONVERSATION)
                            .putBoolean(data.conversationId, true)
                    }
                    conversationDao.insert(c)
                } else {
                    val status = if (data.participants.find { Session.getAccountId() == it.userId } != null) {
                        ConversationStatus.SUCCESS.ordinal
                    } else {
                        ConversationStatus.QUIT.ordinal
                    }
                    if (!data.announcement.isNullOrBlank() && c.announcement != data.announcement) {
                        RxBus.getInstance().post(GroupEvent(data.conversationId))
                        applicationContext.sharedPreferences(PREFERENCES_CONVERSATION)
                            .putBoolean(data.conversationId, true)
                    }
                    conversationDao.updateConversation(data.conversationId, ownerId, data.category, data.name,
                        data.announcement, data.createdAt, status)
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
                participantDao.insertList(participants)
                if (userIdList.isNotEmpty()) {
                    jobManager.addJobInBackground(RefreshUserJob(userIdList, conversationId))
                } else {
                    jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                }
            }
        }

        removeJob()
    }
}
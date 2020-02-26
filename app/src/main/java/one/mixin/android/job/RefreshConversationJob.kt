package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.util.Session
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.SYSTEM_USER

class RefreshConversationJob(val conversationId: String) :
    MixinJob(Params(PRIORITY_UI_HIGH).groupBy("refresh_conversation")
    .requireNetwork().persist(), conversationId) {

    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
        const val PREFERENCES_CONVERSATION = "preferences_conversation"
    }

    override fun onRun() {
        if (conversationId == SYSTEM_USER || conversationId == Session.getAccountId()) {
            return
        }
        val localData = participantDao.getRealParticipants(conversationId)
        val call = conversationApi.getConversation(conversationId).execute()
        val response = call.body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                insertOrUpdateConversation(data)
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

                participantDao.replaceAll(data.conversationId, participants)
                data.participantSessions?.let {
                    syncParticipantSession(conversationId, it)
                }

                if (userIdList.isNotEmpty()) {
                    jobManager.addJobInBackground(RefreshUserJob(userIdList, conversationId))
                }
                if (participants.size != localData.size || userIdList.isNotEmpty()) {
                    jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                }
            }
        }
    }
}

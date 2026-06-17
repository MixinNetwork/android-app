package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.session.Session
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.SYSTEM_USER

class RefreshConversationJob(val conversationId: String, private val skipRefreshCircle: Boolean = false) :
    MixinJob(
        Params(PRIORITY_UI_HIGH).groupBy("refresh_conversation")
            .requireNetwork().persist(),
        conversationId,
    ) {
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
                conversationRepo.insertOrUpdateConversation(data)
                val participants = mutableListOf<Participant>()
                val conversationUserIds = mutableListOf<String>()
                for (p in data.participants) {
                    val item = Participant(conversationId, p.userId, p.role, p.createdAt!!)
                    if (p.role == ParticipantRole.OWNER.name) {
                        participants.add(0, item)
                    } else {
                        participants.add(item)
                    }
                    conversationUserIds.add(p.userId)
                }

                participantDao.replaceAll(data.conversationId, participants)
                data.participantSessions?.let {
                    jobSenderKey.syncParticipantSession(conversationId, it)
                }

                if (conversationUserIds.isNotEmpty()) {
                    jobManager.addJobInBackground(RefreshUserJob(conversationUserIds, conversationId))
                }
                if (participants.size != localData.size || conversationUserIds.isNotEmpty()) {
                    jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                }
                if (skipRefreshCircle) return@let
                data.circles?.let { circles ->
                    circles.forEach {
                        val circle = circleDao.findCircleById(it.circleId)
                        if (circle == null) {
                            val circleResponse = circleService.getCircle(it.circleId).execute().body()
                            if (circleResponse?.isSuccess == true) {
                                circleResponse.data?.let { item ->
                                    circleDao.insert(item)
                                }
                            }
                        }
                        circleConversationDao.insertUpdate(it)
                    }
                }
            }
        }
    }
}

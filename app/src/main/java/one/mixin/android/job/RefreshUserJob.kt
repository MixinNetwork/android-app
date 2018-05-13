package one.mixin.android.job

import com.birbit.android.jobqueue.Params

/**
 * @param conversationId NOT NULL means should generate the group avatar
 */
class RefreshUserJob(private val userIds: List<String>, private val conversationId: String? = null)
    : BaseJob(Params(PRIORITY_UI_HIGH).addTags(RefreshUserJob.GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RefreshUserJob"
    }

    override fun onRun() {
        if (userIds.isEmpty()) {
            return
        }
        val call = userService.getUsers(userIds).execute()
        val response = call.body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                for (u in data) {
                    userRepo.upsert(u)
                }

                conversationId?.let {
                    jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                }
            }
        }
    }
}

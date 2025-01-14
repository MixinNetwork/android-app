package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.extension.isUUID

/**
 * @param conversationId NOT NULL means should generate the group avatar
 * @param forceRefresh Only refresh NOT exists users if `false`, `true` otherwise.
 */
class RefreshUserJob(
    private val userIds: List<String>,
    private val conversationId: String? = null,
    private val forceRefresh: Boolean = false,
) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RefreshUserJob"
    }

    override fun onRun() =
        runBlocking {
            val ids = userIds.filter { it.isNotBlank() && it.isUUID() }
            if (ids.isEmpty()) {
                return@runBlocking
            }
            if (forceRefresh) {
                refreshUsers(ids)
                return@runBlocking
            }
            val existUsers = userDao.findUserExist(ids)
            val queryUsers =
                ids.filter {
                    !existUsers.contains(it)
                }
            if (queryUsers.isEmpty()) {
                refreshConversationAvatar()
                return@runBlocking
            }
            refreshUsers(queryUsers)
        }

    private suspend fun refreshUsers(userIds: List<String>) {
        if (userIds.size == 1) {
            val response = userService.getUserById(userIds[0]).execute().body()
            if (response != null && response.isSuccess) {
                response.data?.let { data ->
                    userRepo.upsert(data)
                    refreshConversationAvatar()
                }
            }
        } else {
            val response = userService.getUsers(userIds).execute().body()
            if (response != null && response.isSuccess) {
                response.data?.let { data ->
                    userRepo.upsertList(data)
                    refreshConversationAvatar()
                }
            }
        }
    }

    private fun refreshConversationAvatar() {
        if (conversationId == null) return
        jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
    }
}

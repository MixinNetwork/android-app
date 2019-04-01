package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants

/**
 * @param conversationId NOT NULL means should generate the group avatar
 * @param forceRefresh Only refresh NOT exists users if `false`, `true` otherwise.
 */
class RefreshUserJob(
    private val userIds: List<String>,
    private val conversationId: String? = null,
    private val forceRefresh: Boolean = false
) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(RefreshUserJob.GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RefreshUserJob"
    }

    override fun onRun() = runBlocking {
        if (userIds.isEmpty()) {
            return@runBlocking
        }
        if (forceRefresh) {
            refreshUsers(userIds)
            return@runBlocking
        }

        val queryUserIds = getQueryExistsUserIds(userIds) {
            userDao.findUserExist(it)
        }
        if (queryUserIds.isEmpty()) {
            return@runBlocking
        }
        refreshUsers(queryUserIds)
    }

    private fun refreshUsers(userIds: List<String>) {
        val response = userService.getUsers(userIds).execute().body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                userRepo.upsertList(data)
                conversationId?.let {
                    jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                }
            }
        }
    }
}

fun getQueryExistsUserIds(
    userIds: List<String>,
    queryUsersAction: (List<String>) -> List<String>
): List<String> {
    val queryUserIds = arrayListOf<String>()
    userIds.chunked(Constants.SQLITE_MAX_VARIABLE_NUMBER)
        .forEach { list ->
            val existsUserIds = queryUsersAction.invoke(list)
            queryUserIds.addAll(userIds.filter { id ->
                !existsUserIds.contains(id)
            })
        }
    return queryUserIds
}

package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class FavoriteAppJob(vararg val userIds: String?) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "FavoriteAppJob"
    }

    override fun onRun() =
        runBlocking {
            userIds.forEach { userId ->
                userId ?: return@forEach
                userService.getUserFavoriteApps(userId).run {
                    if (isSuccess) {
                        data?.let { data ->
                            favoriteAppDao().deleteByUserId(userId)
                            favoriteAppDao().insertList(data)
                            data.map { app -> app.appId }.filter { id ->
                                appDao().findAppById(id) == null || userDao().suspendFindUserById(id) == null
                            }.let { ids ->
                                if (ids.isEmpty()) return@forEach

                                val response = userService.fetchUsers(ids)
                                if (response.isSuccess) {
                                    response.data?.apply {
                                        userDao().insertUpdateList(this, appDao())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

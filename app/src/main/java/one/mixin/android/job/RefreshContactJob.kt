package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.User

class RefreshContactJob : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshContactJob"
    }

    override fun onRun() = runBlocking {
        val response = contactService.friends().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val users = response.data as List<User>
            val existedUserIds = userDao.findUserExist(users.map { it.userId })
            val newUsers = users.filter { user ->
                !existedUserIds.contains(user.userId)
            }
            userDao.insertUpdateList(newUsers, appDao)
        }
    }
}

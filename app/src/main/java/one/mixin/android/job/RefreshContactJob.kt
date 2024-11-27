package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.db.insertUpdateList
import one.mixin.android.vo.User

class RefreshContactJob : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshContactJob"
    }

    override fun onRun() {
        val response = contactService.friends().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val users = response.data as List<User>
            userDao().insertUpdateList(users, appDao())
        }
    }
}

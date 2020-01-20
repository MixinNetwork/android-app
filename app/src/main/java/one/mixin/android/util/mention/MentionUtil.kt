package one.mixin.android.util.mention

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.db.UserDao
import one.mixin.android.vo.User
import timber.log.Timber
import java.util.regex.Pattern

suspend fun parseMention(text: String, userDao: UserDao) {
    withContext(Dispatchers.IO) {
        val matcher = mentionPattern.matcher(text)
        val users = mutableListOf<User>()
        while (matcher.find()) {
            val name = matcher.group().substring(1)
            val user = userDao.findUSerByFullNameSuspend(name)
            user?.let { u ->
                Timber.d(u.userId)
            }
        }

    }
}

private val mentionPattern by lazy {
    Pattern.compile("@([\\S]+(\\s|\$))")
}
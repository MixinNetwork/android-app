package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.extension.getGroupAvatarPath
import one.mixin.android.extension.md5
import one.mixin.android.vo.User
import java.io.File
import javax.inject.Inject

abstract class AvatarWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {
    companion object {
        const val GROUP_ID = "group_id"
    }

    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var conversationDao: ConversationDao

    val users = mutableListOf<User>()

    protected fun checkGroupAvatar(groupId: String): Triple<Boolean, String, File> {
        users.addAll(participantDao.getParticipantsAvatar(groupId))
        val sb = StringBuilder()
        for (u in users) {
            sb.append(u.avatarUrl).append("-")
        }
        sb.append(groupId)
        val name = sb.toString().md5()
        val f = applicationContext.getGroupAvatarPath(name, false)
        if (f.exists()) {
            if (f.absolutePath != name) {
                conversationDao.updateGroupIconUrl(groupId, f.absolutePath)
            }
            return Triple(true, name, f)
        }
        return Triple(false, name, f)
    }
}

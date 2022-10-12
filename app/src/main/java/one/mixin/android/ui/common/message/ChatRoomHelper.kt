package one.mixin.android.ui.common.message

import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication.Companion.appScope
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.runInTransaction
import one.mixin.android.util.SINGLE_THREAD
import one.mixin.android.util.debug.timeoutEarlyWarning
import javax.inject.Inject

class ChatRoomHelper @Inject internal constructor(private val appDatabase: MixinDatabase) {
    fun saveDraft(conversationId: String, draft: String) = appScope.launch {
        timeoutEarlyWarning({
            val localDraft = appDatabase.conversationDao().getConversationDraftById(conversationId)
            if (localDraft != draft) {
                appDatabase.conversationDao().saveDraft(conversationId, draft)
            }
        })
    }

    fun markMessageRead(conversationId: String) {
        appScope.launch(SINGLE_THREAD) {
            val remoteMessageDao = appDatabase.remoteMessageStatusDao()
            timeoutEarlyWarning({
                runInTransaction {
                    remoteMessageDao.markReadByConversationId(conversationId)
                    remoteMessageDao.zeroConversationUnseen(conversationId)
                }
            })
        }
    }
}

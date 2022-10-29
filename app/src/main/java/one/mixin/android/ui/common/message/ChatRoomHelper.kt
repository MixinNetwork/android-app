package one.mixin.android.ui.common.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.runInTransaction
import one.mixin.android.di.ApplicationScope
import one.mixin.android.util.SINGLE_THREAD
import one.mixin.android.util.debug.timeoutEarlyWarning
import javax.inject.Inject

class ChatRoomHelper @Inject internal constructor(@ApplicationScope private val applicationScope: CoroutineScope, private val appDatabase: MixinDatabase) {
    fun saveDraft(conversationId: String, draft: String) = applicationScope.launch {
        timeoutEarlyWarning({
            val localDraft = appDatabase.conversationDao().getConversationDraftById(conversationId)
            if (localDraft != draft) {
                appDatabase.conversationDao().saveDraft(conversationId, draft)
            }
        })
    }

    fun markMessageRead(conversationId: String) {
        applicationScope.launch(SINGLE_THREAD) {
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

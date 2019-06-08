package one.mixin.android.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bugsnag.android.Bugsnag
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.extension.getFilePath
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.StorageUsage
import java.io.File
import javax.inject.Inject

class SettingStorageViewModel @Inject
internal constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    fun getStorageUsage(conversationId: String): Single<List<StorageUsage>?> = conversationRepository.getStorageUsage(conversationId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun getConversationStorageUsage(): LiveData<List<ConversationStorageUsage>?> = conversationRepository.getConversationStorageUsage()

    fun clear(conversationId: String, category: String) {
        conversationRepository.getMediaByConversationIdAndCategory(conversationId, category)
            ?.let {
                for (item in it) {
                    if (item.mediaUrl != null) {
                        try {
                            File(item.mediaUrl.getFilePath()).let { file ->
                                if (file.exists() && file.isFile) {
                                    file.delete()
                                }
                            }
                        } catch (e: Exception) {
                            Bugsnag.notify(e)
                        }
                    }
                    conversationRepository.deleteMessage(item.messageId)
                }
            }
    }
}
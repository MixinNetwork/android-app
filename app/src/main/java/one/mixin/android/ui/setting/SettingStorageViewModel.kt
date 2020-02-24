package one.mixin.android.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.StorageUsage

class SettingStorageViewModel @Inject
internal constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    fun getStorageUsage(conversationId: String): Single<List<StorageUsage>?> =
        conversationRepository.getStorageUsage(conversationId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun getConversationStorageUsage(): LiveData<List<ConversationStorageUsage>?> = conversationRepository.getConversationStorageUsage()

    fun clear(conversationId: String, category: String) {
        conversationRepository.getMediaByConversationIdAndCategory(conversationId, category)
            ?.let { list ->
                list.forEach { item ->
                    conversationRepository.deleteMessage(item.messageId, item.mediaUrl)
                }
            }
    }
}

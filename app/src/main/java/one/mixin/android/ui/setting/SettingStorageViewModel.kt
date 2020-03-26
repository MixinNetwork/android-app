package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.Constants.Storage.AUDIO
import one.mixin.android.Constants.Storage.DATA
import one.mixin.android.Constants.Storage.IMAGE
import one.mixin.android.Constants.Storage.VIDEO
import one.mixin.android.MixinApplication
import one.mixin.android.extension.deleteDir
import one.mixin.android.extension.getConversationAudioPath
import one.mixin.android.extension.getConversationDocumentPath
import one.mixin.android.extension.getConversationImagePath
import one.mixin.android.extension.getConversationMediaSize
import one.mixin.android.extension.getConversationVideoPath
import one.mixin.android.extension.getStorageUsageByConversationAndType
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.StorageUsage
import javax.inject.Inject

class SettingStorageViewModel @Inject
internal constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    fun getStorageUsage(conversationId: String): Single<List<StorageUsage>> =
        Single.just(conversationId).map { cid ->
            val result = mutableListOf<StorageUsage>()
            val context = MixinApplication.appContext
            context.getStorageUsageByConversationAndType(conversationId, IMAGE)?.apply {
                result.add(this)
            }
            context.getStorageUsageByConversationAndType(conversationId, VIDEO)?.apply {
                result.add(this)
            }
            context.getStorageUsageByConversationAndType(conversationId, AUDIO)?.apply {
                result.add(this)
            }
            context.getStorageUsageByConversationAndType(conversationId, DATA)?.apply {
                result.add(this)
            }
            result.toList()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun getConversationStorageUsage(): Single<List<ConversationStorageUsage>> = conversationRepository.getConversationStorageUsage()
        .map { list ->
            list.asSequence().map { item ->
                val context = MixinApplication.appContext
                item.mediaSize = context.getConversationMediaSize(item.conversationId)
                item
            }.filter { conversationStorageUsage ->
                conversationStorageUsage.mediaSize != 0L
            }.sortedByDescending { conversationStorageUsage ->
                conversationStorageUsage.mediaSize
            }.toList()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun clear(conversationId: String, type: String) {
        val context = MixinApplication.appContext
        val dir = when (type) {
            IMAGE -> {
                conversationRepository.deleteMediaMessageByConversationAndCategory(
                    conversationId,
                    MessageCategory.SIGNAL_IMAGE.name,
                    MessageCategory.PLAIN_IMAGE.name
                )
                context.getConversationImagePath(conversationId)
            }
            VIDEO -> {
                conversationRepository.deleteMediaMessageByConversationAndCategory(
                    conversationId,
                    MessageCategory.SIGNAL_VIDEO.name,
                    MessageCategory.PLAIN_VIDEO.name
                )
                context.getConversationVideoPath(conversationId)
            }
            AUDIO -> {
                conversationRepository.deleteMediaMessageByConversationAndCategory(
                    conversationId,
                    MessageCategory.SIGNAL_AUDIO.name,
                    MessageCategory.PLAIN_AUDIO.name
                )
                context.getConversationAudioPath(conversationId)
            }
            DATA -> {
                conversationRepository.deleteMediaMessageByConversationAndCategory(
                    conversationId,
                    MessageCategory.SIGNAL_DATA.name,
                    MessageCategory.PLAIN_DATA.name
                )
                context.getConversationDocumentPath(conversationId)
            }
            else -> null
        } ?: return
        dir.deleteDir()
    }
}

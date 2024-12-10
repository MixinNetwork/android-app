package one.mixin.android.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.player.MusicBottomSheetDialogFragment.Companion.CONVERSATION_UI_PAGE_SIZE
import one.mixin.android.ui.player.internal.ConversationLoader
import one.mixin.android.vo.MediaStatus
import javax.inject.Inject

@UnstableApi @HiltViewModel
class MusicViewModel
    @Inject
    internal constructor(
        private val conversationRepo: ConversationRepository,
        private val jobManager: MixinJobManager,
        private val databaseProvider: DatabaseProvider,
    ) : ViewModel() {
        private val conversationLoader = ConversationLoader()

        fun conversationLiveData(
            conversationId: String,
            initialLoadKey: Int,
        ) =
            conversationLoader.conversationLiveData(conversationId, databaseProvider.getMixinDatabase(), CONVERSATION_UI_PAGE_SIZE, initialLoadKey)

        suspend fun indexAudioByConversationId(
            conversationId: String,
            mediaId: String,
        ) =
            conversationRepo.indexAudioByConversationId(mediaId, conversationId)

        suspend fun download(mediaId: String) {
            conversationRepo.suspendFindMessageById(mediaId)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }

        fun cancel(
            conversationId: String,
            mediaId: String,
        ) =
            viewModelScope.launch(Dispatchers.IO) {
                jobManager.cancelJobByMixinJobId(mediaId) {
                    viewModelScope.launch {
                        conversationRepo.updateMediaStatusSuspend(MediaStatus.CANCELED.name, mediaId, conversationId)
                    }
                }
            }
    }

package one.mixin.android.ui.media.pager.overlay

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediaOverlayEntryPoint {
    fun conversationRepository(): ConversationRepository

    fun jobManager(): MixinJobManager
}

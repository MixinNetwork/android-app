package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.work.RefreshStickerAlbumWorker

@Subcomponent
interface RefreshStickerAlbumWorkerSubcomponent : AndroidInjector<RefreshStickerAlbumWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshStickerAlbumWorker>()
}
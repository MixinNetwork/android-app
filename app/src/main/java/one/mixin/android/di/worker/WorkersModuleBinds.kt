package one.mixin.android.di.worker

import androidx.work.WorkerFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import one.mixin.android.worker.DownloadAvatarWorker
import one.mixin.android.worker.GenerateAvatarWorker
import one.mixin.android.worker.RefreshAccountWorker
import one.mixin.android.worker.RefreshAddressWorker
import one.mixin.android.worker.RefreshAssetsWorker
import one.mixin.android.worker.RefreshContactWorker
import one.mixin.android.worker.RefreshFcmWorker
import one.mixin.android.worker.RefreshStickerAlbumWorker
import one.mixin.android.worker.RefreshStickerWorker
import one.mixin.android.worker.RefreshTopAssetsWorker
import one.mixin.android.worker.RefreshUserWorker
import one.mixin.android.worker.RemoveStickersWorker

@Module
abstract class WorkersModuleBinds {
    @Binds
    @IntoMap
    @WorkerKey(RefreshAssetsWorker::class)
    abstract fun bindRefreshAssetsWorker(factory: RefreshAssetsWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshAccountWorker::class)
    abstract fun bindRefreshAccountWorker(factory: RefreshAccountWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshFcmWorker::class)
    abstract fun bindRefreshFcmWorker(factory: RefreshFcmWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(DownloadAvatarWorker::class)
    abstract fun bindDownloadAvatarWorker(factory: DownloadAvatarWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshContactWorker::class)
    abstract fun bindRefreshContactWorker(factory: RefreshContactWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(GenerateAvatarWorker::class)
    abstract fun bindGenerateAvatarWorker(factory: GenerateAvatarWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshAddressWorker::class)
    abstract fun bindRefreshAddressWorker(factory: RefreshAddressWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshStickerAlbumWorker::class)
    abstract fun bindRefreshStickerAlbumWorker(factory: RefreshStickerAlbumWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshStickerWorker::class)
    abstract fun bindRefreshStickerWorker(factory: RefreshStickerWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshTopAssetsWorker::class)
    abstract fun bindRefreshTopAssetsWorker(factory: RefreshTopAssetsWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RefreshUserWorker::class)
    abstract fun bindRefreshUserWorker(factory: RefreshUserWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RemoveStickersWorker::class)
    abstract fun bindRemoveStickersWorker(factory: RemoveStickersWorker.Factory): ChildWorkerFactory

    @Binds
    @IntoSet
    abstract fun provideMixinWorkerFactory(factory: MixinWorkerFactory): WorkerFactory
}

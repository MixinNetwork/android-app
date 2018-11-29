package one.mixin.android.di.worker

import androidx.work.Worker
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import one.mixin.android.worker.GenerateAvatarWorker
import one.mixin.android.worker.RefreshAccountWorker
import one.mixin.android.worker.RefreshAddressWorker
import one.mixin.android.worker.RefreshAssetsWorker
import one.mixin.android.worker.RefreshContactWorker
import one.mixin.android.worker.RefreshConversationWorker
import one.mixin.android.worker.RefreshFcmWorker
import one.mixin.android.worker.RefreshSnapshotsWorker
import one.mixin.android.worker.RefreshStickerAlbumWorker
import one.mixin.android.worker.RefreshStickerWorker
import one.mixin.android.worker.RefreshTopAssetsWorker
import one.mixin.android.worker.RefreshUserSnapshotsWorker
import one.mixin.android.worker.RefreshUserWorker
import one.mixin.android.worker.RemoveStickersWorker

@Module(
    subcomponents = [
        GenerateAvatarWorkerSubcomponent::class,
        RefreshAccountWorkerSubcomponent::class,
        RefreshContactWorkerSubcomponent::class,
        RefreshAssetsWorkerSubcomponent::class,
        RefreshFcmWorkerSubcomponent::class,
        RefreshStickerAlbumWorkerSubcomponent::class,
        RefreshSnapshotsWorkerSubcomponent::class,
        RefreshUserSnapshotsWorkerSubcomponent::class,
        RefreshAddressWorkerSubcomponent::class,
        RefreshStickerWorkerSubcomponent::class,
        RefreshConversationWorkerSubcomponent::class,
        RefreshTopAssetsWorkerSubcomponent::class,
        RemoveStickersWorkerSubcomponent::class,
        RefreshUserWorkerSubcomponent::class]
)
abstract class WorkerModule {
    @Binds
    @IntoMap
    @WorkerKey(GenerateAvatarWorker::class)
    abstract fun bindGenerateAvatarWorker(builder: GenerateAvatarWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshAccountWorker::class)
    abstract fun bindRefreshAccountWorker(builder: RefreshAccountWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshContactWorker::class)
    abstract fun bindRefreshContactWorker(builder: RefreshContactWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshAssetsWorker::class)
    abstract fun bindRefreshAssertsWorker(builder: RefreshAssetsWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshFcmWorker::class)
    abstract fun bindRefreshFcmsWorker(builder: RefreshFcmWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshStickerAlbumWorker::class)
    abstract fun bindRefreshStickerAlbumWorker(builder: RefreshStickerAlbumWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshSnapshotsWorker::class)
    abstract fun bindRefreshSnapshotsWorker(builder: RefreshSnapshotsWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshUserSnapshotsWorker::class)
    abstract fun bindRefreshUserSnapshotsWorker(builder: RefreshUserSnapshotsWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshAddressWorker::class)
    abstract fun bindRefreshAddressWorker(builder: RefreshAddressWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshStickerWorker::class)
    abstract fun bindRefreshStickerWorker(builder: RefreshStickerWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshConversationWorker::class)
    abstract fun bindRefreshConversationWorker(builder: RefreshConversationWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshTopAssetsWorker::class)
    abstract fun bindRefreshTopAssetsWorker(builder: RefreshTopAssetsWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RemoveStickersWorker::class)
    abstract fun bindRemoveStickersWorker(builder: RemoveStickersWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>

    @Binds
    @IntoMap
    @WorkerKey(RefreshUserWorker::class)
    abstract fun bindRefreshUserWorker(builder: RefreshUserWorkerSubcomponent.Builder): AndroidInjector.Factory<out Worker>
}
package one.mixin.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import javax.inject.Singleton

@Module
internal class BaseDbModule {

    @Singleton
    @Provides
    fun provideSignalDb(app: Application) = SignalDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideRatchetSenderKeyDao(db: SignalDatabase) = db.ratchetSenderKeyDao()

    @Singleton
    @Provides
    @DatabaseCategory(DatabaseCategoryEnum.BASE)
    fun provideDb(app: Application) = MixinDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideUserDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.userDao()

    @Singleton
    @Provides
    @DatabaseCategory(DatabaseCategoryEnum.BASE)
    fun provideConversationDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.conversationDao()

    @Singleton
    @Provides
    @DatabaseCategory(DatabaseCategoryEnum.BASE)
    fun provideMessageDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.messageDao()

    @Singleton
    @Provides
    fun provideParticipantDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.participantDao()

    @Singleton
    @Provides
    fun provideOffsetDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.offsetDao()

    @Singleton
    @Provides
    fun provideAssetDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.assetDao()

    @Singleton
    @Provides
    fun provideSnapshotDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.snapshotDao()

    @Singleton
    @Provides
    fun provideMessageHistoryDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.messageHistoryDao()

    @Singleton
    @Provides
    fun provideSentSenderKeyDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.sentSenderKeyDao()

    @Singleton
    @Provides
    fun provideStickerAlbumDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.stickerAlbumDao()

    @Singleton
    @Provides
    fun provideStickerDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.stickerDao()

    @Singleton
    @Provides
    fun provideHyperlinkDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.hyperlinkDao()

    @Singleton
    @Provides
    fun providesAppDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.appDao()

    @Singleton
    @Provides
    fun providesFloodMessageDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.floodMessageDao()

    @Singleton
    @Provides
    fun providesJobDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.jobDao()

    @Singleton
    @Provides
    fun providesAddressDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.addressDao()

    @Singleton
    @Provides
    fun providesResendMessageDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.resendMessageDao()

    @Singleton
    @Provides
    fun providesStickerRelationshipDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.stickerRelationshipDao()

    @Singleton
    @Provides
    fun providesHotAssetDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.topAssetDao()
}

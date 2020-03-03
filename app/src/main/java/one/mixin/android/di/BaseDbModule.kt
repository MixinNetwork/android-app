package one.mixin.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum

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
    fun provideSessionParticipantDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.participantSessionDao()

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
    fun provideAssetDisplyDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.assetsExtraDao()

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
    fun providesResendSessionMessageDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.resendSessionMessageDao()

    @Singleton
    @Provides
    fun providesStickerRelationshipDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.stickerRelationshipDao()

    @Singleton
    @Provides
    fun providesHotAssetDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.topAssetDao()

    @Singleton
    @Provides
    fun providesFavoriteAppDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.favoriteAppDao()

    @Singleton
    @Provides
    fun providesMentionMessageDao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.mentionMessageDao()

    @Singleton
    @Provides
    fun providesMessageFts4Dao(@DatabaseCategory(DatabaseCategoryEnum.BASE) db: MixinDatabase) = db.messageFts4Dao()
}

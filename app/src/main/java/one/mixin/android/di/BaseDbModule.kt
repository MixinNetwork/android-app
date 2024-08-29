package one.mixin.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.fts.FtsDatabase
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object BaseDbModule {
    @Singleton
    @Provides
    fun provideSignalDb(app: Application) = SignalDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideFtsDb(app: Application) = FtsDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideRatchetSenderKeyDao(db: SignalDatabase) = db.ratchetSenderKeyDao()

    @Singleton
    @Provides
    fun provideDb(app: Application) = MixinDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun providePendingDatabase(
        app: Application,
        mixinDatabase: MixinDatabase,
    ): PendingDatabase = PendingDatabaseImp.getDatabase(app.applicationContext, mixinDatabase.floodMessageDao(), mixinDatabase.jobDao())

    @Singleton
    @Provides
    fun provideUserDao(db: MixinDatabase) = db.userDao()

    @Singleton
    @Provides
    fun provideSessionParticipantDao(db: MixinDatabase) = db.participantSessionDao()

    @Singleton
    @Provides
    fun provideConversationDao(db: MixinDatabase) = db.conversationDao()

    @Singleton
    @Provides
    fun provideMessageDao(db: MixinDatabase) = db.messageDao()

    @Singleton
    @Provides
    fun providePendingMessageDao(pendingDatabase: PendingDatabase) =
        pendingDatabase.pendingMessageDao()

    @Singleton
    @Provides
    fun provideParticipantDao(db: MixinDatabase) = db.participantDao()

    @Singleton
    @Provides
    fun provideOffsetDao(db: MixinDatabase) = db.offsetDao()

    @Singleton
    @Provides
    fun provideAssetDao(db: MixinDatabase) = db.assetDao()

    @Singleton
    @Provides
    fun provideTokenDao(db: MixinDatabase) = db.tokenDao()

    @Singleton
    @Provides
    fun provideTokensExtraDao(db: MixinDatabase) = db.tokensExtraDao()

    @Singleton
    @Provides
    fun provideSnapshotDao(db: MixinDatabase) = db.snapshotDao()

    @Singleton
    @Provides
    fun provideSafeSnapshotDao(db: MixinDatabase) = db.safeSnapshotDao()

    @Singleton
    @Provides
    fun provideMessageHistoryDao(db: MixinDatabase) = db.messageHistoryDao()

    @Singleton
    @Provides
    fun provideStickerAlbumDao(db: MixinDatabase) = db.stickerAlbumDao()

    @Singleton
    @Provides
    fun provideStickerDao(db: MixinDatabase) = db.stickerDao()

    @Singleton
    @Provides
    fun provideHyperlinkDao(db: MixinDatabase) = db.hyperlinkDao()

    @Singleton
    @Provides
    fun providesAppDao(db: MixinDatabase) = db.appDao()

    @Singleton
    @Provides
    fun providesFloodMessageDao(db: PendingDatabase) = db.floodMessageDao()

    @Singleton
    @Provides
    fun providesJobDao(db: PendingDatabase) = db.jobDao()

    @Singleton
    @Provides
    fun providesAddressDao(db: MixinDatabase) = db.addressDao()

    @Singleton
    @Provides
    fun providesResendSessionMessageDao(db: MixinDatabase) = db.resendSessionMessageDao()

    @Singleton
    @Provides
    fun providesStickerRelationshipDao(db: MixinDatabase) = db.stickerRelationshipDao()

    @Singleton
    @Provides
    fun providesHotAssetDao(db: MixinDatabase) = db.topAssetDao()

    @Singleton
    @Provides
    fun providesFavoriteAppDao(db: MixinDatabase) = db.favoriteAppDao()

    @Singleton
    @Provides
    fun providesMentionMessageDao(db: MixinDatabase) = db.mentionMessageDao()

    @Singleton
    @Provides
    fun providesCircleDao(db: MixinDatabase) = db.circleDao()

    @Singleton
    @Provides
    fun providesCircleConversationDao(db: MixinDatabase) = db.circleConversationDao()

    @Singleton
    @Provides
    fun providesTraceDao(db: MixinDatabase) = db.traceDao()

    @Singleton
    @Provides
    fun providesTranscriptDao(db: MixinDatabase) = db.transcriptDao()

    @Singleton
    @Provides
    fun providesPinMessageDao(db: MixinDatabase) = db.pinMessageDao()

    @Singleton
    @Provides
    fun providesPropertyDao(db: MixinDatabase) = db.propertyDao()

    @Singleton
    @Provides
    fun providesRemoteMessageStatusDao(db: MixinDatabase) = db.remoteMessageStatusDao()

    @Singleton
    @Provides
    fun providesExpiredMessageDao(db: MixinDatabase) = db.expiredMessageDao()

    @Singleton
    @Provides
    fun providesConversationExtDao(db: MixinDatabase) = db.conversationExtDao()

    @Singleton
    @Provides
    fun providesChainDao(db: MixinDatabase) = db.chainDao()

    @Singleton
    @Provides
    fun provideOutputDao(db: MixinDatabase) = db.outputDao()

    @Singleton
    @Provides
    fun provideDepositDao(db: MixinDatabase) = db.depositDao()

    @Singleton
    @Provides
    fun provideRawTransactionDao(db: MixinDatabase) = db.rawTransactionDao()

    @Singleton
    @Provides
    fun provideInscriptionCollectionDao(db: MixinDatabase) = db.inscriptionCollectionDao()

    @Singleton
    @Provides
    fun provideInscriptionDao(db: MixinDatabase) = db.inscriptionDao()

    @Singleton
    @Provides
    fun provideHistoryPriceDao(db: MixinDatabase) = db.historyPriceDao()

    @Singleton
    @Provides
    fun provideMarketCoinDao(db: MixinDatabase) = db.marketCoinDao()

    @Singleton
    @Provides
    fun provideMarketIdsDao(db: MixinDatabase) = db.marketIdsDao()

    @Singleton
    @Provides
    fun provideMarketFavoredDao(db: MixinDatabase) = db.marketFavoredDao()

    @Singleton
    @Provides
    fun provideGlobalMarketDao(db: MixinDatabase) = db.globalMarketDao()

}

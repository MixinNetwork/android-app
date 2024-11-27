package one.mixin.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.fts.FtsDatabase
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object DatabaseModule {
    @Singleton
    @Provides
    fun provideSignalDb(app: Application) = SignalDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideRatchetSenderKeyDao(db: SignalDatabase) = db.ratchetSenderKeyDao()

    @Singleton
    @Provides
    fun provideDatabaseProvider(app: Application): DatabaseProvider {
        return DatabaseProvider(app)
    }

    @Provides
    fun provideAppDatabase(
        databaseProvider: DatabaseProvider
    ): MixinDatabase {
        return databaseProvider.getMixinDatabase()
    }

    @Provides
    fun provideFtsDatabase(
        databaseProvider: DatabaseProvider
    ): FtsDatabase {
        return databaseProvider.getFtsDatabase()
    }

    @Provides
    fun providePendingDatabase(
        databaseProvider: DatabaseProvider
    ): PendingDatabase {
        return databaseProvider.getPendingDatabase()
    }

    @Provides
    fun provideUserDao(db: MixinDatabase) = db.userDao()

    @Provides
    fun provideSessionParticipantDao(db: MixinDatabase) = db.participantSessionDao()

    @Provides
    fun provideConversationDao(db: MixinDatabase) = db.conversationDao()

    @Provides
    fun provideMessageDao(db: MixinDatabase) = db.messageDao()

    @Provides
    fun providePendingMessageDao(pendingDatabase: PendingDatabase) =
        pendingDatabase.pendingMessageDao()

    @Provides
    fun provideParticipantDao(db: MixinDatabase) = db.participantDao()

    @Provides
    fun provideOffsetDao(db: MixinDatabase) = db.offsetDao()

    @Provides
    fun provideAssetDao(db: MixinDatabase) = db.assetDao()

    @Provides
    fun provideTokenDao(db: MixinDatabase) = db.tokenDao()

    @Provides
    fun provideTokensExtraDao(db: MixinDatabase) = db.tokensExtraDao()

    @Provides
    fun provideSnapshotDao(db: MixinDatabase) = db.snapshotDao()

    @Provides
    fun provideSafeSnapshotDao(db: MixinDatabase) = db.safeSnapshotDao()

    @Provides
    fun provideMessageHistoryDao(db: MixinDatabase) = db.messageHistoryDao()

    @Provides
    fun provideStickerAlbumDao(db: MixinDatabase) = db.stickerAlbumDao()

    @Provides
    fun provideStickerDao(db: MixinDatabase) = db.stickerDao()

    @Provides
    fun provideHyperlinkDao(db: MixinDatabase) = db.hyperlinkDao()

    @Provides
    fun providesAppDao(db: MixinDatabase) = db.appDao()

    @Provides
    fun providesFloodMessageDao(db: PendingDatabase) = db.floodMessageDao()

    @Provides
    fun providesJobDao(db: PendingDatabase) = db.jobDao()

    @Provides
    fun providesAddressDao(db: MixinDatabase) = db.addressDao()

    @Provides
    fun providesResendSessionMessageDao(db: MixinDatabase) = db.resendSessionMessageDao()

    @Provides
    fun providesStickerRelationshipDao(db: MixinDatabase) = db.stickerRelationshipDao()

    @Provides
    fun providesHotAssetDao(db: MixinDatabase) = db.topAssetDao()

    @Provides
    fun providesFavoriteAppDao(db: MixinDatabase) = db.favoriteAppDao()

    @Provides
    fun providesMentionMessageDao(db: MixinDatabase) = db.messageMentionDao()

    @Provides
    fun providesCircleDao(db: MixinDatabase) = db.circleDao()

    @Provides
    fun providesCircleConversationDao(db: MixinDatabase) = db.circleConversationDao()

    @Provides
    fun providesTraceDao(db: MixinDatabase) = db.traceDao()

    @Provides
    fun providesTranscriptDao(db: MixinDatabase) = db.transcriptDao()

    @Provides
    fun providesPinMessageDao(db: MixinDatabase) = db.pinMessageDao()

    @Provides
    fun providesPropertyDao(db: MixinDatabase) = db.propertyDao()

    @Provides
    fun providesRemoteMessageStatusDao(db: MixinDatabase) = db.remoteMessageStatusDao()

    @Provides
    fun providesExpiredMessageDao(db: MixinDatabase) = db.expiredMessageDao()

    @Provides
    fun providesConversationExtDao(db: MixinDatabase) = db.conversationExtDao()

    @Provides
    fun providesChainDao(db: MixinDatabase) = db.chainDao()

    @Provides
    fun provideOutputDao(db: MixinDatabase) = db.outputDao()

    @Provides
    fun provideDepositDao(db: MixinDatabase) = db.depositDao()

    @Provides
    fun provideRawTransactionDao(db: MixinDatabase) = db.rawTransactionDao()

    @Provides
    fun provideInscriptionCollectionDao(db: MixinDatabase) = db.inscriptionCollectionDao()

    @Provides
    fun provideInscriptionDao(db: MixinDatabase) = db.inscriptionDao()

    @Provides
    fun provideHistoryPriceDao(db: MixinDatabase) = db.historyPriceDao()

    @Provides
    fun provideMarketDao(db: MixinDatabase) = db.marketDao()

    @Provides
    fun provideMarketCoinDao(db: MixinDatabase) = db.marketCoinDao()

    @Provides
    fun provideMarketFavoredDao(db: MixinDatabase) = db.marketFavoredDao()

    @Provides
    fun provideAlertDao(db: MixinDatabase) = db.alertDao()

    @Provides
    fun provideMarketCapRankDao(db: MixinDatabase) = db.marketCapRankDao()
}
package one.mixin.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.session.CurrentUserScopeManager

@InstallIn(SingletonComponent::class)
@Module
internal object BaseDbModule {
    @Provides
    fun provideSignalDb(app: Application) = SignalDatabase.getDatabase(app)
    @Provides
    fun provideFtsDb(scopeManager: CurrentUserScopeManager) = scopeManager.getFtsDatabase()
    @Provides
    fun provideWalletDatabase(scopeManager: CurrentUserScopeManager) = scopeManager.getWalletDatabase()
    @Provides
    fun provideRatchetSenderKeyDao(db: SignalDatabase) = db.ratchetSenderKeyDao()
    @Provides
    fun provideDb(scopeManager: CurrentUserScopeManager) = scopeManager.getMixinDatabase()
    @Provides
    fun providePendingDatabase(scopeManager: CurrentUserScopeManager): PendingDatabase = scopeManager.getPendingDatabase()
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
    fun providesMentionMessageDao(db: MixinDatabase) = db.mentionMessageDao()
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
    @Provides
    fun provideMemberOrderDao(db: MixinDatabase) = db.memberOrderDao()
    @Provides
    fun provideWeb3TokenDao(db: WalletDatabase) = db.web3TokenDao()
    @Provides
    fun provideWeb3TransactionDao(db: WalletDatabase) = db.web3TransactionDao()
    @Provides
    fun provideWeb3WalletDao(db: WalletDatabase) = db.web3WalletDao()
    @Provides
    fun provideWeb3AddressDao(db: WalletDatabase) = db.web3AddressDao()
    @Provides
    fun provideWeb3TokensExtraDao(db: WalletDatabase) = db.web3TokensExtraDao()
    @Provides
    fun provideWeb3ChainDao(db: WalletDatabase) = db.web3ChainDao()
    @Provides
    fun provideWeb3PropertyDao(db: WalletDatabase) = db.web3PropertyDao()
    @Provides
    fun provideWeb3RawTransactionDao(db: WalletDatabase) = db.web3RawTransactionDao()
    @Provides
    fun provideOrderDao(db: WalletDatabase) = db.orderDao()
    @Provides
    fun provideSafeWalletsDao(db: WalletDatabase) = db.safeWalletsDao()
    @Provides
    fun provideWalletOutputDao(db: WalletDatabase) = db.walletOutputDao()
}

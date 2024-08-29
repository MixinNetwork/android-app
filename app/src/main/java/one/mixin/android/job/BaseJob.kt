package one.mixin.android.job

import com.birbit.android.jobqueue.CancelReason
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.ExpiredTokenException
import one.mixin.android.api.LocalJobException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.WebSocketException
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ContactService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.MessageService
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.TipService
import one.mixin.android.api.service.TokenService
import one.mixin.android.api.service.UserService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.api.service.Web3Service
import one.mixin.android.crypto.EncryptedProtocol
import one.mixin.android.crypto.JobSenderKey
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ChainDao
import one.mixin.android.db.CircleConversationDao
import one.mixin.android.db.CircleDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.FavoriteAppDao
import one.mixin.android.db.GlobalMarketDao
import one.mixin.android.db.HistoryPriceDao
import one.mixin.android.db.HyperlinkDao
import one.mixin.android.db.InscriptionCollectionDao
import one.mixin.android.db.InscriptionDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageHistoryDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.OffsetDao
import one.mixin.android.db.OutputDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.MarketDao
import one.mixin.android.db.PropertyDao
import one.mixin.android.db.RawTransactionDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerAlbumDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TokensExtraDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.MarketFavoredDao
import one.mixin.android.db.MarketCoinDao
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.di.ApplicationScope
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.Tip
import one.mixin.android.util.reportException
import one.mixin.android.vo.LinkState
import one.mixin.android.websocket.ChatWebSocket
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

abstract class BaseJob(params: Params) : Job(params) {
    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface JobEntryPoint {
        fun inject(baseJob: BaseJob)
    }

    @Inject
    @Transient
    lateinit var jobManager: MixinJobManager

    @Inject
    @Transient
    lateinit var ftsDatabase: FtsDatabase

    @Inject
    @Transient
    lateinit var mixinDatabase: MixinDatabase

    @Inject
    @Transient
    lateinit var pendingDatabase: PendingDatabase

    @Inject
    @Transient
    lateinit var conversationApi: ConversationService

    @Inject
    @Transient
    lateinit var userService: UserService

    @Inject
    @Transient
    lateinit var contactService: ContactService

    @Inject
    @Transient
    lateinit var signalKeyService: SignalKeyService

    @Inject
    @Transient
    lateinit var messageService: MessageService

    @Inject
    @Transient
    lateinit var tokenService: TokenService

    @Inject
    @Transient
    lateinit var assetService: AssetService

    @Inject
    @Transient
    lateinit var accountService: AccountService

    @Inject
    @Transient
    lateinit var addressService: AddressService

    @Inject
    @Transient
    lateinit var circleService: CircleService

    @Inject
    @Transient
    lateinit var utxoService: UtxoService

    @Inject
    @Transient
    lateinit var routeService: RouteService

    @Inject
    @Transient
    lateinit var messageDao: MessageDao

    @Inject
    @Transient
    lateinit var messageHistoryDao: MessageHistoryDao

    @Inject
    @Transient
    lateinit var userDao: UserDao

    @Inject
    @Transient
    lateinit var conversationDao: ConversationDao

    @Inject
    @Transient
    lateinit var conversationExtDao: ConversationExtDao

    @Inject
    @Transient
    lateinit var participantDao: ParticipantDao

    @Inject
    @Transient
    lateinit var participantSessionDao: ParticipantSessionDao

    @Inject
    @Transient
    lateinit var offsetDao: OffsetDao

    @Inject
    @Transient
    lateinit var assetDao: AssetDao

    @Inject
    @Transient
    lateinit var tokenDao: TokenDao

    @Inject
    @Transient
    lateinit var tokensExtraDao: TokensExtraDao

    @Inject
    @Transient
    lateinit var snapshotDao: SnapshotDao

    @Inject
    @Transient
    lateinit var chainDao: ChainDao

    @Inject
    @Transient
    lateinit var chatWebSocket: ChatWebSocket

    @Inject
    @Transient
    lateinit var conversationRepo: ConversationRepository

    @Inject
    @Transient
    lateinit var userRepo: UserRepository

    @Inject
    @Transient
    lateinit var assetRepo: TokenRepository

    @Inject
    @Transient
    lateinit var stickerDao: StickerDao

    @Inject
    @Transient
    lateinit var hyperlinkDao: HyperlinkDao

    @Inject
    @Transient
    lateinit var stickerAlbumDao: StickerAlbumDao

    @Inject
    @Transient
    lateinit var stickerRelationshipDao: StickerRelationshipDao

    @Inject
    @Transient
    lateinit var addressDao: AddressDao

    @Inject
    @Transient
    lateinit var topAssetDao: TopAssetDao

    @Inject
    @Transient
    lateinit var jobDao: JobDao

    @Inject
    @Transient
    lateinit var favoriteAppDao: FavoriteAppDao

    @Inject
    @Transient
    lateinit var messageMentionDao: MessageMentionDao

    @Inject
    @Transient
    lateinit var appDao: AppDao

    @Inject
    @Transient
    lateinit var circleDao: CircleDao

    @Inject
    @Transient
    lateinit var circleConversationDao: CircleConversationDao

    @Inject
    @Transient
    lateinit var transcriptMessageDao: TranscriptMessageDao

    @Inject
    @Transient
    lateinit var pinMessageDao: PinMessageDao

    @Inject
    @Transient
    lateinit var propertyDao: PropertyDao

    @Inject
    @Transient
    lateinit var remoteMessageStatusDao: RemoteMessageStatusDao

    @Inject
    @Transient
    lateinit var expiredMessageDao: ExpiredMessageDao

    @Inject
    @Transient
    lateinit var outputDao: OutputDao

    @Inject
    @Transient
    lateinit var rawTransactionDao: RawTransactionDao

    @Inject
    @Transient
    lateinit var safeSnapshotDao: SafeSnapshotDao

    @Inject
    @Transient
    lateinit var inscriptionDao: InscriptionDao

    @Inject
    @Transient
    lateinit var marketDao: MarketDao

    @Inject
    @Transient
    lateinit var marketFavoredDao: MarketFavoredDao

    @Inject
    @Transient
    lateinit var marketIdsDao: MarketCoinDao

    @Inject
    @Transient
    lateinit var globalMarketDao: GlobalMarketDao

    @Inject
    @Transient
    lateinit var historyPriceDao: HistoryPriceDao

    @Inject
    @Transient
    lateinit var inscriptionCollectionDao: InscriptionCollectionDao

    @Inject
    @Transient
    lateinit var signalProtocol: SignalProtocol

    @Inject
    @Transient
    lateinit var encryptedProtocol: EncryptedProtocol

    @Transient
    @Inject
    lateinit var appDatabase: MixinDatabase

    @Transient
    @Inject
    lateinit var linkState: LinkState

    @Transient
    @Inject
    lateinit var tip: Tip

    @Transient
    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    @Inject
    @Transient
    lateinit var tipService: TipService

    @Inject
    @Transient
    lateinit var web3Service: Web3Service

    @ApplicationScope
    @Transient
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Transient
    @Inject
    lateinit var jobSenderKey: JobSenderKey

    open fun shouldRetry(throwable: Throwable): Boolean {
        if (throwable is SocketTimeoutException) {
            return true
        }
        if (throwable is IOException) {
            return true
        }
        if (throwable is InterruptedException) {
            return true
        }
        return (throwable as? ServerErrorException)?.shouldRetry()
            ?: (throwable as? ExpiredTokenException)?.shouldRetry()
            ?: (
                (throwable as? ClientErrorException)?.shouldRetry()
                    ?: (
                        (throwable as? NetworkException)?.shouldRetry()
                            ?: (
                                (throwable as? WebSocketException)?.shouldRetry()
                                    ?: ((throwable as? LocalJobException)?.shouldRetry() ?: false)
                            )
                    )
            )
    }

    public override fun shouldReRunOnThrowable(
        throwable: Throwable,
        runCount: Int,
        maxRunCount: Int,
    ): RetryConstraint {
        if (runCount >= 10) {
            reportException("Job shouldReRunOnThrowable retry max count:$runCount", throwable)
        }
        return if (shouldRetry(throwable)) {
            RetryConstraint.RETRY
        } else {
            RetryConstraint.CANCEL
        }
    }

    override fun onAdded() {
    }

    override fun onCancel(
        cancelReason: Int,
        throwable: Throwable?,
    ) {
        if (cancelReason == CancelReason.REACHED_RETRY_LIMIT) {
            throwable?.let {
                reportException("Job cancelReason REACHED_RETRY_LIMIT", it)
            }
        }
    }

    override fun getRetryLimit(): Int {
        return Integer.MAX_VALUE
    }

    @Suppress("unused")
    companion object {
        private const val serialVersionUID = 1L

        const val PRIORITY_UI_HIGH = 20
        const val PRIORITY_SEND_MESSAGE = 18
        const val PRIORITY_SEND_ATTACHMENT_MESSAGE = 17
        const val PRIORITY_SEND_SESSION_MESSAGE = 16
        const val PRIORITY_RECEIVE_MESSAGE = 15
        const val PRIORITY_BACKGROUND = 10
        const val PRIORITY_DELIVERED_ACK_MESSAGE = 7
        const val PRIORITY_ACK_MESSAGE = 5
        const val PRIORITY_LOWER = 3
        const val PRIORITY_LOWEST = 0
    }
}

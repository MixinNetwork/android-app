package one.mixin.android.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.util.ArrayMap
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.DataBase.CURRENT_VERSION
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_15_16
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_16_17
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_17_18
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_18_19
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_19_20
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_20_21
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_21_22
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_22_23
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_23_24
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_24_25
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_25_26
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_26_27
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_27_28
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_28_29
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_29_30
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_30_31
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_31_32
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_32_33
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_33_34
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_34_35
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_35_36
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_36_37
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_37_38
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_38_39
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_39_40
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_40_41
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_41_42
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_42_43
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_43_44
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_44_45
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_45_46
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_46_47
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_47_48
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_48_49
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_49_50
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_50_51
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_51_52
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_52_53
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_53_54
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_54_55
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_55_56
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_56_57
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_57_58
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_58_59
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_59_60
import one.mixin.android.db.converter.DepositEntryListConverter
import one.mixin.android.db.converter.MembershipConverter
import one.mixin.android.db.converter.MessageStatusConverter
import one.mixin.android.db.converter.OutputStateConverter
import one.mixin.android.db.converter.PriceListConverter
import one.mixin.android.db.converter.RawTransactionTypeConverter
import one.mixin.android.db.converter.SafeDepositConverter
import one.mixin.android.db.converter.SafeWithdrawalConverter
import one.mixin.android.db.converter.TreasuryConverter
import one.mixin.android.db.converter.WithdrawalMemoPossibilityConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_DB_EXECUTOR
import one.mixin.android.util.debug.getContent
import one.mixin.android.util.reportException
import one.mixin.android.vo.Address
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.Chain
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationExt
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.FavoriteApp
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Hyperlink
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageFts4
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Offset
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Property
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.ResendMessage
import one.mixin.android.vo.ResendSessionMessage
import one.mixin.android.vo.SentSenderKey
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.Trace
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.market.HistoryPrice
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.market.MarketFavored
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.RawTransaction
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokensExtra
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

@Database(
    entities = [
        (User::class),
        (Conversation::class),
        (Message::class),
        (Participant::class),
        (ParticipantSession::class),
        (Offset::class),
        (Asset::class),
        (AssetsExtra::class),
        (TokensExtra::class),
        (Snapshot::class),
        (MessageHistory::class),
        (SentSenderKey::class),
        (Sticker::class),
        (StickerAlbum::class),
        (App::class),
        (Hyperlink::class),
        (FloodMessage::class),
        (Address::class),
        (ResendMessage::class),
        (ResendSessionMessage::class),
        (StickerRelationship::class),
        (TopAsset::class),
        (FavoriteApp::class),
        (Job::class),
        (MessageMention::class),
        (MessageFts4::class),
        (Circle::class),
        (CircleConversation::class),
        (Trace::class),
        (TranscriptMessage::class),
        (PinMessage::class),
        (Property::class),
        (RemoteMessageStatus::class),
        (ExpiredMessage::class),
        (ConversationExt::class),
        (Chain::class),
        (Output::class),
        (Token::class),
        (DepositEntry::class),
        (SafeSnapshot::class),
        (RawTransaction::class),
        (InscriptionCollection::class),
        (InscriptionItem::class),
        (Market::class),
        (HistoryPrice::class),
        (MarketCoin::class),
        (MarketFavored::class),
    ],
    version = CURRENT_VERSION,
)
@TypeConverters(MessageStatusConverter::class, DepositEntryListConverter::class, WithdrawalMemoPossibilityConverter::class, SafeDepositConverter::class, SafeWithdrawalConverter::class, RawTransactionTypeConverter::class, OutputStateConverter::class, TreasuryConverter::class, PriceListConverter::class, MembershipConverter::class)
abstract class MixinDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    abstract fun conversationExtDao(): ConversationExtDao

    abstract fun messageDao(): MessageDao

    abstract fun userDao(): UserDao

    abstract fun participantSessionDao(): ParticipantSessionDao

    abstract fun participantDao(): ParticipantDao

    abstract fun offsetDao(): OffsetDao

    abstract fun assetDao(): AssetDao

    abstract fun tokenDao(): TokenDao

    abstract fun tokensExtraDao(): TokensExtraDao

    abstract fun snapshotDao(): SnapshotDao

    abstract fun safeSnapshotDao(): SafeSnapshotDao

    abstract fun messageHistoryDao(): MessageHistoryDao

    abstract fun stickerDao(): StickerDao

    abstract fun stickerAlbumDao(): StickerAlbumDao

    abstract fun appDao(): AppDao

    abstract fun hyperlinkDao(): HyperlinkDao

    abstract fun floodMessageDao(): FloodMessageDao

    abstract fun jobDao(): JobDao

    abstract fun addressDao(): AddressDao

    abstract fun resendSessionMessageDao(): ResendSessionMessageDao

    abstract fun stickerRelationshipDao(): StickerRelationshipDao

    abstract fun topAssetDao(): TopAssetDao

    abstract fun favoriteAppDao(): FavoriteAppDao

    abstract fun mentionMessageDao(): MessageMentionDao

    abstract fun circleDao(): CircleDao

    abstract fun circleConversationDao(): CircleConversationDao

    abstract fun traceDao(): TraceDao

    abstract fun transcriptDao(): TranscriptMessageDao

    abstract fun pinMessageDao(): PinMessageDao

    abstract fun remoteMessageStatusDao(): RemoteMessageStatusDao

    abstract fun propertyDao(): PropertyDao

    abstract fun expiredMessageDao(): ExpiredMessageDao

    abstract fun chainDao(): ChainDao

    abstract fun outputDao(): OutputDao

    abstract fun depositDao(): DepositDao

    abstract fun rawTransactionDao(): RawTransactionDao

    abstract fun inscriptionCollectionDao(): InscriptionCollectionDao

    abstract fun inscriptionDao(): InscriptionDao

    abstract fun historyPriceDao(): HistoryPriceDao

    abstract fun marketDao(): MarketDao

    abstract fun marketCoinDao(): MarketCoinDao

    abstract fun marketFavoredDao(): MarketFavoredDao

    companion object {
        private var INSTANCE: MixinDatabase? = null

        private val lock = Any()
        private var supportSQLiteDatabase: SupportSQLiteDatabase? = null

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        @SuppressLint("RestrictedApi")
        fun getDatabase(context: Context): MixinDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder =
                        Room.databaseBuilder(context, MixinDatabase::class.java, DB_NAME)
                            .openHelperFactory(
                                MixinOpenHelperFactory(
                                    FrameworkSQLiteOpenHelperFactory(),
                                    listOf(
                                        object : MixinCorruptionCallback {
                                            override fun onCorruption(database: SupportSQLiteDatabase) {
                                                val e = IllegalStateException("Mixin database is corrupted, current DB version: $CURRENT_VERSION")
                                                reportException(e)
                                            }
                                        },
                                    ),
                                ),
                            )
                            .addMigrations(
                                MIGRATION_15_16,
                                MIGRATION_16_17,
                                MIGRATION_17_18,
                                MIGRATION_18_19,
                                MIGRATION_19_20,
                                MIGRATION_20_21,
                                MIGRATION_21_22,
                                MIGRATION_22_23,
                                MIGRATION_23_24,
                                MIGRATION_24_25,
                                MIGRATION_25_26,
                                MIGRATION_26_27,
                                MIGRATION_27_28,
                                MIGRATION_28_29,
                                MIGRATION_29_30,
                                MIGRATION_30_31,
                                MIGRATION_31_32,
                                MIGRATION_32_33,
                                MIGRATION_33_34,
                                MIGRATION_34_35,
                                MIGRATION_35_36,
                                MIGRATION_36_37,
                                MIGRATION_37_38,
                                MIGRATION_38_39,
                                MIGRATION_39_40,
                                MIGRATION_40_41,
                                MIGRATION_41_42,
                                MIGRATION_42_43,
                                MIGRATION_43_44,
                                MIGRATION_44_45,
                                MIGRATION_45_46,
                                MIGRATION_46_47,
                                MIGRATION_47_48,
                                MIGRATION_48_49,
                                MIGRATION_49_50,
                                MIGRATION_50_51,
                                MIGRATION_51_52,
                                MIGRATION_52_53,
                                MIGRATION_53_54,
                                MIGRATION_54_55,
                                MIGRATION_55_56,
                                MIGRATION_56_57,
                                MIGRATION_57_58,
                                MIGRATION_58_59,
                                MIGRATION_59_60,
                            )
                            .enableMultiInstanceInvalidation()
                            .setQueryExecutor(
                                Executors.newFixedThreadPool(
                                    max(
                                        2,
                                        min(Runtime.getRuntime().availableProcessors() - 1, 4),
                                    ),
                                ),
                            )
                            .setTransactionExecutor(SINGLE_DB_EXECUTOR)
                            .addCallback(CALLBACK)
                    if (BuildConfig.DEBUG) {
                        builder.setQueryCallback(
                            object : QueryCallback {
                                override fun onQuery(
                                    sqlQuery: String,
                                    bindArgs: List<Any?>,
                                ) {
                                    DatabaseMonitor.monitor(sqlQuery, bindArgs)
                                }
                            },
                            ArchTaskExecutor.getIOThreadExecutor(),
                        )
                    }
                    INSTANCE = builder.build()
                }
                return INSTANCE as MixinDatabase
            }
        }

        fun query(query: String): String? {
            val start = System.currentTimeMillis()
            var cursor: Cursor? = null
            try {
                cursor =
                    supportSQLiteDatabase?.query(query) ?: return null
                cursor.moveToFirst()
                val result = ArrayList<ArrayMap<String, String>>()
                do {
                    val map = ArrayMap<String, String>()
                    for (i in 0 until cursor.columnCount) {
                        map[cursor.getColumnName(i)] = cursor.getContent(i)
                    }
                    result.add(map)
                } while (cursor.moveToNext())
                return "${GsonHelper.customGson.toJson(result)} ${System.currentTimeMillis() - start}ms"
            } catch (e: Exception) {
                return e.message
            } finally {
                cursor?.close()
            }
        }

        fun checkPoint() {
            supportSQLiteDatabase?.query("PRAGMA wal_checkpoint(FULL)")?.close()
        }

        fun getWritableDatabase(): SupportSQLiteDatabase? {
            return INSTANCE?.openHelper?.writableDatabase
        }

        private val CALLBACK =
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    supportSQLiteDatabase = db
                    db.execSQL("PRAGMA synchronous = NORMAL")
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_count_insert")
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_insert")
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_last_message_update")
                    db.execSQL("DROP TRIGGER IF EXISTS conversation_last_message_delete")
                }
            }
    }
}

fun runInTransaction(block: () -> Unit) {
    MixinDatabase.getDatabase(MixinApplication.appContext).runInTransaction(block)
}

suspend fun withTransaction(block: suspend () -> Unit) {
    MixinDatabase.getDatabase(MixinApplication.appContext).withTransaction(block)
}

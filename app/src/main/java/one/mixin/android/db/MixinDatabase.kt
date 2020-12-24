package one.mixin.android.db

import android.annotation.SuppressLint
import android.content.Context
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
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
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_34_35
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_33_34
import one.mixin.android.db.MixinDatabaseMigrations.Companion.MIGRATION_35_34
import one.mixin.android.vo.Address
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.FavoriteApp
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.Hyperlink
import one.mixin.android.vo.Job
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageFts4
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Offset
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.ResendMessage
import one.mixin.android.vo.ResendSessionMessage
import one.mixin.android.vo.SentSenderKey
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.Trace
import one.mixin.android.vo.User

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
        (Trace::class)
    ],
    version = CURRENT_VERSION
)
abstract class MixinDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun participantSessionDao(): ParticipantSessionDao
    abstract fun participantDao(): ParticipantDao
    abstract fun offsetDao(): OffsetDao
    abstract fun assetDao(): AssetDao
    abstract fun assetsExtraDao(): AssetsExtraDao
    abstract fun snapshotDao(): SnapshotDao
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
    abstract fun messageFts4Dao(): MessagesFts4Dao
    abstract fun circleDao(): CircleDao
    abstract fun circleConversationDao(): CircleConversationDao
    abstract fun traceDao(): TraceDao

    companion object {
        private var INSTANCE: MixinDatabase? = null

        private val lock = Any()
        private var supportSQLiteDatabase: SupportSQLiteDatabase? = null

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        @SuppressLint("RestrictedApi")
        fun getDatabase(context: Context): MixinDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    val builder = Room.databaseBuilder(context, MixinDatabase::class.java, DB_NAME)
                        .addMigrations(
                            MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                            MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28,
                            MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35,
                            MIGRATION_35_34
                        )
                        .enableMultiInstanceInvalidation()
                        .addCallback(CALLBACK)
                    if (BuildConfig.DEBUG) {
                        builder.setQueryCallback(
                            { sqlQuery, bindArgs ->
                                // Timber.d(sqlQuery)
                            },
                            ArchTaskExecutor.getIOThreadExecutor()
                        )
                    }
                    INSTANCE = builder.build()
                }
                return INSTANCE as MixinDatabase
            }
        }

        fun checkPoint() {
            supportSQLiteDatabase?.query("PRAGMA wal_checkpoint(FULL)")?.close()
        }

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS conversation_last_message_update AFTER INSERT ON messages BEGIN UPDATE conversations SET last_message_id = new.id WHERE conversation_id = new.conversation_id; END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS conversation_last_message_delete AFTER DELETE ON messages BEGIN UPDATE conversations SET last_message_id = (select id from messages where conversation_id = old.conversation_id order by created_at DESC limit 1) WHERE conversation_id = old.conversation_id; END"
                )
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                supportSQLiteDatabase = db
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS conversation_last_message_update AFTER INSERT ON messages BEGIN UPDATE conversations SET last_message_id = new.id WHERE conversation_id = new.conversation_id; END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS conversation_last_message_delete AFTER DELETE ON messages BEGIN UPDATE conversations SET last_message_id = (select id from messages where conversation_id = old.conversation_id order by created_at DESC limit 1) WHERE conversation_id = old.conversation_id; END"
                )
                db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_count_insert")
                db.execSQL("DROP TRIGGER IF EXISTS conversation_unseen_message_count_insert")
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

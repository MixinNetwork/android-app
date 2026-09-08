package one.mixin.android.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.fts.FtsDatabase
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Room3DatabaseCompatibilityTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun mainDatabasePreservesLegacyRawTransaction() {
        verifyLegacyDatabase(
            MixinDatabase::class.java,
            "INSERT INTO raw_transactions(request_id, raw_transaction, receiver_id, type, state, created_at) VALUES('request', 'signed-main', 'receiver', 1, 'pending', '2026-09-08T00:00:00Z')",
            "SELECT request_id || ':' || raw_transaction FROM raw_transactions",
            "request:signed-main",
        )
    }

    @Test
    fun walletDatabasePreservesLegacyRawTransaction() {
        verifyLegacyDatabase(
            WalletDatabase::class.java,
            "INSERT INTO raw_transactions(hash, chain_id, account, nonce, raw, state, created_at, updated_at) VALUES('hash', 'chain', 'account', '7', 'signed-wallet', 'pending', '2026-09-08T00:00:00Z', '2026-09-08T00:00:00Z')",
            "SELECT hash || ':' || raw || ':' || nonce FROM raw_transactions",
            "hash:signed-wallet:7",
        )
    }

    @Test
    fun walletDatabaseAddsMarketCoinsWithoutLosingRawTransaction() {
        verifyLegacyDatabase(
            WalletDatabase::class.java,
            "INSERT INTO raw_transactions(hash, chain_id, account, nonce, raw, state, created_at, updated_at) VALUES('hash', 'chain', 'account', '7', 'signed-wallet', 'pending', '2026-09-08T00:00:00Z', '2026-09-08T00:00:00Z')",
            "SELECT hash || ':' || raw || ':' || (SELECT COUNT(*) FROM market_coins) FROM raw_transactions",
            "hash:signed-wallet:0",
            legacyVersion = 8,
            migrations = listOf(WalletDatabase.MIGRATION_8_9),
        )
    }

    @Test
    fun perpsDatabasePreservesLegacyFavorite() {
        verifyLegacyDatabase(
            PerpsDatabase::class.java,
            "INSERT INTO favorites(market_id, is_favored, created_at) VALUES('market', 1, '2026-09-08T00:00:00Z')",
            "SELECT market_id || ':' || is_favored FROM favorites",
            "market:1",
        )
    }

    @Test
    fun signalDatabasePreservesLegacyKeyBytes() {
        verifyLegacyDatabase(
            SignalDatabase::class.java,
            "INSERT INTO prekeys(prekey_id, record) VALUES(17, X'0001FF80')",
            "SELECT prekey_id || ':' || hex(record) FROM prekeys",
            "17:0001FF80",
        )
    }

    @Test
    fun pendingDatabasePreservesLegacyMessage() {
        verifyLegacyDatabase(
            PendingDatabaseImp::class.java,
            "INSERT INTO flood_messages(message_id, data, created_at) VALUES('message', 'pending-payload', '2026-09-08T00:00:00Z')",
            "SELECT message_id || ':' || data FROM flood_messages",
            "message:pending-payload",
            schemaName = "one.mixin.android.db.pending.PendingDataBaseImp",
        )
    }

    @Test
    fun ftsDatabasePreservesLegacySearchIndex() {
        verifyLegacyDatabase(
            FtsDatabase::class.java,
            "INSERT INTO messages_fts(content) VALUES('retained searchable content')",
            "SELECT content FROM messages_fts WHERE content MATCH 'searchable'",
            "retained searchable content",
        )
    }

    private fun <T : RoomDatabase> verifyLegacyDatabase(
        databaseClass: Class<T>,
        insertSql: String,
        selectSql: String,
        expectedValue: String,
        schemaName: String = databaseClass.name,
        legacyVersion: Int? = null,
        migrations: List<Migration> = emptyList(),
    ) {
        val schemaDirectory = listOf(File("schemas/googlePlay/$schemaName"), File("app/schemas/googlePlay/$schemaName"))
            .firstOrNull(File::isDirectory)
        requireNotNull(schemaDirectory) { "Missing exported schema for $schemaName in ${File(".").absolutePath}" }
        val schemaFile = requireNotNull(schemaDirectory.listFiles()?.filter { it.extension == "json" }?.maxByOrNull { it.nameWithoutExtension.toInt() })
        val currentSchema = JsonParser.parseString(schemaFile.readText()).asJsonObject.getAsJsonObject("database")
        val schema = if (legacyVersion == null) currentSchema else
            JsonParser.parseString(File(schemaDirectory, "$legacyVersion.json").readText()).asJsonObject.getAsJsonObject("database")
        val version = currentSchema.get("version").asInt
        val identityHash = currentSchema.get("identityHash").asString
        val file = File(temporaryFolder.root, "${databaseClass.simpleName}.db")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { legacy ->
            schema.getAsJsonArray("entities").forEach { entityElement ->
                val entity = entityElement.asJsonObject
                val tableName = entity.get("tableName").asString
                legacy.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", tableName))
                entity.getAsJsonArray("indices")?.forEach { index ->
                    legacy.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", tableName))
                }
                entity.getAsJsonArray("contentSyncTriggers")?.forEach { trigger -> legacy.execSQL(trigger.asString) }
            }
            schema.getAsJsonArray("views")?.forEach { viewElement ->
                val view = viewElement.asJsonObject
                legacy.execSQL(view.get("createSql").asString.replace("\${VIEW_NAME}", view.get("viewName").asString))
            }
            schema.getAsJsonArray("setupQueries").forEach { legacy.execSQL(it.asString) }
            legacy.version = schema.get("version").asInt
            legacy.execSQL(insertSql)
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        repeat(2) {
            val database = Room.databaseBuilder(context, databaseClass, file.absolutePath)
                .setDriver(ReportingAndroidSQLiteDriver(databaseClass.simpleName, version))
                .allowMainThreadQueries()
                .addMigrations(*migrations.toTypedArray())
                .build()
            try {
                assertScalar(database, selectSql, expectedValue)
                assertScalar(database, "SELECT identity_hash FROM room_master_table WHERE id = 42", identityHash)
                assertScalar(database, "PRAGMA user_version", version.toString())
                assertScalar(database, "PRAGMA integrity_check", "ok")
                RoomDatabaseCompat.query(database, "PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            } finally {
                database.close()
            }
        }
    }

    private fun assertScalar(database: RoomDatabase, sql: String, expected: String) {
        RoomDatabaseCompat.query(database, sql).use { cursor ->
            assertTrue(cursor.moveToFirst(), sql)
            assertEquals(expected, cursor.getString(0), sql)
            assertFalse(cursor.moveToNext(), sql)
        }
    }
}

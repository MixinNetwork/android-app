package one.mixin.android.db

import android.content.Context
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.session.Session
import javax.inject.Inject

class DatabaseProvider @Inject constructor(
    private val context: Context,
) {
    private var mixinDatabase: MixinDatabase? = null
    private var ftsDatabase: FtsDatabase? = null
    private var pendingDatabase: PendingDatabaseImp? = null
    private var identityNumber: String? = null

    @Synchronized
    fun getMixinDatabase(): MixinDatabase {
        if (mixinDatabase == null) {
            val db = MixinDatabase.getDatabase(context)
            mixinDatabase = db
        }
        return mixinDatabase ?: throw IllegalStateException("MixinDatabase is not initialized")
    }

    @Synchronized
    fun getFtsDatabase(): FtsDatabase {
        if (ftsDatabase == null) {
            val db = FtsDatabase.getDatabase(context)
            ftsDatabase = db
        }
        return ftsDatabase ?: throw IllegalStateException("FtsDatabase is not initialized")
    }

    @Synchronized
    fun getPendingDatabase(needCreate: Boolean = false): PendingDatabase {
        if (pendingDatabase == null) {
            val db = getMixinDatabase()
            pendingDatabase = PendingDatabaseImp.getDatabase(context, db.floodMessageDao(), db.jobDao())
        }
        return pendingDatabase ?: throw IllegalStateException("PendingDatabase is not initialized")
    }

    @Synchronized
    fun initAllDatabases() {
        val identityNumber = requireNotNull(Session.getAccount()?.identityNumber)
        if (identityNumber == this.identityNumber && isInit()) return

        this.identityNumber = identityNumber

        mixinDatabase?.close()
        val db = MixinDatabase.getDatabase(context)
        mixinDatabase = db

        ftsDatabase?.close()
        ftsDatabase = FtsDatabase.getDatabase(context)

        pendingDatabase?.close()
        pendingDatabase = PendingDatabaseImp.getDatabase(context, db.floodMessageDao(), db.jobDao())
    }

    @Synchronized
    fun closeAllDatabases() {
        mixinDatabase?.close()
        mixinDatabase = null

        ftsDatabase?.close()
        ftsDatabase = null

        pendingDatabase?.close()
        pendingDatabase = null

        identityNumber = null
    }

    @Synchronized
    fun isInit() = mixinDatabase != null && ftsDatabase != null && pendingDatabase != null
}
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

    @Synchronized
    fun getMixinDatabase(): MixinDatabase {
        if (mixinDatabase == null || mixinDatabase?.isOpen != true){
            initAllDatabases()
        }
        return mixinDatabase ?: throw IllegalStateException("MixinDatabase is not initialized")
    }

    @Synchronized
    fun getFtsDatabase(): FtsDatabase {
        if (ftsDatabase == null || ftsDatabase?.isOpen != true) {
            initAllDatabases()
        }
        return ftsDatabase ?: throw IllegalStateException("FtsDatabase is not initialized")
    }

    @Synchronized
    fun getPendingDatabase(): PendingDatabase {
        if (pendingDatabase == null || pendingDatabase?.isOpen != true) {
            initAllDatabases()
        }
        return pendingDatabase ?: throw IllegalStateException("PendingDatabase is not initialized")
    }

    private var identityNumber: String? = null

    @Synchronized
    private fun initAllDatabases() {
        val identityNumber = requireNotNull(Session.getAccount()?.identityNumber)
        if (identityNumber == this.identityNumber) return

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
    }
}
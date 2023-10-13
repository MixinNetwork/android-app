package one.mixin.android.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import timber.log.Timber

interface MixinCorruptionCallback {
    fun onCorruption(database: SupportSQLiteDatabase)
}

class MixinOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val corruptions: List<MixinCorruptionCallback>,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val decoratedConfiguration =
            SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name)
                .callback(MixinOpenHelperCallback(configuration.callback, corruptions))
                .build()
        return delegate.create(decoratedConfiguration)
    }
}

class MixinOpenHelperCallback(
    private val delegate: SupportSQLiteOpenHelper.Callback,
    private val corruptions: List<MixinCorruptionCallback>,
) : SupportSQLiteOpenHelper.Callback(delegate.version) {

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        delegate.onCreate(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        delegate.onOpen(db)
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        delegate.onConfigure(db)
    }

    override fun onCorruption(db: SupportSQLiteDatabase) {
        try {
            corruptions.forEach { it.onCorruption(db) }
        } catch (e: Exception) {
            Timber.w(e)
        }
        delegate.onCorruption(db)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onUpgrade(db, oldVersion, newVersion)
    }
}

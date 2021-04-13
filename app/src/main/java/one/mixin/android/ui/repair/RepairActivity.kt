package one.mixin.android.ui.repair

import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import one.mixin.android.Constants
import one.mixin.android.databinding.ActivityRepairBinding
import one.mixin.android.extension.toast
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import kotlin.random.Random

class RepairActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRepairBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepairBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.dump.setOnClickListener { dump() }
        binding.corrupt.setOnClickListener { corrupt() }
        binding.repair.setOnClickListener { repair() }
    }

    private fun corrupt() = lifecycleScope.launch(Dispatchers.IO) {
        val dbFile = getDatabasePath(Constants.DataBase.DB_NAME)
        var db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)?.close()
        db.closeQuietly()
        RandomAccessFile(dbFile, "rw").use { raf ->
            val buffer = ByteArray(1024)
            Random.nextBytes(buffer)
            raf.seek(0)
            raf.write(buffer)
        }

        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val sb = StringBuilder()
            DatabaseUtils.dumpCursor(db.rawQuery("PRAGMA integrity_check", null), sb)
            Timber.d("@@@ integrity $sb")
        } catch (e: SQLiteDatabaseCorruptException) {
            withContext(Dispatchers.Main) {
                toast("Database is now CORRUPTED!")
            }
        }
    }

    private fun dump() = lifecycleScope.launch(Dispatchers.IO) {
        val dbFile = getDatabasePath(Constants.DataBase.DB_NAME)
        val dbDir = dbFile.parentFile
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val dumpFile = File(dbDir, "mixin.dump")
        if (dumpFile.exists()) {
            dumpFile.delete()
        }

        val start = System.currentTimeMillis()
        dumpSqliteMaster(db, dumpFile)
        db.close()
        val dumpCost = System.currentTimeMillis()
        Timber.d("@@@ dump cost: ${dumpCost - start}")
    }

    private fun repair() = lifecycleScope.launch(Dispatchers.IO) {
        val dbFile = getDatabasePath(Constants.DataBase.DB_NAME)
        val dbDir = dbFile.parentFile
        val dumpFile = File(dbDir, "mixin.dump")
        val newDBFile = File(dbDir, "mixin.new")
        if (newDBFile.exists()) {
            newDBFile.delete()
        }
        val newDB = SQLiteDatabase.openOrCreateDatabase(newDBFile, null)
        val start = System.currentTimeMillis()
        recoverSqliteMaster(newDB, dumpFile)
        newDB.close()
        val importCost = System.currentTimeMillis()
        Timber.d("@@@ import cost: ${importCost - start}")

        dbFile.delete()
        newDBFile.renameTo(dbFile)
        File("${dbFile}-wal").delete()
        File("${dbFile}-shm").delete()

        // TODO read data from corrupted db file
    }
}
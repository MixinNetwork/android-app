package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class BackupMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "backup_migration"
    }

    override fun onRun() {
    }
}

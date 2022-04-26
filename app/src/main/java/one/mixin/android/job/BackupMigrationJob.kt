package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class BackupMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "backup_migration"
        private const val serialVersionUID = -446433387151L
    }

    override fun onRun() {
    }
}

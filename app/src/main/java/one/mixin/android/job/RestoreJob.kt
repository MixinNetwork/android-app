package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.drive.Drive
import one.mixin.android.MixinApplication
import one.mixin.android.util.Session
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.FileBackupManager
import one.mixin.android.util.backup.Result

class RestoreJob : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RestoreJob"
    }

    override fun onRun() {
        val context = MixinApplication.appContext
        val account = GoogleSignIn.getAccountForScopes(context, Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
        if (!account.isExpired) {
            val driveResourceClient = Drive.getDriveResourceClient(context, account)
            BackupNotification.show(false)
            FileBackupManager.getManager(driveResourceClient, Session.getAccount()!!.identity_number).restore { result ->
                if (result == Result.SUCCESS) {
                } else {
                }
                BackupNotification.cancel()
            }
        }
    }
}
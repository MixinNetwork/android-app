package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AlertDialog
import com.drive.demo.backup.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveResourceClient
import kotlinx.android.synthetic.main.activity_restore.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.Session
import one.mixin.android.util.backup.DataBaseBackupManager
import java.util.Date

class RestoreActivity : BaseActivity() {

    private lateinit var driveResourceClient: DriveResourceClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val account = GoogleSignIn.getAccountForScopes(this, Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
        if (account.displayName == null) {
            AlertDialog.Builder(this, R.style.MixinAlertDialogTheme)
                .setMessage(R.string.restore_message)
                .setNegativeButton(R.string.restore_skip) { dialog, _ ->
                    InitializeActivity.showLoading(this)
                    dialog.dismiss()
                    finish()
                }
                .setPositiveButton(R.string.restore_authorization) { dialog, _ ->
                    startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
                    dialog.dismiss()
                }
                .show()
        } else {
            initialize(account)
        }
    }

    private fun initialize(account: GoogleSignInAccount) {
        driveResourceClient = Drive.getDriveResourceClient(this, account)
        val manager = DataBaseBackupManager.getManager(driveResourceClient, "mixin.db",
            Session.getAccount()!!.identity_number, { this.getDatabasePath("mixin.db") }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)

        manager.findBackup { result, metadata ->
            when (result) {
                Result.SUCCESS -> {
                    metadata?.let { data ->
                        setContentView(R.layout.activity_restore)
                        restore_time.text = data.createdDate.run {
                            val now = Date().time
                            val createTime = data.createdDate.time
                            DateUtils.getRelativeTimeSpanString(createTime, now, when {
                                ((now - createTime) < 60000L) -> DateUtils.SECOND_IN_MILLIS
                                ((now - createTime) < 3600000L) -> DateUtils.MINUTE_IN_MILLIS
                                ((now - createTime) < 86400000L) -> DateUtils.HOUR_IN_MILLIS
                                else -> DateUtils.DAY_IN_MILLIS
                            })
                        }
                        restore_restore.setOnClickListener {
                            manager.restoreDatabase {result
                                if (result == Result.SUCCESS) {
                                    InitializeActivity.showLoading(this)
                                    finish()
                                    toast("success")
                                } else {
                                    toast("failure")
                                }
                            }
                        }
                        restore_size.text = getString(R.string.restore_size, data.fileSize.fileSize())
                        restore_name.text = getString(R.string.restore_account, account.email)
                        restore_alert.text = getString(R.string.restore_alert, data.fileSize.fileSize())
                    }
                    restore_skip.setOnClickListener {
                        // InitializeActivity.showLoading(this)
                        MainActivity.show(this)
                        finish()
                    }
                }
                Result.FAILURE -> {
                }
                Result.NOT_FOUND -> {
                }
                Result.NOT_SUPPORT -> {
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                if (account == null) {
                    InitializeActivity.showLoading(this)
                    finish()
                } else {
                    initialize(account)
                }
            } catch (e: ApiException) {
                InitializeActivity.showLoading(this)
                finish()
            }
        }
    }

    // Todo test close
    // override fun onBackPressed() {
    // }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RestoreActivity::class.java))
        }

        private const val RC_SIGN_IN = 9001
    }
}
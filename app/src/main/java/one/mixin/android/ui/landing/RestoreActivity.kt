package one.mixin.android.ui.landing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveResourceClient
import com.google.android.gms.drive.Metadata
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_restore.*
import one.mixin.android.Constants
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RestoreJob
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.Session
import one.mixin.android.util.backup.DataBaseBackupManager
import one.mixin.android.util.backup.FileBackupManager
import one.mixin.android.util.backup.Result
import java.util.Date
import javax.inject.Inject

class RestoreActivity : BaseActivity() {

    private lateinit var driveResourceClient: DriveResourceClient

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, true)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val account = GoogleSignIn.getAccountForScopes(this, Drive.SCOPE_FILE, Drive.SCOPE_APPFOLDER)
        if (account.isExpired) {
            AlertDialog.Builder(this, R.style.MixinAlertDialogTheme)
                .setMessage(R.string.restore_message)
                .setNegativeButton(R.string.restore_skip) { dialog, _ ->
                    defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                    InitializeActivity.showLoading(this)
                    dialog.dismiss()
                    finish()
                }
                .setPositiveButton(R.string.restore_authorization) { dialog, _ ->
                    startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
                    dialog.dismiss()
                }.create().run {
                    this.setCanceledOnTouchOutside(false)
                    this.show()
                }
        } else {
            initialize(account)
        }
    }

    private fun initialize(account: GoogleSignInAccount) {
        driveResourceClient = Drive.getDriveResourceClient(this, account)
        val manager = DataBaseBackupManager.getManager(driveResourceClient, DB_NAME,
            Session.getAccount()!!.identity_number, { this.getDatabasePath(DB_NAME) }, Constants.DataBase.MINI_VERSION, Constants.DataBase.CURRENT_VERSION)
        findBackup(manager, account)
    }

    private fun findBackup(manager: DataBaseBackupManager, account: GoogleSignInAccount) {
        manager.findBackup { result, metadata ->
            when (result) {
                Result.SUCCESS -> {
                    metadata?.let { data ->
                        FileBackupManager.getManager(driveResourceClient!!, Session.getAccount()!!.identity_number).findBackup { result, metadata ->
                            if (result == Result.SUCCESS) {

                            }
                        }
                        initUI(manager, account, data)
                    }
                }
                else -> showErrorAlert(manager, account, result)
            }
        }
    }

    private fun showErrorAlert(manager: DataBaseBackupManager, account: GoogleSignInAccount, error: Result) {
        AlertDialog.Builder(this, R.style.MixinAlertDialogTheme)
            .setMessage(when (error) {
                Result.FAILURE -> {
                    R.string.restore_failure
                }
                Result.NOT_FOUND -> {
                    R.string.restore_not_found
                }
                else -> {
                    R.string.restore_not_support
                }
            })
            .setNegativeButton(R.string.restore_retry) { dialog, _ ->
                findBackup(manager, account)
                dialog.dismiss()
            }
            .setPositiveButton(R.string.restore_skip) { dialog, _ ->
                dialog.dismiss()
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                InitializeActivity.showLoading(this)
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    private fun initUI(manager: DataBaseBackupManager, account: GoogleSignInAccount, data: Metadata) {
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
            RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (!granted) {
                        openPermissionSetting()
                    } else {
                        showProgress()
                        manager.restoreDatabase { result ->
                            if (result == Result.SUCCESS) {
                                jobManager.addJobInBackground(RestoreJob())
                                InitializeActivity.showLoading(this)
                                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                                finish()
                            } else {
                                hideProgress()
                            }
                        }
                    }
                }, {

                })

        }
        restore_size.text = getString(R.string.restore_size, data.fileSize.fileSize())
        restore_name.text = getString(R.string.restore_account, account.email)
        restore_alert.text = getString(R.string.restore_alert, data.fileSize.fileSize())
        restore_skip.setOnClickListener {
            InitializeActivity.showLoading(this)
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
            finish()
        }
    }

    private fun showProgress() {
        restore_progress.visibility = View.VISIBLE
        restore_restore.visibility = View.GONE
        restore_skip.visibility = View.GONE
    }

    private fun hideProgress() {
        restore_progress.visibility = View.GONE
        restore_restore.visibility = View.VISIBLE
        restore_skip.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                if (account == null) {
                    defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                    InitializeActivity.showLoading(this)
                    finish()
                } else {
                    initialize(account)
                }
            } catch (e: ApiException) {
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                InitializeActivity.showLoading(this)
                finish()
            }
        }
    }

    override fun onBackPressed() {
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RestoreActivity::class.java))
        }

        private const val RC_SIGN_IN = 9001
    }
}
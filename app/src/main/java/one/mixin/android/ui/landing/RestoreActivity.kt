package one.mixin.android.ui.landing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.io.File
import java.util.Date
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_restore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.Result
import one.mixin.android.util.backup.restore

class RestoreActivity : BaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, true)
        alertDialogBuilder()
            .setMessage(R.string.restore_message)
            .setNegativeButton(R.string.restore_skip) { dialog, _ ->
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                InitializeActivity.showLoading(this)
                dialog.dismiss()
                finish()
            }
            .setPositiveButton(R.string.restore_authorization) { dialog, _ ->
                RxPermissions(this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe({ granted ->
                        if (!granted) {
                            openPermissionSetting()
                        } else {
                            findBackup()
                        }
                    }, {
                        InitializeActivity.showLoading(this)
                        finish()
                    })
                dialog.dismiss()
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun findBackup() = lifecycleScope.launch(Dispatchers.IO) {
        val file = one.mixin.android.util.backup.findBackup(this@RestoreActivity, coroutineContext)
        withContext(Dispatchers.Main) {
            if (file == null) {
                showErrorAlert(Result.NOT_FOUND)
            } else {
                initUI(file)
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun showErrorAlert(error: Result) {
        alertDialogBuilder()
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
                findBackup()
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

    @SuppressLint("MissingPermission")
    private fun initUI(data: File) {
        setContentView(R.layout.activity_restore)
        restore_time.text = data.lastModified().run {
            val now = Date().time
            val createTime = data.lastModified()
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
                .autoDispose(stopScope)
                .subscribe({ granted ->
                    if (!granted) {
                        openPermissionSetting()
                    } else {
                        showProgress()
                        BackupNotification.show(false)
                        restore()
                    }
                }, {
                    BackupNotification.cancel()
                    hideProgress()
                })
        }
        restore_size.text = getString(R.string.restore_size, data.length().fileSize())
        restore_skip.setOnClickListener {
            InitializeActivity.showLoading(this)
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
            finish()
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun restore() = lifecycleScope.launch {
        restore(this@RestoreActivity) { result ->
            BackupNotification.cancel()
            if (result == Result.SUCCESS) {
                InitializeActivity.showLoading(this@RestoreActivity)
                defaultSharedPreferences.putBoolean(
                    Constants.Account.PREF_RESTORE,
                    false
                )
                finish()
            } else {
                hideProgress()
            }
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

    override fun onBackPressed() {
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RestoreActivity::class.java))
        }
    }
}

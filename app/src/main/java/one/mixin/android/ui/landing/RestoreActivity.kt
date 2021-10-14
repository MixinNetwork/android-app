package one.mixin.android.ui.landing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ActivityRestoreBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.getLegacyBackupPath
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.backup.BackupInfo
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.Result
import one.mixin.android.util.backup.findBackup
import one.mixin.android.util.backup.findBackupApi29
import one.mixin.android.util.backup.restoreApi29
import javax.inject.Inject

@AndroidEntryPoint
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
                    .subscribe(
                        { granted ->
                            if (!granted) {
                                openPermissionSetting()
                            } else {
                                findBackupInfo()
                            }
                        },
                        {
                            InitializeActivity.showLoading(this)
                            finish()
                        }
                    )
                dialog.dismiss()
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun findBackupInfo() = lifecycleScope.launch(Dispatchers.IO) {
        val backupInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findBackupApi29(this@RestoreActivity, coroutineContext)
        } else {
            findBackup(this@RestoreActivity, coroutineContext)
        }
        withContext(Dispatchers.Main) {
            if (backupInfo == null) {
                showErrorAlert(Result.NOT_FOUND)
            } else {
                initUI(backupInfo)
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun showErrorAlert(error: Result) {
        alertDialogBuilder()
            .apply {
                when (error) {
                    Result.FAILURE -> {
                        setMessage(
                            R.string.restore_failure
                        )
                    }
                    Result.NOT_FOUND -> {
                        setMessage(context.getString(R.string.restore_not_found, context.getLegacyBackupPath()?.parentFile?.absoluteFile.toString()))
                    }
                    else -> {
                        setMessage(
                            R.string.restore_not_support
                        )
                    }
                }
            }
            .setNegativeButton(R.string.restore_retry) { dialog, _ ->
                findBackupInfo()
                dialog.dismiss()
            }
            .setPositiveButton(R.string.restore_skip) { dialog, _ ->
                dialog.dismiss()
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                InitializeActivity.showLoading(this)
                finish()
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    private lateinit var binding: ActivityRestoreBinding
    @SuppressLint("MissingPermission")
    private fun initUI(backupInfo: BackupInfo) {
        binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.restoreTime.text = backupInfo.lastModified.run {
            this.getRelativeTimeSpan()
        }
        binding.restoreRestore.setOnClickListener {
            RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (!granted) {
                            openPermissionSetting()
                        } else {
                            showProgress()
                            BackupNotification.show(false)
                            restore()
                        }
                    },
                    {
                        BackupNotification.cancel()
                        hideProgress()
                    }
                )
        }
        binding.restoreSize.text = getString(R.string.restore_size, backupInfo.length.fileSize())
        binding.restoreSkip.setOnClickListener {
            InitializeActivity.showLoading(this)
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
            finish()
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun restore() = lifecycleScope.launch {
        restoreApi29(this@RestoreActivity) { result ->
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
        binding.restoreProgress.visibility = View.VISIBLE
        binding.restoreRestore.visibility = View.GONE
        binding.restoreSkip.visibility = View.GONE
    }

    private fun hideProgress() {
        binding.restoreProgress.visibility = View.GONE
        binding.restoreRestore.visibility = View.VISIBLE
        binding.restoreSkip.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RestoreActivity::class.java))
        }
    }
}

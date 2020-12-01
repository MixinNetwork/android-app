package one.mixin.android.ui.landing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.Result
import one.mixin.android.util.backup.restore
import java.io.File
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
                                findBackup()
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
            .setMessage(
                when (error) {
                    Result.FAILURE -> {
                        R.string.restore_failure
                    }
                    Result.NOT_FOUND -> {
                        R.string.restore_not_found
                    }
                    else -> {
                        R.string.restore_not_support
                    }
                }
            )
            .setNegativeButton(R.string.restore_retry) { dialog, _ ->
                findBackup()
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
    private fun initUI(data: File) {
        binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.restoreTime.text = data.lastModified().run {
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
        binding.restoreSize.text = getString(R.string.restore_size, data.length().fileSize())
        binding.restoreSkip.setOnClickListener {
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

package one.mixin.android.ui.landing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLocalRestoreBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getDisplayPath
import one.mixin.android.extension.getLegacyBackupPath
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putString
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.ChooseFolderContract
import one.mixin.android.util.backup.BackupInfo
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.Result
import one.mixin.android.util.backup.canUserAccessBackupDirectory
import one.mixin.android.util.backup.findBackup
import one.mixin.android.util.backup.findBackupApi29
import one.mixin.android.util.backup.restore
import one.mixin.android.util.backup.restoreApi29
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocalRestoreFragment : BaseFragment(R.layout.fragment_local_restore) {
    private val binding by viewBinding(FragmentLocalRestoreBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private lateinit var chooseFolderResult: ActivityResultLauncher<String?>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        chooseFolderResult =
            registerForActivityResult(
                ChooseFolderContract(),
                requireActivity().activityResultRegistry,
                ::callbackChooseFolder,
            )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        alertDialogBuilder()
            .setMessage(R.string.restore_message)
            .setNegativeButton(R.string.Skip) { dialog, _ ->
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                InitializeActivity.showLoading(requireContext())
                dialog.dismiss()
                requireActivity().finish()
            }
            .setPositiveButton(R.string.authorization) { dialog, _ ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    RxPermissions(this)
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .autoDispose(stopScope)
                        .subscribe(
                            { granted ->
                                if (!granted) {
                                    requireActivity().openPermissionSetting()
                                } else {
                                    findBackupInfo()
                                }
                            },
                            {
                                InitializeActivity.showLoading(requireContext())
                                requireActivity().finish()
                            },
                        )
                } else {
                    findBackupInfo()
                }
                dialog.dismiss()
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    private fun findBackupInfo() =
        lifecycleScope.launch(Dispatchers.IO) {
            val backupInfo =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        findBackupApi29(requireContext(), coroutineContext)
                    }
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                        findBackup(requireContext(), coroutineContext)
                    }
                    else -> {
                        null
                    }
                }
            withContext(Dispatchers.Main) {
                if (backupInfo == null) {
                    showErrorAlert(Result.NOT_FOUND)
                } else {
                    initUI(backupInfo)
                }
            }
        }

    private fun showErrorAlert(
        @Suppress("SameParameterValue") error: Result,
    ) {
        val userBackup = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        alertDialogBuilder()
            .apply {
                when (error) {
                    Result.FAILURE -> {
                        setMessage(
                            R.string.Failure,
                        )
                    }
                    Result.NOT_FOUND -> {
                        if (userBackup) {
                            setMessage(context.getString(R.string.restore_choose_backup_folder))
                        } else {
                            setMessage(context.getString(R.string.restore_not_found, context.getLegacyBackupPath()?.parentFile?.absoluteFile.toString()))
                        }
                    }
                    else -> {
                        setMessage(
                            R.string.Not_support,
                        )
                    }
                }
            }
            .setNegativeButton(
                if (userBackup) {
                    R.string.Choose
                } else {
                    R.string.Retry
                },
            ) { dialog, _ ->
                if (userBackup) {
                    chooseFolderResult.launch(
                        defaultSharedPreferences.getString(
                            Constants.Account.PREF_BACKUP_DIRECTORY,
                            null,
                        ),
                    )
                } else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        RxPermissions(this)
                            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            .autoDispose(stopScope)
                            .subscribe(
                                { granted ->
                                    if (!granted) {
                                        requireActivity().openPermissionSetting()
                                    } else {
                                        findBackupInfo()
                                    }
                                },
                                {
                                },
                            )
                    } else {
                        findBackupInfo()
                    }
                }
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Skip) { dialog, _ ->
                dialog.dismiss()
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                InitializeActivity.showLoading(requireContext())
                requireActivity().finish()
            }.create().run {
                this.setCanceledOnTouchOutside(false)
                this.show()
            }
    }

    private fun callbackChooseFolder(uri: Uri?) {
        if (uri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Timber.d(requireActivity().getDisplayPath(uri))
            }
            defaultSharedPreferences.putString(Constants.Account.PREF_BACKUP_DIRECTORY, uri.toString())
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            requireActivity().contentResolver
                .takePersistableUriPermission(uri, takeFlags)
            Timber.d("${canUserAccessBackupDirectory(requireContext())}")
            findBackupInfo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initUI(backupInfo: BackupInfo) {
        binding.restoreTime.text = backupInfo.lastModified.getRelativeTimeSpan()
        binding.restoreRestore.setOnClickListener {
            fun afterGranted() {
                showProgress()
                BackupNotification.show(false)
                internalRestore()
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                RxPermissions(this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (!granted) {
                                requireActivity().openPermissionSetting()
                            } else {
                                afterGranted()
                            }
                        },
                        {
                            BackupNotification.cancel()
                            hideProgress()
                        },
                    )
            } else {
                afterGranted()
            }
        }
        binding.restoreSkip.setOnClickListener {
            InitializeActivity.showLoading(requireContext())
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
            requireActivity().finish()
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun internalRestore() =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                restoreApi29(requireContext(), restoreCallback)
            } else {
                restore(requireContext(), restoreCallback)
            }
        }

    private val restoreCallback: (result: Result) -> Unit = { result ->
        BackupNotification.cancel()
        if (result == Result.SUCCESS) {
            InitializeActivity.showLoading(requireContext())
            defaultSharedPreferences.putBoolean(
                Constants.Account.PREF_RESTORE,
                false,
            )
            requireActivity().finish()
        } else {
            hideProgress()
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

    override fun onBackPressed(): Boolean {
        return true
    }

    companion object {
        const val TAG = "LocalRestoreFragment"

        fun newInstance() = LocalRestoreFragment()
    }
}

package one.mixin.android.ui.setting

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_BACKUP_DIRECTORY
import one.mixin.android.Constants.BackUp.BACKUP_MEDIA
import one.mixin.android.Constants.BackUp.BACKUP_PERIOD
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBackupBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getDisplayPath
import one.mixin.android.extension.getRelativeTimeSpan
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.BackupJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.PropertyHelper
import one.mixin.android.util.backup.Result
import one.mixin.android.util.backup.canUserAccessBackupDirectory
import one.mixin.android.util.backup.delete
import one.mixin.android.util.backup.deleteApi29
import one.mixin.android.util.backup.findBackup
import one.mixin.android.util.backup.findBackupApi29
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BackUpFragment : BaseFragment(R.layout.fragment_backup) {
    companion object {
        const val TAG = "BackUpFragment"
        fun newInstance() = BackUpFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val binding by viewBinding(FragmentBackupBinding::bind)

    private lateinit var resultRegistry: ActivityResultRegistry
    private lateinit var chooseFolderResult: ActivityResultLauncher<String?>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry =
            requireActivity().activityResultRegistry
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            chooseFolderResult = registerForActivityResult(
                ChooseFolderContract(),
                resultRegistry,
                ::callbackChooseFolder
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            backupInfo.text = getString(R.string.backup_external_storage, getString(R.string.Never))
            backupChoose.setOnClickListener {
                chooseFolder()
            }
            backupAuto.isVisible = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            backupAuto.setOnClickListener {
                showBackupDialog()
            }
            lifecycleScope.launch {
                binding.backupAutoTv.text = options[loadBackupPeriod()]
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backupChoose.isVisible = true
                backupDes.isVisible = false
                (backupAuto.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToBottom = R.id.backup_choose
                }
                backupBn.isVisible = canUserAccessBackupDirectory(requireContext())
            } else {
                backupChoose.isVisible = false
                backupDes.isVisible = true
                (backupAuto.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToBottom = R.id.backup_des
                }
            }
            titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
            backupBn.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    showBackupMediaDialog()
                                } else {
                                    jobManager.addJobInBackground(BackupJob(true))
                                }
                            } else {
                                context?.openPermissionSetting()
                            }
                        },
                        {
                            context?.openPermissionSetting()
                        }
                    )
            }
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                findBackUp()
            }
            deleteBn.setOnClickListener {
                deleteBn.isVisible = false
                lifecycleScope.launch {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        deleteApi29(requireContext())
                    } else {
                            delete(requireContext())
                        }
                    ) {
                        findBackUp()
                    } else {
                        withContext(Dispatchers.Main) {
                            deleteBn.isVisible = true
                        }
                    }
                }
            }
        }
        BackupJob.backupLiveData.observe(
            viewLifecycleOwner
        ) {
            if (it) {
                binding.backupChoose.isVisible = false
                binding.backupBn.isInvisible = true
                binding.progressGroup.isVisible = true
                binding.deleteBn.isVisible = false
            } else {
                binding.backupChoose.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                binding.backupBn.isInvisible = false
                binding.progressGroup.isVisible = false
                binding.deleteBn.isVisible = true
                when (val result = BackupJob.backupLiveData.result) {
                    Result.SUCCESS -> findBackUp()
                    Result.NO_AVAILABLE_MEMORY ->
                        alertDialogBuilder()
                            .setMessage(R.string.backup_no_available_memory)
                            .setNegativeButton(R.string.OK) { dialog, _ -> dialog.dismiss() }
                            .show()
                    Result.FAILURE -> toast(R.string.backup_failure_tip)
                    else -> reportException(IllegalStateException("Backup result $result"))
                }
            }
        }
    }

    private val options by lazy {
        requireContext().resources.getStringArray(R.array.backup_dialog_list)
    }

    private suspend fun loadBackupPeriod(): Int =
        PropertyHelper.findValueByKey(BACKUP_PERIOD)?.toIntOrNull() ?: 0

    private fun showBackupDialog() = lifecycleScope.launch {
        val builder = alertDialogBuilder()
        builder.setTitle(R.string.backup_dialog_title)

        val checkedItem = loadBackupPeriod()
        builder.setSingleChoiceItems(options, checkedItem) { dialog, which ->
            binding.backupAutoTv.text = options[which]
            lifecycleScope.launch {
                PropertyHelper.updateKeyValue(BACKUP_PERIOD, which.toString())
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showBackupMediaDialog() = lifecycleScope.launch {
        val builder = alertDialogBuilder()
        builder.setTitle(R.string.backup_dialog_title)
        val animals = arrayOf(getString(R.string.backup_media))
        var checked = PropertyHelper.findValueByKey(BACKUP_MEDIA)?.toBooleanStrictOrNull() ?: true
        val checkedItems = booleanArrayOf(checked)
        builder.setMultiChoiceItems(
            animals, checkedItems
        ) { _, _, isChecked ->
            checked = isChecked
            lifecycleScope.launch {
                PropertyHelper.updateKeyValue(BACKUP_MEDIA, checked.toString())
            }
        }
        builder.setPositiveButton(R.string.Backup) { dialog, _ ->
            jobManager.addJobInBackground(BackupJob(force = true, backupMedia = checked))
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun chooseFolder() {
        val builder = alertDialogBuilder()
        builder.setMessage(R.string.backup_choose_a_folder)
        builder.setPositiveButton(R.string.backup_choose_folder) { _, _ ->
            chooseFolderResult.launch(
                defaultSharedPreferences.getString(
                    PREF_BACKUP_DIRECTORY,
                    null
                )
            )
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun callbackChooseFolder(uri: Uri?) {
        if (uri != null) {
            Timber.d(requireContext().getDisplayPath(uri))
            defaultSharedPreferences.putString(PREF_BACKUP_DIRECTORY, uri.toString())
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            requireContext().contentResolver
                .takePersistableUriPermission(uri, takeFlags)
            lifecycleScope.launch {
                findBackUp()
            }
            Timber.d("${canUserAccessBackupDirectory(requireContext())}")
        }
    }
    private fun findBackUp() = lifecycleScope.launch(Dispatchers.IO) {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            withContext(Dispatchers.Main) {
                if (viewDestroyed()) return@withContext
                binding.backupProgress.isVisible = true
                binding.backupInfo.isInvisible = true
                binding.backupBn.isVisible = canUserAccessBackupDirectory(requireContext())
            }
            findBackupApi29(requireContext(), coroutineContext)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@launch
            }
            findBackup(requireContext(), coroutineContext)
        }
        withContext(Dispatchers.Main) {
            if (viewDestroyed()) return@withContext
            binding.apply {
                binding.backupProgress.isVisible = false
                binding.backupInfo.isInvisible = false
                if (info == null) {
                    backupInfo.text = getString(R.string.backup_external_storage, getString(R.string.Never))
                    backupPath.isVisible = false
                } else {
                    val time = info.lastModified.run {
                        this.getRelativeTimeSpan()
                    }
                    backupPath.text = getString(R.string.restore_path, info.path)
                    backupInfo.text = getString(R.string.backup_external_storage, time)
                    backupPath.isVisible = true
                }
            }
        }
    }
}

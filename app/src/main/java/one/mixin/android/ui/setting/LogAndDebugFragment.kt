package one.mixin.android.ui.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLogDebugBinding
import one.mixin.android.db.DatabaseMonitor
import one.mixin.android.db.property.PropertyHelper.findValueByKey
import one.mixin.android.db.property.PropertyHelper.updateKeyValue
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.util.debug.FileLogTree
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class LogAndDebugFragment : BaseFragment(R.layout.fragment_log_debug) {
    companion object {
        const val TAG = "LogAndDebugFragment"

        fun newInstance() = LogAndDebugFragment()
    }

    private val binding by viewBinding(FragmentLogDebugBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.apply {
                titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                webDebugSc.isChecked =
                    defaultSharedPreferences.getBoolean(Constants.Debug.DB_DEBUG, false)
                webDebugSc.setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        if (isChecked) {
                            defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG, true)
                        } else {
                            defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG, false)
                            defaultSharedPreferences.putBoolean(
                                Constants.Debug.DB_DEBUG_WARNING,
                                true,
                            )
                        }
                    }
                }

                diagnosis.setOnClickListener {
                    navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
                }

                logs.setOnClickListener {
                    shareLogsFile()
                }
                database.setOnClickListener {
                    navTo(
                        DatabaseDebugFragment.newInstance(),
                        DatabaseDebugFragment.TAG,
                    )
                }
                databaseDebugLogsSc.isChecked = findValueByKey(Constants.Debug.DB_DEBUG, false)
                databaseDebugLogsSc.setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        updateKeyValue(Constants.Debug.DB_DEBUG_LOGS, isChecked)
                        DatabaseMonitor.reset()
                    }
                }
                walletConnectSc.isChecked = defaultSharedPreferences.getBoolean(Constants.Debug.WALLET_CONNECT_DEBUG, false)
                walletConnectSc.setOnCheckedChangeListener { _, isChecked ->
                    defaultSharedPreferences.putBoolean(Constants.Debug.WALLET_CONNECT_DEBUG, isChecked)
                }
                safe.setOnClickListener {
                    navTo(
                        SafeDebugFragment.newInstance(),
                        SafeDebugFragment.TAG,
                    )
                }
            }
        }
    }

    private fun shareLogsFile() {
        val dialog =
            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                setCancelable(false)
            }
        dialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val logFile = FileLogTree.getLogFile()
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                if (logFile.length() <= 0) {
                    toast(R.string.File_does_not_exist)
                }

                Intent().apply {
                    val uri = logFile.absolutePath.toUri()
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val extraMimeTypes = arrayOf("text/plain", "audio/*", "image/*", "video/*")
                    putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
                    type = "application/*"

                    val resInfoList =
                        requireContext().packageManager.queryIntentActivities(
                            this,
                            PackageManager.MATCH_DEFAULT_ONLY,
                        )
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        requireContext().grantUriPermission(
                            packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    try {
                        startActivity(Intent.createChooser(this, logFile.name))
                    } catch (ignored: ActivityNotFoundException) {
                    }
                }
            }
        }
    }
}

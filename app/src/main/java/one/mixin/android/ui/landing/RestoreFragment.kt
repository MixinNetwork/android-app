package one.mixin.android.ui.landing

import android.Manifest
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentRestoreBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.showConfirmDialog
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.transfer.TransferActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class RestoreFragment : BaseFragment(R.layout.fragment_restore) {
    private val binding by viewBinding(FragmentRestoreBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            fromAnotherCl.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(
                        *mutableListOf(Manifest.permission.CAMERA).apply {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }.toTypedArray(),
                    )
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            AnalyticsTracker.trackLoginRestore("another_phone")
                            TransferActivity.showRestoreFromPhone(requireContext())
                        } else {
                            requireActivity().openPermissionSetting()
                        }
                    }
            }
            fromLocalCl.setOnClickListener {
                lifecycleScope.launch {
                    val localData = getLocalDataInfo()
                    val count = localData?.first
                    val lastCreatedAt = localData?.second
                    if (count != null && lastCreatedAt != null) {
                        AnalyticsTracker.trackLoginRestore("local")
                        requireContext().showConfirmDialog(
                            getString(R.string.restore_local_exists, "$count".numberFormat(), lastCreatedAt),
                            cancelable = false,
                        ) {
                            fromLocal()
                        }
                    } else {
                        fromLocal()
                    }
                }
            }
            skipTv.setOnClickListener {
                AnalyticsTracker.trackLoginRestore("skip")
                InitializeActivity.showLoading(requireContext())
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                requireActivity().finish()
            }
        }
    }

    private fun fromLocal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        navTo(LocalRestoreFragment.newInstance(), LocalRestoreFragment.TAG)
                    } else {
                        requireActivity().openPermissionSetting()
                    }
                }
        } else {
            navTo(LocalRestoreFragment.newInstance(), LocalRestoreFragment.TAG)
        }
    }

    private suspend fun getLocalDataInfo(): Pair<Int?, String?>? =
        withContext(Dispatchers.IO) {
            val dbFile = requireContext().getDatabasePath(Constants.DataBase.DB_NAME)
            if (!dbFile.exists()) {
                return@withContext null
            }
            var c: Cursor? = null
            var db: SQLiteDatabase? = null

            try {
                db =
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                    )
                c = db.rawQuery("SELECT count(1) FROM messages", null)
                var count: Int? = null
                if (c.moveToFirst()) {
                    count = c.getIntOrNull(0) ?: 0
                }
                c?.close()

                c = db.rawQuery("SELECT created_at FROM messages ORDER BY created_at DESC LIMIT 1", null)
                var lastCreatedAt: String? = null
                if (c.moveToFirst()) {
                    lastCreatedAt = c.getStringOrNull(0)?.fullDate()
                }

                return@withContext Pair(count, lastCreatedAt)
            } catch (e: Exception) {
                return@withContext null
            } finally {
                c?.close()
                db?.close()
            }
        }

    override fun onBackPressed(): Boolean {
        return true
    }

    companion object {
        const val TAG = "RestoreFragment"

        fun newInstance() = RestoreFragment()
    }
}

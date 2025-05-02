package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDatabaseDebugBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.WarningBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import timber.log.Timber

@AndroidEntryPoint
class DatabaseDebugFragment : BaseFragment(R.layout.fragment_database_debug) {
    companion object {
        const val TAG = "DatabaseDebugFragment"
        fun newInstance() = DatabaseDebugFragment()
        
        private val DATABASE_OPTIONS = arrayOf(
            "Mixin Database",
            "Pending Database",
            "Wallet Database"
        )
    }

    private val binding by viewBinding(FragmentDatabaseDebugBinding::bind)
    private lateinit var walletDb: WalletDatabase

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener { }
        
        setupDatabaseSpinner()
        setupQueryButton()
        setupLogsView()
        setupTitleView()
        
        walletDb = WalletDatabase.getDatabase(requireContext())
        showWarning()
    }

    private fun setupDatabaseSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, DATABASE_OPTIONS)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.databaseSpinner.adapter = adapter
        binding.databaseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // do nothing
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }
    }

    private fun setupQueryButton() {
        binding.runBn.setOnClickListener {
            lifecycleScope.launch {
                val sql = binding.sql.text.toString()
                val result = when(binding.databaseSpinner.selectedItemPosition) {
                    0 -> MixinDatabase.query(sql)
                    1 -> PendingDatabaseImp.query(sql)
                    2 -> queryWalletDatabase(sql)
                    else -> requireActivity().getString(R.string.Unknown)
                }
                binding.logs.text = "${binding.logs.text}\n$result"
            }
        }
    }

    private fun queryWalletDatabase(sql: String): String {
        return try {
            val cursor = walletDb.openHelper.readableDatabase.query(sql)
            val result = StringBuilder()
            
            val columnNames = cursor.columnNames
            result.append(columnNames.joinToString(" | ")).append("\n")
            result.append("-".repeat(columnNames.size * 20)).append("\n")
            
            while (cursor.moveToNext()) {
                val row = columnNames.map { column ->
                    val index = cursor.getColumnIndex(column)
                    when {
                        cursor.isNull(index) -> "null"
                        else -> cursor.getString(index)
                    }
                }
                result.append(row.joinToString(" | ")).append("\n")
            }
            
            cursor.close()
            result.toString()
        } catch (e: Exception) {
            Timber.e(e)
            "Error: ${e.message}"
        }
    }

    private fun setupLogsView() {
        binding.logs.setOnLongClickListener {
            requireContext().getClipboardManager()
                .setPrimaryClip(ClipData.newPlainText(null, binding.logs.text))
            toast(R.string.copied_to_clipboard)
            true
        }
    }

    private fun setupTitleView() {
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        view?.removeCallbacks(showWarningRunnable)
    }

    private fun showWarning() {
        val showWarning = defaultSharedPreferences.getBoolean(Constants.Debug.DB_DEBUG_WARNING, true)
        if (showWarning) {
            view?.post(showWarningRunnable)
        }
    }

    private val showWarningRunnable =
        Runnable {
            val bottom = WarningBottomSheetDialogFragment.newInstance(getString(R.string.db_debug_warning), 5)
            bottom.callback =
                object : WarningBottomSheetDialogFragment.Callback {
                    override fun onContinue() {
                        defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG_WARNING, false)
                    }
                }
            bottom.showNow(parentFragmentManager, WarningBottomSheetDialogFragment.TAG)
        }
}

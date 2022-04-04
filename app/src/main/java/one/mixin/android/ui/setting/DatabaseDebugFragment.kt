package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDatabaseDebugBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.WarningBottomSheetDialogFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class DatabaseDebugFragment : BaseFragment(R.layout.fragment_database_debug) {
    companion object {
        const val TAG = "DatabaseDebugFragment"

        fun newInstance() = DatabaseDebugFragment()
    }

    private val binding by viewBinding(FragmentDatabaseDebugBinding::bind)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener { }
        binding.runBn.setOnClickListener {
            lifecycleScope.launch {
                binding.logs.text =
                    "${binding.logs.text}\n${MixinDatabase.query(binding.sql.text.toString())}"
            }
        }
        binding.logs.setOnLongClickListener {
            requireContext().getClipboardManager()
                .setPrimaryClip(ClipData.newPlainText(null, binding.logs.text))
            toast(R.string.copied_to_clipboard)
            true
        }
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressed()
        }

        showWarning()
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

    private val showWarningRunnable = Runnable {
        val bottom = WarningBottomSheetDialogFragment.newInstance(getString(R.string.db_debug_warning), 5)
        bottom.callback = object : WarningBottomSheetDialogFragment.Callback {
            override fun onContinue() {
                defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG_WARNING, false)
            }
        }
        bottom.showNow(parentFragmentManager, WarningBottomSheetDialogFragment.TAG)
    }
}

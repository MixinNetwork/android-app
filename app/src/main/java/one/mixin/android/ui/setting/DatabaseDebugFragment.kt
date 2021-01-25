package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDatabaseDebugBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
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
            requireContext().toast(R.string.copy_success)
            true
        }
    }
}

package one.mixin.android.ui.setting.diagnosis

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDiagnosisBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.net.diagnosis
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class DiagnosisFragment : BaseFragment() {
    companion object {
        const val TAG = "DiagnosisFragment"
        fun newInstance(): DiagnosisFragment {
            return DiagnosisFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        layoutInflater.inflate(R.layout.fragment_diagnosis, container, false)

    private val binding by viewBinding(FragmentDiagnosisBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.titleTv.setText(R.string.Net_Diagnosis)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
        binding.titleView.rightIb.setOnClickListener {
            context?.getClipboardManager()
                ?.setPrimaryClip(ClipData.newPlainText(null, binding.resultTv.text))
            toast(R.string.copied_to_clipboard)
        }
        binding.titleView.rightAnimator.displayedChild = 2

        lifecycleScope.launch(Dispatchers.IO) {
            diagnosis(requireContext()) {
                lifecycleScope.launch inner@{
                    if (viewDestroyed()) return@inner
                    binding.resultTv.append(it)
                    if (it == getString(R.string.Diagnosis_Complete)) {
                        binding.titleView.rightAnimator.displayedChild = 0
                    }
                }
            }
        }
    }
}

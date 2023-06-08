package one.mixin.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.transfer.compose.SelectDatePage

@AndroidEntryPoint
class SelectDateFragment : BaseFragment() {
    companion object {
        const val TAG = "SelectDateFragment"

        fun newInstance() = SelectDateFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                SelectDatePage {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }
}

package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.compose.theme.MixinAppTheme
@AndroidEntryPoint
class PerpetualGuideFragment : Fragment() {

    companion object {
        const val TAG = "PerpetualGuideFragment"

        fun newInstance() = PerpetualGuideFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MixinAppTheme {
                    PerpetualGuidePage(
                        pop = {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    )
                }
            }
        }
    }
}


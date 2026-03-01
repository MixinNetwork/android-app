package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.api.response.perps.PositionHistoryView
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class PositionDetailFragment : BaseFragment() {
    companion object {
        const val TAG = "PositionDetailFragment"
        private const val ARGS_POSITION = "args_position"

        fun newInstance(position: PositionHistoryView): PositionDetailFragment {
            return PositionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_POSITION, position)
                }
            }
        }
    }

    private val viewModel by viewModels<PerpetualViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val position = arguments?.getParcelableCompat(ARGS_POSITION, PositionHistoryView::class.java)
            ?: throw IllegalArgumentException("Position is required")

        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    PositionDetailPage(
                        position = position,
                        pop = {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    )
                }
            }
        }
    }
}

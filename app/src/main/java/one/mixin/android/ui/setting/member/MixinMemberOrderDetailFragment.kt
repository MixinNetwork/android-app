package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.ui.setting.ui.components.InvoiceDetailPage

class MixinMemberOrderDetailFragment : Fragment() {

    companion object {
        const val TAG = "MixinMemberOrderDetailFragment"
        private const val ARG_INVOICE = "arg_invoice"

        fun newInstance(order: MemberOrder): MixinMemberOrderDetailFragment {
            val fragment = MixinMemberOrderDetailFragment()
            val args = Bundle()
            args.putParcelable(ARG_INVOICE, order)
            fragment.arguments = args
            return fragment
        }
    }

    private val order: MemberOrder by lazy {
        requireArguments().getParcelable(ARG_INVOICE)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InvoiceDetailPage(
                    order = order,
                    onPop = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onCancel = { order ->
                        MixinMemberCancelBottomSheetDialogFragment.newInstance(order).showNow(parentFragmentManager,
                            MixinMemberCancelBottomSheetDialogFragment.TAG)
                    }
                )
            }
        }
    }
}

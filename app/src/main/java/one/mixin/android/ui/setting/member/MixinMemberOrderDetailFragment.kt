package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import one.mixin.android.ui.setting.ui.components.InvoiceDetailPage

class MixinMemberOrderDetailFragment : Fragment() {

    companion object {
        const val TAG = "MixinMemberOrderDetailFragment"
        private const val ARG_INVOICE = "arg_invoice"

        fun newInstance(invoice: MemberInvoice): MixinMemberOrderDetailFragment {
            val fragment = MixinMemberOrderDetailFragment()
            val args = Bundle()
            args.putParcelable(ARG_INVOICE, invoice)
            fragment.arguments = args
            return fragment
        }
    }

    private val invoice: MemberInvoice by lazy {
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
                    invoice = invoice,
                    onPop = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

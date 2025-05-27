package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import one.mixin.android.ui.setting.ui.page.AllInvoicesPage

class AllMixinMemberInvoicesFragment : Fragment() {

    companion object {
        const val TAG = "AllMixinMemberInvoicesFragment"

        fun newInstance(invoices: List<MemberInvoice>): AllMixinMemberInvoicesFragment {
            val fragment = AllMixinMemberInvoicesFragment()
            val args = Bundle()
            args.putParcelableArrayList("invoices", ArrayList(invoices))
            fragment.arguments = args
            return fragment
        }
    }

    private val invoices: List<MemberInvoice> by lazy {
        requireArguments().getParcelableArrayList<MemberInvoice>("invoices") ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AllInvoicesPage(invoices = invoices) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}

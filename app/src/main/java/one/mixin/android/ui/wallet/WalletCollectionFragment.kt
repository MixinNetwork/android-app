package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import one.mixin.android.R
import one.mixin.android.databinding.FragmentOldWalletBinding
import one.mixin.android.databinding.FragmentWalletCollectionBinding
import one.mixin.android.databinding.ViewWalletBottomBinding
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navigate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SegmentedView

class WalletCollectionFragment : BaseFragment(R.layout.fragment_wallet_collection) {

    private val binding by viewBinding(FragmentWalletCollectionBinding::bind)
    private var _bottomBinding: ViewWalletBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            rightIb.setOnClickListener { showBottom() }
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            searchIb.setOnClickListener { view.navigate(R.id.action_wallet_to_wallet_search) }
            segmented.setOnSelectListener(object : SegmentedView.OnSelectListener {
                override fun onClick(status: Int) {
                    // Todo
                    parentFragmentManager.inTransaction {
                        replace(
                            R.id.collection,
                            OldWalletFragment.newInstance(),
                            tag
                        )
                    }
                }
            })
        }
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        _bottomBinding = ViewWalletBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(
                    requireActivity(),
                    R.style.Custom
                ), R.layout.view_wallet_bottom, null
            )
        )
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        val rootView = this.view
        bottomBinding.hide.setOnClickListener {
            rootView?.navigate(R.id.action_wallet_fragment_to_hidden_assets_fragment)
            bottomSheet.dismiss()
        }
        bottomBinding.transactionsTv.setOnClickListener {
            rootView?.navigate(R.id.action_wallet_fragment_to_all_transactions_fragment)
            bottomSheet.dismiss()
        }
        bottomBinding.connectedTv.setOnClickListener {
            rootView?.navigate(R.id.action_wallet_to_wallet_connect)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bottomBinding = null
    }
}
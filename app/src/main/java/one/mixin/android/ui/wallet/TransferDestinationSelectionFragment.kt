package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentTransfeDestinationSelectionBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.putString
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.R
import one.mixin.android.extension.navigate

@AndroidEntryPoint
class  TransferDestinationSelectionFragment : BaseFragment() {

    companion object {
        const val TAG = "TransferDestinationSelectionFragment"
        const val ARGS_ASSET = "args_asset"
        const val ARGS_FROM_WALLET = "args_from_wallet"

        fun newInstance(asset: TokenItem, fromWallet: Boolean = true) = TransferDestinationSelectionFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_ASSET, asset)
                putBoolean(ARGS_FROM_WALLET, fromWallet)
            }
        }
    }

    private var _binding: FragmentTransfeDestinationSelectionBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var asset: TokenItem
    private var fromWallet: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        asset = requireArguments().getParcelableCompat(
            if (requireArguments().containsKey(ARGS_ASSET)) ARGS_ASSET else TransactionsFragment.ARGS_ASSET,
            TokenItem::class.java
        )!!
        fromWallet = requireArguments().getBoolean(ARGS_FROM_WALLET, false)
        _binding = FragmentTransfeDestinationSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            contact.setOnClickListener {
                defaultSharedPreferences.putString(TransferFragment.ASSET_PREFERENCE, asset.assetId)
                if (fromWallet) {
                    WalletActivity.showWithToken(
                        requireActivity(),
                        asset,
                        WalletActivity.Destination.Contact
                    )
                } else {
                    view.navigate(R.id.action_destination_to_single_friend_select, Bundle().apply {
                        putParcelable(ARGS_ASSET, asset)
                    })
                }
            }
            address.setOnClickListener {
                if (fromWallet) {
                    WalletActivity.showWithToken(
                        requireActivity(),
                        asset,
                        WalletActivity.Destination.Address
                    )
                } else {
                    view.navigate(R.id.action_destination_to_address_input, Bundle().apply {
                        putParcelable(ARGS_ASSET, asset)
                    })
                }
            }
            titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

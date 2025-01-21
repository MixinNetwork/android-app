package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentTransfeDestinationSelectionBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.putString
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class TransferDestinationSelectionFragment : BaseFragment() {

    companion object {
        const val TAG = "TransferDestinationSelectionFragment"
        const val ARGS_ASSET = "args_asset"

        fun newInstance(asset: TokenItem) = TransferDestinationSelectionFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_ASSET, asset)
            }
        }
    }

    private var _binding: FragmentTransfeDestinationSelectionBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var asset: TokenItem

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        asset = requireArguments().getParcelableCompat(TransactionsFragment.Companion.ARGS_ASSET, TokenItem::class.java)!!
        _binding = FragmentTransfeDestinationSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            contact.setOnClickListener {
                defaultSharedPreferences.putString(TransferFragment.ASSET_PREFERENCE, asset.assetId)
                if (navContactAction == -1) {
                    WalletActivity.showWithToken(requireActivity(), asset, WalletActivity.Destination.Contact)
                } else {
                    binding.root.navigate(
                        navContactAction,
                        Bundle().apply {
                            putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                        },
                    )
                }
            }
            parentFragmentManager.beginTransaction().remove(this@TransferDestinationSelectionFragment).commit()
            if (navAddressAction == -1) {
                WalletActivity.showWithToken(requireActivity(), asset, WalletActivity.Destination.Address)
            } else {
                binding.root.navigate(
                    navAddressAction,
                    Bundle().apply {
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                    },
                )
            }
            titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
    }

    var navAddressAction = -1
    var navContactAction = -1

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

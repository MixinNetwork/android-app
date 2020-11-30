package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetKeyBottomBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AssetKeyBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetKeyBottomSheetDialogFragment"

        fun newInstance(asset: AssetItem) = AssetKeyBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, asset)
        }
    }

    private var _binding: FragmentAssetKeyBottomBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val asset: AssetItem by lazy {
        requireArguments().getParcelable(ARGS_ASSET)!!
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = FragmentAssetKeyBottomBinding.bind(View.inflate(context, R.layout.fragment_asset_key_bottom, null))
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            titleView.apply {
                rightIv.setOnClickListener { dismiss() }
                titleTv.text = asset.name
            }
            titleView.showBadgeCircleView(asset)
            symbolAsTv.text = asset.symbol
            chainAsTv.text = asset.chainName
            assetKeyAsTv.text = asset.assetKey
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

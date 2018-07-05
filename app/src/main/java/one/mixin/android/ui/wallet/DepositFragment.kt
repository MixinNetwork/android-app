package one.mixin.android.ui.wallet

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.fragment_deposit.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.wallet.DepositQrBottomFragment.Companion.TYPE_MEMO
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

class DepositFragment : Fragment() {

    companion object {
        const val TAG = "DepositFragment"

        fun newInstance(asset: AssetItem) = DepositFragment().apply {
            arguments = bundleOf(ARGS_ASSET to asset)
        }
    }

    private val asset: AssetItem by lazy { arguments!!.getParcelable<AssetItem>(ARGS_ASSET) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_deposit, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        asset_symbol_tv.text = asset.symbol
        asset_name_tv.text = asset.name
        account_name_tv.text = asset.accountName
        account_memo_tv.text = asset.accountMemo
        account_name_iv.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
        account_memo_iv.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset, TYPE_MEMO).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
    }
}
package one.mixin.android.ui.wallet

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_deposit_account.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.DepositQrBottomFragment.Companion.TYPE_TAG
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

class DepositAccountFragment : Fragment() {

    companion object {
        const val TAG = "DepositAccountFragment"
    }

    private val asset: AssetItem by lazy { arguments!!.getParcelable<AssetItem>(ARGS_ASSET) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_deposit_account, container, false).apply { this.setOnClickListener { } }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title.setOnClickListener { activity?.onBackPressed() }
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        asset_symbol_tv.text = asset.symbol
        asset_name_tv.text = asset.name
        account_name_tv.text = asset.accountName
        account_memo_tv.text = asset.accountTag
        tip_tv.text = getString(R.string.deposit_tip, asset.symbol, asset.confirmations)
        account_name_iv.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
        account_memo_iv.setOnClickListener {
            DepositQrBottomFragment.newInstance(asset, TYPE_TAG).show(requireFragmentManager(), DepositQrBottomFragment.TAG)
        }
        account_name_tv.setOnClickListener {
            context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, asset.accountName)
            context?.toast(R.string.copy_success)
        }
        account_memo_tv.setOnClickListener {
            context?.getClipboardManager()?.primaryClip = ClipData.newPlainText(null, asset.accountTag)
            context?.toast(R.string.copy_success)
        }
    }
}
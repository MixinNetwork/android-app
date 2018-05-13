package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_wallet_password_change.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.ui.common.BaseFragment

class WalletChangePasswordFragment : BaseFragment() {
    companion object {
        val TAG = "WalletChangePasswordFragment"

        fun newInstance(): WalletChangePasswordFragment = WalletChangePasswordFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_wallet_password_change, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_animator.setOnClickListener { activity?.onBackPressed() }
        change_tv.setOnClickListener { activity?.addFragment(this@WalletChangePasswordFragment,
            OldPasswordFragment.newInstance(), OldPasswordFragment.TAG) }
    }
}

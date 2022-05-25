package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAccountBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.setting.delete.DeleteAccountFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AccountFragment : BaseFragment(R.layout.fragment_account) {
    companion object {
        const val TAG = "AccountFragment"

        fun newInstance() = AccountFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentAccountBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            privacyRl.setOnClickListener {
                navTo(PrivacyFragment.newInstance(), PrivacyFragment.TAG)
            }
            securityRl.setOnClickListener {
                navTo(SecurityFragment.newInstance(), SecurityFragment.TAG)
            }
            deleteRl.setOnClickListener {
                navTo(DeleteAccountFragment.newInstance(), DeleteAccountFragment.TAG)
            }
            changeRl.setOnClickListener {
                changeNumber()
            }
        }
    }

    private fun changeNumber() {
        alert(getString(R.string.profile_modify_number))
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.Change_Phone_Number) { dialog, _ ->
                dialog.dismiss()
                if (Session.getAccount()?.hasPin == true) {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom
                        )
                            .add(
                                R.id.container,
                                VerifyFragment.newInstance(VerifyFragment.FROM_PHONE)
                            )
                            .addToBackStack(null)
                    }
                } else {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom
                        )
                            .add(
                                R.id.container,
                                WalletPasswordFragment.newInstance(),
                                WalletPasswordFragment.TAG
                            )
                            .addToBackStack(null)
                    }
                }
            }.show()
    }
}

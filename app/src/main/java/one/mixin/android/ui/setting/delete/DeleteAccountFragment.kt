package one.mixin.android.ui.setting.delete

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDeleteAccountBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.landing.MobileFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class DeleteAccountFragment : BaseFragment(R.layout.fragment_delete_account) {
    companion object {
        const val TAG = "DeleteAccountFragment"

        fun newInstance() = DeleteAccountFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentDeleteAccountBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            deleteRl.setOnClickListener {
                verifyDeleteAccount()
            }
            changeRl.setOnClickListener {
                changeNumber()
            }
        }
    }

    private fun verifyDeleteAccount() {
        if (Session.getAccount()?.hasPin == true) {
            VerifyBottomSheetDialogFragment.newInstance().setContinueCallback {
                showTip()
            }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
        } else {
            if (Session.getAccount()?.hasPin == true) {
                WalletActivity.show(requireActivity())
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, WalletPasswordFragment.newInstance(false))
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }
    }

    private fun showTip() {
        lifecycleScope.launch {
            if (viewModel.findAllAssetIdSuspend().isEmpty()) {
                toMobile()
            } else {
                DeleteAccountTipBottomSheetDialogFragment.newInstance()
                    .setContinueCallback {
                        toMobile()
                    }
                    .showNow(
                        parentFragmentManager,
                        DeleteAccountTipBottomSheetDialogFragment.TAG
                    )
            }
        }
    }

    private fun toMobile() {
        parentFragmentManager.inTransaction {
            setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom,
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom
            )
                .add(
                    R.id.container,
                    MobileFragment.newInstance(from = VerifyFragment.FROM_DELETE_ACCOUNT)
                )
                .addToBackStack(null)
        }
    }

    private fun changeNumber() {
        alert(getString(R.string.profile_modify_number))
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.profile_phone) { dialog, _ ->
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

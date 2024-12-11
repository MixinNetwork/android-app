package one.mixin.android.ui.setting.delete

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.databinding.FragmentDeleteAccountBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alert
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_DELETE_ACCOUNT
import one.mixin.android.ui.landing.VerificationFragment
import one.mixin.android.ui.setting.LogoutPinBottomSheetDialogFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.wallet.BackupMnemonicPhraseWarningBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.CaptchaView

@AndroidEntryPoint
class DeleteAccountFragment : BaseFragment(R.layout.fragment_delete_account) {
    companion object {
        const val TAG = "DeleteAccountFragment"

        fun newInstance() = DeleteAccountFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentDeleteAccountBinding::bind)

    private var captchaView: CaptchaView? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(getString(R.string.emergency_url)) }
            deleteRl.setOnClickListener {
                verifyDeleteAccount()
            }
            changeRl.setOnClickListener {
                changeNumber()
            }
            logOutRl.setOnClickListener{
                if (!Session.hasPhone() && !Session.saltExported()) {
                    BackupMnemonicPhraseWarningBottomSheetDialogFragment.newInstance()
                        .show(parentFragmentManager, BackupMnemonicPhraseWarningBottomSheetDialogFragment.TAG)
                } else {
                    LogoutPinBottomSheetDialogFragment.newInstance()
                        .showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (captchaView?.isVisible() == true) {
            captchaView?.hide()
            return true
        }
        return false
    }

    private fun verifyDeleteAccount() {
        if (!Session.hasPhone()){
            deleteAnonymousUser()
        } else if (Session.getAccount()?.hasPin == true) {
            VerifyBottomSheetDialogFragment.newInstance().apply {
                if (Session.hasPhone()) {
                    setContinueCallback { dialog ->
                        showTip(dialog)
                    }
                }
            }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
        } else {
            if (Session.getAccount()?.hasPin == true) {
                MainActivity.showWallet(requireContext())
            } else {
                TipActivity.show(requireActivity(), TipType.Create, false)
            }
        }
    }

    private fun showTip(dialog: DialogFragment) {
        lifecycleScope.launch {
            val phone = Session.getAccount()?.phone ?: return@launch
            if (viewModel.findAllAssetIdSuspend().isEmpty()) {
                showDialog(phone) {
                    dialog.dismiss()
                }
            } else {
                dialog.dismiss()
                DeleteAccountTipBottomSheetDialogFragment.newInstance()
                    .setContinueCallback { dialog ->
                        showDialog(phone) {
                            dialog.dismiss()
                        }
                    }
                    .showNow(
                        parentFragmentManager,
                        DeleteAccountTipBottomSheetDialogFragment.TAG,
                    )
            }
        }
    }

    private fun deleteAnonymousUser() {
        lifecycleScope.launch {
            if (viewModel.findAllAssetIdSuspend().isEmpty()) {
                DeleteAccountPinBottomSheetDialogFragment.newInstance(null).showNow(parentFragmentManager, DeleteAccountPinBottomSheetDialogFragment.TAG)
            } else {
                DeleteAccountTipBottomSheetDialogFragment.newInstance()
                    .setContinueCallback { dialog ->
                        dialog.dismiss()
                        DeleteAccountPinBottomSheetDialogFragment.newInstance(null).showNow(parentFragmentManager, DeleteAccountPinBottomSheetDialogFragment.TAG)
                    }
                    .showNow(
                        parentFragmentManager,
                        DeleteAccountTipBottomSheetDialogFragment.TAG,
                    )
            }
        }
    }

    private fun showDialog(
        phone: String,
        callback: () -> Unit,
    ) {
        alertDialogBuilder()
            .setMessage(
                getString(
                    R.string.setting_delete_account_send,
                    phone,
                ),
            )
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
                callback.invoke()
            }
            .setPositiveButton(R.string.Continue) { dialog, _ ->
                lifecycleScope.launch {
                    verify(phone)
                    dialog.dismiss()
                    callback.invoke()
                }
            }
            .show()
    }

    private suspend fun verify(
        phone: String = requireNotNull(Session.getAccount()).phone,
        captchaResponse: Pair<CaptchaView.CaptchaType, String>? = null,
    ) {
        val verificationRequest =
            VerificationRequest(
                phone,
                VerificationPurpose.DEACTIVATED.name,
            )
        if (captchaResponse != null) {
            if (captchaResponse.first.isG()) {
                verificationRequest.gRecaptchaResponse = captchaResponse.second
            } else {
                verificationRequest.hCaptchaResponse = captchaResponse.second
            }
        }
        binding.deleteCover.isVisible = true
        handleMixinResponse(
            invokeNetwork = {
                viewModel.verification(verificationRequest)
            },
            successBlock = { response ->
                if (viewDestroyed()) return@handleMixinResponse
                binding.deleteCover.isVisible = false
                val verificationResponse = response.data!!
                activity?.addFragment(
                    this@DeleteAccountFragment,
                    VerificationFragment.newInstance(
                        verificationResponse.id,
                        phone,
                        null,
                        verificationResponse.hasEmergencyContact,
                        FROM_DELETE_ACCOUNT,
                    ),
                    VerificationFragment.TAG,
                )
            },
            failureBlock = { r ->
                if (viewDestroyed()) return@handleMixinResponse true
                if (r.errorCode == ErrorHandler.NEED_CAPTCHA) {
                    initAndLoadCaptcha()
                    return@handleMixinResponse true
                }
                binding.deleteCover.isVisible = false
                return@handleMixinResponse false
            },
            exceptionBlock = {
                if (viewDestroyed()) return@handleMixinResponse false
                binding.deleteCover.isVisible = false
                return@handleMixinResponse false
            },
        )
    }

    private fun initAndLoadCaptcha() =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            if (captchaView == null) {
                captchaView =
                    CaptchaView(
                        requireContext(),
                        object : CaptchaView.Callback {
                            override fun onStop() {
                                if (viewDestroyed()) return

                                binding.deleteCover.isVisible = false
                            }

                            override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                                lifecycleScope.launch {
                                    verify(captchaResponse = value)
                                }
                            }
                        },
                    )
                (view as ViewGroup).addView(
                    captchaView?.webView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            captchaView?.loadCaptcha(CaptchaView.CaptchaType.GCaptcha)
        }

    private fun changeNumber() {
        alert(getString(if (Session.hasPhone()) R.string.profile_modify_number else R.string.profile_add_number))
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(if (Session.hasPhone()) R.string.Change_Phone_Number else R.string.Add_Mobile_Number) { dialog, _ ->
                dialog.dismiss()
                if (Session.getAccount()?.hasPin == true) {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                        )
                            .add(
                                R.id.container,
                                VerifyFragment.newInstance(VerifyFragment.FROM_PHONE),
                            )
                            .addToBackStack(null)
                    }
                } else {
                    TipActivity.show(requireActivity(), TipType.Create, true)
                }
            }.show()
    }
}

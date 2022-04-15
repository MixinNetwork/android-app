package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentEmergencyContactBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.User

@AndroidEntryPoint
class EmergencyContactFragment : BaseFragment(R.layout.fragment_emergency_contact) {
    companion object {
        const val TAG = "EmergencyContactFragment"

        fun newInstance() = EmergencyContactFragment()
    }

    private var showEmergency = true

    private val viewModel by viewModels<EmergencyViewModel>()
    private val binding by viewBinding(FragmentEmergencyContactBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            enableRl.setOnClickListener {
                EmergencyContactTipBottomSheetDialogFragment.newInstance()
                    .showNow(parentFragmentManager, EmergencyContactTipBottomSheetDialogFragment.TAG)
            }
            viewRl.setOnClickListener {
                showEmergency = true
                val pinBottom = PinEmergencyBottomSheetDialog.newInstance()
                pinBottom.pinEmergencyCallback = bottomSheetCallback
                pinBottom.showNow(parentFragmentManager, PinEmergencyBottomSheetDialog.TAG)
            }
            changeRl.setOnClickListener {
                parentFragmentManager.inTransaction {
                    setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom
                    )
                        .add(R.id.container, VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY))
                        .addToBackStack(null)
                }
            }
            deleteRl.setOnClickListener {
                alertDialogBuilder()
                    .setMessage(getString(R.string.setting_emergency_remove_tip))
                    .setNegativeButton(R.string.Cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.Confirm) { dialog, _ ->
                        showEmergency = false
                        val pinBottom = PinEmergencyBottomSheetDialog.newInstance()
                        pinBottom.pinEmergencyCallback = bottomSheetCallback
                        pinBottom.showNow(parentFragmentManager, PinEmergencyBottomSheetDialog.TAG)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        setEmergencySet()
    }

    fun setEmergencySet() {
        binding.apply {
            if (Session.hasEmergencyContact()) {
                enableRl.isVisible = false
                viewRl.isVisible = true
                changeRl.isVisible = true
                deleteRl.isVisible = true
            } else {
                enableRl.isVisible = true
                viewRl.isVisible = false
                changeRl.isVisible = false
                deleteRl.isVisible = false
            }
        }
    }

    private fun fetchEmergencyContact(pinCode: String) = lifecycleScope.launch {
        binding.apply {
            viewPb.isVisible = true
            handleMixinResponse(
                invokeNetwork = { viewModel.showEmergency(pinCode) },
                successBlock = { response ->
                    val user = response.data as User
                    navTo(ViewEmergencyContactFragment.newInstance(user), ViewEmergencyContactFragment.TAG)
                },
                exceptionBlock = {
                    viewPb.isVisible = false
                    setEmergencySet()
                    return@handleMixinResponse false
                },
                doAfterNetworkSuccess = {
                    viewPb.isVisible = false
                    setEmergencySet()
                }
            )
        }
    }

    private fun deleteEmergencyContact(pinCode: String) = lifecycleScope.launch {
        binding.apply {
            deletePb.isVisible = true
            handleMixinResponse(
                invokeNetwork = { viewModel.deleteEmergency(pinCode) },
                successBlock = { response ->
                    val a = response.data as Account
                    Session.storeAccount(a)
                    Session.setHasEmergencyContact(a.hasEmergencyContact)
                    setEmergencySet()
                },
                exceptionBlock = {
                    deletePb.isVisible = false
                    setEmergencySet()
                    return@handleMixinResponse false
                },
                doAfterNetworkSuccess = {
                    deletePb.isVisible = false
                    setEmergencySet()
                }
            )
        }
    }

    private val bottomSheetCallback = object : PinEmergencyBottomSheetDialog.PinEmergencyCallback() {
        override fun onSuccess(pinCode: String) {
            if (showEmergency) {
                fetchEmergencyContact(pinCode)
            } else {
                deleteEmergencyContact(pinCode)
            }
        }
    }
}

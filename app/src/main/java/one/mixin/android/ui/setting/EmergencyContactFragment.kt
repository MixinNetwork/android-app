package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_emergency_contact.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.User

class EmergencyContactFragment : BaseViewModelFragment<EmergencyViewModel>() {
    companion object {
        const val TAG = "EmergencyContactFragment"

        fun newInstance() = EmergencyContactFragment()
    }

    override fun getModelClass(): Class<EmergencyViewModel> = EmergencyViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_emergency_contact, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        title_view.right_animator.setOnClickListener { context?.openUrl("") }
        enable_rl.setOnClickListener {
            EmergencyContactTipBottomSheetDialogFragment.newInstance()
                .showNow(requireFragmentManager(), EmergencyContactTipBottomSheetDialogFragment.TAG)
        }
        view_rl.setOnClickListener {
            val pinBottom = PinEmergencyBottomSheetDialog.newInstance()
            pinBottom.pinEmergencyCallback = bottomSheetCallback
            pinBottom.showNow(requireFragmentManager(), PinEmergencyBottomSheetDialog.TAG)
        }
        change_rl.setOnClickListener {
            requireFragmentManager().inTransaction {
                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                    R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .add(R.id.container, VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY))
                    .addToBackStack(null)
            }
        }
        setEmergencySet()
    }

    fun setEmergencySet() {
        if (Session.hasEmergencyContact()) {
            enable_rl.isVisible = false
            view_rl.isVisible = true
            change_rl.isVisible = true
        } else {
            enable_rl.isVisible = true
            view_rl.isVisible = false
            change_rl.isVisible = false
        }
    }

    private fun fetchEmergencyContact(pinCode: String) = lifecycleScope.launch {
        view_pb.isVisible = true
        handleMixinResponse(
            invokeNetwork = { viewModel.showEmergency(pinCode) },
            switchContext = Dispatchers.IO,
            successBlock = { response ->
                val user = response.data as User
                navTo(ViewEmergencyContactFragment.newInstance(user), ViewEmergencyContactFragment.TAG)
            },
            exceptionBlock = {
                view_pb.isVisible = false
                setEmergencySet()
            },
            doAfterNetworkSuccess = {
                view_pb.isVisible = false
                setEmergencySet()
            }
        )
    }

    private val bottomSheetCallback = object : PinEmergencyBottomSheetDialog.PinEmergencyCallback() {
        override fun onSuccess(pinCode: String) {
            fetchEmergencyContact(pinCode)
        }
    }
}
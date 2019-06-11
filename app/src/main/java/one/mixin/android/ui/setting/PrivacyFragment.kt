package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_privacy.*
import kotlinx.android.synthetic.main.view_emergency_bottom.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.EmergencyContact.HAS_EMERGENCY_CONTACT
import one.mixin.android.Constants.EmergencyContact.SHOWN_EMERGENCY_TIP
import one.mixin.android.R
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.Session
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

class PrivacyFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "PrivacyFragment"

        fun newInstance() = PrivacyFragment()
    }

    private var emergencyContact: User? = null

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_privacy, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        viewModel.countBlockingUsers().observe(this, Observer {
            it?.let { users ->
                blocking_tv.text = "${users.size}"
            }
        })
        blocked_rl.setOnClickListener {
            navTo(SettingBlockedFragment.newInstance(), SettingBlockedFragment.TAG)
        }
        conversation_rl.setOnClickListener {
            navTo(SettingConversationFragment.newInstance(), SettingConversationFragment.TAG)
        }
        auth_rl.setOnClickListener {
            navTo(AuthenticationsFragment.newInstance(), AuthenticationsFragment.TAG)
        }
        emergency_rl.setOnClickListener {
            val shown = defaultSharedPreferences.getBoolean(SHOWN_EMERGENCY_TIP, false)
            if (shown) {
                showEmergencyBottom()
            } else {
                EmergencyContactTipBottomSheetDialogFragment.newInstance()
                    .showNow(requireFragmentManager(), EmergencyContactTipBottomSheetDialogFragment.TAG)
                defaultSharedPreferences.putBoolean(SHOWN_EMERGENCY_TIP, true)
            }
        }

        fetchEmergencyContact()
    }

    private fun fetchEmergencyContact() = lifecycleScope.launch {
        val response = try {
            withContext(Dispatchers.IO) {
                viewModel.showEmergency()
            }
        } catch (ignored: Throwable) {
            return@launch
        }
        if (response.isSuccess) {
            val userId = (response.data as VerificationResponse).contactId ?: return@launch
            val user = withContext(Dispatchers.IO) {
                viewModel.findUserById(userId)
            } ?: return@launch
            val t = user.identityNumber
            if (t.length <= 1) {
                emergency_id_tv.text = t
            } else {
                emergency_id_tv.text = t.replaceRange(1, t.length, "*".repeat(t.length - 1))
            }
            defaultSharedPreferences.putBoolean(HAS_EMERGENCY_CONTACT, true)
            emergencyContact = user
        }
    }

    @SuppressLint("InflateParams")
    private fun showEmergencyBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_emergency_bottom, null)
        if (emergencyContact == null) {
            view.change.text = getString(R.string.setting_emergency_bottom_create)
        }
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.current.setOnClickListener {
            emergencyContact?.let {
                UserBottomSheetDialogFragment.newInstance(it)
                    .showNow(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
            }
            bottomSheet.dismiss()
        }
        view.change.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                activity?.supportFragmentManager?.inTransaction {
                    setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                        .add(R.id.container, VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY))
                        .addToBackStack(null)
                }
            } else {
                WalletActivity.show(requireActivity())
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }
}
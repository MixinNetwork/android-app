package one.mixin.android.ui.setting

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_verification_emergency_id.*
import kotlinx.android.synthetic.main.fragment_verification_emergency_id.back_iv
import kotlinx.android.synthetic.main.fragment_verification_emergency_id.verification_cover
import kotlinx.android.synthetic.main.fragment_verification_emergency_id.verification_keyboard
import kotlinx.android.synthetic.main.fragment_verification_emergency_id.verification_next_fab
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.EmergencyPurpose
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.extension.navTo
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.FabLoadingFragment
import one.mixin.android.ui.setting.VerificationEmergencyFragment.Companion.FROM_SESSION
import one.mixin.android.widget.Keyboard

@AndroidEntryPoint
class VerificationEmergencyIdFragment : FabLoadingFragment() {
    companion object {
        const val TAG = "VerificationEmergencyIdFragment"
        const val ARGS_PHONE = "args_phone"

        fun newInstance(
            phone: String
        ) = VerificationEmergencyIdFragment().withArgs {
            putString(ARGS_PHONE, phone)
        }
    }

    private val phone by lazy { requireArguments().getString(ARGS_PHONE) }

    private val viewModel by viewModels<EmergencyViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_verification_emergency_id, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_iv.setOnClickListener { activity?.onBackPressed() }
        verification_next_fab.setOnClickListener {
            sendCode(id_et.text.toString())
        }
        id_et.addTextChangedListener(watcher)
        id_et.showSoftInputOnFocus = false
        id_et.requestFocus()

        verification_keyboard.setOnClickKeyboardListener(mKeyboardListener)
    }

    private fun sendCode(mixinID: String) = lifecycleScope.launch {
        showLoading()
        handleMixinResponse(
            invokeNetwork = { viewModel.createEmergency(buildEmergencyRequest(mixinID)) },
            successBlock = { response ->
                navTo(
                    VerificationEmergencyFragment.newInstance(
                        verificationId = (response.data as VerificationResponse).id,
                        from = FROM_SESSION,
                        userIdentityNumber = mixinID
                    ),
                    VerificationEmergencyFragment.TAG
                )
            },
            doAfterNetworkSuccess = { hideLoading() },
            defaultExceptionHandle = {
                handleError(it)
            }
        )
    }

    override fun hideLoading() {
        if (!isAdded) return

        verification_next_fab.hide()
        verification_cover.visibility = View.GONE
    }

    private fun buildEmergencyRequest(mixinID: String) = EmergencyRequest(
        phone = phone,
        identityNumber = mixinID,
        purpose = EmergencyPurpose.SESSION.name
    )

    private fun handleEditView(str: String) {
        id_et.setSelection(id_et.text.toString().length)
        if (str.isNotBlank()) {
            verification_next_fab.visibility = VISIBLE
        } else {
            verification_next_fab.visibility = INVISIBLE
        }
    }

    private val mKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                id_et.setText(id_et.text.dropLast(1))
            } else {
                id_et.text = id_et.text.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                id_et.setText("")
            } else {
                id_et.text = id_et.text.append(value)
            }
        }
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            handleEditView(s.toString())
        }
    }
}

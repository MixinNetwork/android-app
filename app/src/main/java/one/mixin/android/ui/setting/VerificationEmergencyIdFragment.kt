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
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.EmergencyPurpose
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.databinding.FragmentVerificationBinding
import one.mixin.android.databinding.FragmentVerificationEmergencyIdBinding
import one.mixin.android.extension.navTo
import one.mixin.android.extension.tapVibrate
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

    private var _binding : FragmentVerificationEmergencyIdBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVerificationEmergencyIdBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backIv.setOnClickListener { activity?.onBackPressed() }
        binding.verificationNextFab.setOnClickListener {
            sendCode(binding.idEt.text.toString())
        }
        binding.idEt.addTextChangedListener(watcher)
        binding.idEt.showSoftInputOnFocus = false
        binding.idEt.requestFocus()
        binding.verificationKeyboard.setOnClickKeyboardListener(mKeyboardListener)
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
        binding.verificationNextFab.hide()
        binding.verificationCover.visibility = View.GONE
    }

    private fun buildEmergencyRequest(mixinID: String) = EmergencyRequest(
        phone = phone,
        identityNumber = mixinID,
        purpose = EmergencyPurpose.SESSION.name
    )

    private fun handleEditView(str: String) {
        binding.idEt.setSelection(binding.idEt.text.toString().length)
        if (str.isNotBlank()) {
            binding.verificationNextFab.visibility = VISIBLE
        } else {
            binding.verificationNextFab.visibility = INVISIBLE
        }
    }

    private val mKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tapVibrate()
            if (position == 11) {
                binding.idEt.setText(binding.idEt.text.dropLast(1))
            } else {
                binding.idEt.text = binding.idEt.text.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.tapVibrate()
            if (position == 11) {
                binding.idEt.setText("")
            } else {
                binding.idEt.text = binding.idEt.text.append(value)
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

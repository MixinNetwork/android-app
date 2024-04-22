package one.mixin.android.web3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.textColor
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class InputAddressFragment() : BaseFragment(R.layout.fragment_address_input) {

    companion object {
        const val TAG = "InputAddressFragment"
        const val ARGS_TOKEN = "args_token"
        fun newInstance(web3Token: Web3Token) = InputAddressFragment().apply {
            withArgs {
                putParcelable(ARGS_TOKEN, web3Token)
            }
        }
    }
    lateinit var token: Web3Token

    // for testing
    private lateinit var resultRegistry: ActivityResultRegistry

    // testing constructor
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        testRegistry: ActivityResultRegistry,
    ) : this() {
        resultRegistry = testRegistry
    }

    lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>
    private val binding by viewBinding(FragmentAddressInputBinding::bind)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) {
            resultRegistry =
                requireActivity().activityResultRegistry
        }

        getScanResult =
            registerForActivityResult(
                CaptureActivity.CaptureContract(),
                resultRegistry,
                ::callbackScan,
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = requireArguments().getParcelableCompat(ARGS_TOKEN, Web3Token::class.java)!!
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.rightAnimator.isEnabled = false
        binding.titleView.leftIb.setOnClickListener {
            if (viewDestroyed()) return@setOnClickListener

            if (binding.addrEt.isFocused) binding.addrEt.hideKeyboard()
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.continueTv.setOnClickListener {
            val destination = binding.addrEt.text.toString()
            navTo(InputFragment.newInstance(destination, token), InputFragment.TAG)
        }
        binding.addrIv.setOnClickListener {
            handleClick()
        }
        binding.addrEt.addTextChangedListener(mWatcher)
    }

    private fun handleClick() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private fun callbackScan(
        data: Intent?,
        isAddr: Boolean = true,
    ) {
        val text = data?.getStringExtra(ARGS_FOR_SCAN_RESULT)
        if (text != null) {
            if (isIcapAddress(text)) {
                binding.addrEt.setText(decodeICAP(text))
            } else {
                binding.addrEt.setText(text)
            }
        }
    }

    private val mWatcher: TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {
            }

            override fun afterTextChanged(s: Editable) {
                if (viewDestroyed()) return
                updateSaveButton()
            }
        }

    private fun updateSaveButton() {
        if (binding.addrEt.text.isNotEmpty() && isValidEthAddress(binding.addrEt.text.toString())) {
            binding.continueTv.isEnabled = true
            binding.continueTv.textColor = requireContext().getColor(R.color.white)
        } else {
            binding.continueTv.isEnabled = false
            binding.continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
        }
    }

    private fun isValidEthAddress(address: String): Boolean {
        return address.matches("^0x[0-9a-fA-F]{40}$".toRegex())
    }

}

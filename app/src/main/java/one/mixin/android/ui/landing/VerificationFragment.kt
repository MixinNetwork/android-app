package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.i2p.crypto.eddsa.EdDSAPublicKey
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.crypto.CryptoPreference
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.generateEd25519KeyPair
import one.mixin.android.databinding.FragmentVerificationBinding
import one.mixin.android.databinding.ViewVerificationBottomBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putInt
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.ui.common.PinCodeFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.ui.landing.MobileFragment.Companion.ARGS_FROM
import one.mixin.android.ui.landing.MobileFragment.Companion.ARGS_PHONE_NUM
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_CHANGE_PHONE_ACCOUNT
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_DELETE_ACCOUNT
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING
import one.mixin.android.ui.setting.VerificationEmergencyIdFragment
import one.mixin.android.ui.setting.delete.DeleteAccountPinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CaptchaView

@AndroidEntryPoint
class VerificationFragment : PinCodeFragment(R.layout.fragment_verification) {
    companion object {
        const val TAG: String = "VerificationFragment"
        private const val ARGS_ID = "args_id"
        const val ARGS_HAS_EMERGENCY_CONTACT = "args_has_emergency_contact"

        fun newInstance(
            id: String,
            phoneNum: String,
            pin: String? = null,
            hasEmergencyContact: Boolean = false,
            from: Int = FROM_LANDING
        ): VerificationFragment = VerificationFragment().apply {
            arguments = bundleOf(
                ARGS_ID to id,
                ARGS_PHONE_NUM to phoneNum,
                ARGS_PIN to pin,
                ARGS_HAS_EMERGENCY_CONTACT to hasEmergencyContact,
                ARGS_FROM to from
            )
        }
    }

    private val viewModel by viewModels<MobileViewModel>()

    private var mCountDownTimer: CountDownTimer? = null

    private val pin: String? by lazy {
        requireArguments().getString(ARGS_PIN)
    }
    private val from: Int by lazy {
        requireArguments().getInt(ARGS_FROM, FROM_LANDING)
    }
    private val phoneNum by lazy { requireArguments().getString(ARGS_PHONE_NUM)!! }

    private var captchaView: CaptchaView? = null

    private var hasEmergencyContact = false

    private val binding by viewBinding(FragmentVerificationBinding::bind)

    override fun getContentView() = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasEmergencyContact = requireArguments().getBoolean(ARGS_HAS_EMERGENCY_CONTACT)
        binding.pinVerificationTitleTv.text = getString(R.string.landing_validation_title, phoneNum)
        binding.verificationResendTv.setOnClickListener { sendVerification() }
        binding.verificationNeedHelpTv.setOnClickListener { showBottom() }

        startCountDown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCountDownTimer?.cancel()
    }

    override fun onBackPressed(): Boolean {
        if (captchaView?.isVisible() == true) {
            hideLoading()
            return true
        }
        return false
    }

    override fun clickNextFab() {
        when (from) {
            FROM_CHANGE_PHONE_ACCOUNT -> {
                handlePhoneModification()
            }
            FROM_DELETE_ACCOUNT -> {
                handleDeleteAccount()
            }
            else -> {
                handleLogin()
            }
        }
    }

    override fun insertUser(u: User) {
        viewModel.insertUser(u)
    }

    private fun isPhoneModification() = pin != null

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_verification_bottom, null)
        val viewBinding = ViewVerificationBottomBinding.bind(view)
        viewBinding.lostTv.isVisible = hasEmergencyContact && !isPhoneModification()
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        viewBinding.cantTv.setOnClickListener {
            requireContext().openUrl(getString(R.string.landing_verification_tip_url))
            bottomSheet.dismiss()
        }
        viewBinding.lostTv.setOnClickListener {
            navTo(VerificationEmergencyIdFragment.newInstance(phoneNum), VerificationEmergencyIdFragment.TAG)
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private fun handleDeleteAccount() {
        showLoading()
        viewModel.deactiveVerification(requireArguments().getString(ARGS_ID)!!, binding.pinVerificationView.code())
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<VerificationResponse> ->
                    binding.verificationNextFab.hide()
                    binding.verificationCover.visibility = GONE
                    if (!r.isSuccess) {
                        handleFailure(r)
                        return@subscribe
                    }
                    DeleteAccountPinBottomSheetDialogFragment.newInstance(
                        r.data!!.id
                    ).showNow(parentFragmentManager, DeleteAccountPinBottomSheetDialogFragment.TAG)
                },
                { t: Throwable ->
                    handleError(t)
                }
            )
    }

    private fun handlePhoneModification() {
        showLoading()
        viewModel.changePhone(requireArguments().getString(ARGS_ID)!!, binding.pinVerificationView.code(), pin = pin!!)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<Account> ->
                    binding.verificationNextFab.hide()
                    binding.verificationCover.visibility = GONE
                    if (!r.isSuccess) {
                        handleFailure(r)
                        return@subscribe
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val a = Session.getAccount()
                        a?.let {
                            val phone = requireArguments().getString(ARGS_PHONE_NUM) ?: return@launch
                            viewModel.updatePhone(a.userId, phone)
                            a.phone = phone
                            Session.storeAccount(a)
                        }
                        withContext(Dispatchers.Main) {
                            alert(getString(R.string.change_phone_success))
                                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                    dialog.dismiss()
                                    activity?.finish()
                                }
                                .show()
                        }
                    }
                },
                { t: Throwable ->
                    handleError(t)
                }
            )
    }

    private fun handleLogin() = lifecycleScope.launch {
        showLoading()

        SignalProtocol.initSignal(requireContext().applicationContext)
        val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
        val sessionKey = generateEd25519KeyPair()
        val publicKey = sessionKey.public as EdDSAPublicKey

        val sessionSecret = publicKey.abyte.base64Encode()
        val accountRequest = AccountRequest(
            binding.pinVerificationView.code(),
            registration_id = registrationId,
            purpose = VerificationPurpose.SESSION.name,
            pin = Session.getPinToken()?.let { encryptPin(it, pin) },
            session_secret = sessionSecret
        )

        handleMixinResponse(
            invokeNetwork = { viewModel.create(requireArguments().getString(ARGS_ID)!!, accountRequest) },
            successBlock = { response ->
                handleAccount(response, sessionKey) {
                    defaultSharedPreferences.putInt(PREF_LOGIN_FROM, FROM_LOGIN)
                }
            },
            doAfterNetworkSuccess = { hideLoading() },
            defaultErrorHandle = {
                handleFailure(it)
            },
            defaultExceptionHandle = {
                handleError(it)
            }
        )
    }

    override fun hideLoading() {
        super.hideLoading()
        captchaView?.webView?.visibility = GONE
    }

    private fun sendVerification(captchaResponse: Pair<CaptchaView.CaptchaType, String>? = null) {
        showLoading()
        val verificationRequest = VerificationRequest(
            requireArguments().getString(ARGS_PHONE_NUM),
            when {
                from == FROM_DELETE_ACCOUNT -> VerificationPurpose.DEACTIVATED.name
                isPhoneModification() -> VerificationPurpose.PHONE.name
                else -> VerificationPurpose.SESSION.name
            }
        )
        if (captchaResponse != null) {
            if (captchaResponse.first.isG()) {
                verificationRequest.gRecaptchaResponse = captchaResponse.second
            } else {
                verificationRequest.hCaptchaResponse = captchaResponse.second
            }
        }
        viewModel.verification(verificationRequest)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<VerificationResponse> ->
                    if (!r.isSuccess) {
                        if (r.errorCode == NEED_CAPTCHA) {
                            initAndLoadCaptcha()
                        } else {
                            hideLoading()
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        }
                    } else {
                        hasEmergencyContact = (r.data as VerificationResponse).hasEmergencyContact
                        hideLoading()
                        binding.pinVerificationView.clear()
                        startCountDown()
                    }
                },
                { t: Throwable ->
                    handleError(t)
                    binding.verificationNextFab.visibility = GONE
                    captchaView?.webView?.visibility = GONE
                }
            )
    }

    private fun initAndLoadCaptcha() {
        if (captchaView == null) {
            captchaView = CaptchaView(
                requireContext(),
                object : CaptchaView.Callback {
                    override fun onStop() {
                        hideLoading()
                    }

                    override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                        sendVerification(value)
                    }
                }
            )
            (view as ViewGroup).addView(captchaView?.webView, MATCH_PARENT, MATCH_PARENT)
        }
        captchaView?.loadCaptcha(CaptchaView.CaptchaType.GCaptcha)
    }

    private fun startCountDown() {
        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(60000, 1000) {

            override fun onTick(l: Long) {
                binding.verificationResendTv.text = getString(R.string.Resend_code_in, l / 1000)
            }

            override fun onFinish() {
                resetCountDown()
            }
        }
        mCountDownTimer?.start()
        binding.verificationResendTv.isEnabled = false
        context?.let {
            binding.verificationResendTv.setTextColor(ContextCompat.getColor(it, R.color.colorGray))
        }
    }

    private fun resetCountDown() {
        binding.verificationResendTv.setText(R.string.Resend_code)
        binding.verificationResendTv.isEnabled = true
        context?.let {
            binding.verificationResendTv.setTextColor(ContextCompat.getColor(it, R.color.colorBlue))
        }
        binding.verificationNeedHelpTv.isVisible = true
    }
}

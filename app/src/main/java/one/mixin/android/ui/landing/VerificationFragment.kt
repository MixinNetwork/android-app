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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
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
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.databinding.FragmentVerificationBinding
import one.mixin.android.databinding.ViewVerificationBottomBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putInt
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.ui.common.PinCodeFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.ui.landing.MobileFragment.Companion.ARGS_FROM
import one.mixin.android.ui.landing.MobileFragment.Companion.ARGS_PHONE_NUM
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_CHANGE_PHONE_ACCOUNT
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_DELETE_ACCOUNT
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING_CREATE
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_VERIFY_MOBILE_REMINDER
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.setting.VerificationEmergencyIdFragment
import one.mixin.android.ui.setting.delete.DeleteAccountPinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CaptchaView
import one.mixin.android.widget.CaptchaView.Companion.gtCAPTCHA
import one.mixin.android.widget.CaptchaView.Companion.hCAPTCHA
import timber.log.Timber
import javax.inject.Inject

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
            from: Int = FROM_LANDING,
        ): VerificationFragment =
            VerificationFragment().apply {
                arguments =
                    bundleOf(
                        ARGS_ID to id,
                        ARGS_PHONE_NUM to phoneNum,
                        ARGS_PIN to pin,
                        ARGS_HAS_EMERGENCY_CONTACT to hasEmergencyContact,
                        ARGS_FROM to from,
                    )
            }
    }

    private val viewModel by viewModels<MobileViewModel>()

    @Inject
    lateinit var tip: Tip

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        Timber.e("VerificationFragment onViewCreated")
        hasEmergencyContact = requireArguments().getBoolean(ARGS_HAS_EMERGENCY_CONTACT)
        binding.pinVerificationTitleTv.text = getString(R.string.landing_validation_title, phoneNum)
        binding.title.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        binding.verificationResendTv.setOnClickListener { sendVerification() }
        binding.verificationNeedHelpTv.setOnClickListener { showBottom() }

        if (from == FROM_LANDING_CREATE) {
            AnalyticsTracker.trackSignUpSmsVerify()
        } else if (from == FROM_LOGIN) {
            AnalyticsTracker.trackLoginSmsVerify()
        }

        startCountDown()
    }

    private fun applySafeTopPadding(rootView: View) {
        val originalPaddingTop: Int = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, insets: WindowInsetsCompat ->
            val topInset: Int = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            v.setPadding(v.paddingLeft, originalPaddingTop + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
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
            FROM_VERIFY_MOBILE_REMINDER -> {
                handleVerifyMobileReminder()
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
            requireContext().openUrl(getString(R.string.landing_verification_url))
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
                        r.data!!.id,
                    ).showNow(parentFragmentManager, DeleteAccountPinBottomSheetDialogFragment.TAG)
                },
                { t: Throwable ->
                    handleError(t)
                },
            )
    }

    private fun handlePhoneModification() =
        lifecycleScope.launch {
            showLoading()
            handleMixinResponse(
                invokeNetwork = {
                    if (pin != null) {
                        val seed = tip.getOrRecoverTipPriv(requireContext(), pin!!).getOrThrow()
                        tip.checkSalt(requireContext(), pin!!, seed)
                        val saltBase64 = if (Session.hasPhone()) {
                            tip.getEncryptSalt(requireContext(), pin!!, seed)
                        } else {
                            // User real salt
                            tip.getEncryptSalt(requireContext(), pin!!, seed, false)
                        }
                        viewModel.changePhone(requireArguments().getString(ARGS_ID)!!, binding.pinVerificationView.code(), pin = pin!!, saltBase64)
                    } else {
                        viewModel.changePhone(requireArguments().getString(ARGS_ID)!!, binding.pinVerificationView.code(), pin = pin!!)
                    }
                },
                successBlock = {
                    val hasPhone = Session.hasPhone()
                    withContext(Dispatchers.IO) {
                        val a = Session.getAccount()
                        a?.let {
                            val phone =
                                requireArguments().getString(ARGS_PHONE_NUM)
                                    ?: return@withContext
                            viewModel.updatePhone(a.userId, phone)
                            removeValueFromEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC)
                            a.phone = phone
                            Session.storeAccount(a)
                        }
                    }
                    alert(
                        getString(
                            if (hasPhone) R.string.Changed
                            else R.string.Added
                        )
                    )
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            if (activity !is MainActivity) {
                                activity?.finish()
                            } else {
                                activity?.finish()
                                MainActivity.show(requireActivity())
                            }
                        }
                        .show()
                },
                doAfterNetworkSuccess = { hideLoading() },
                defaultErrorHandle = {
                    handleFailure(it)
                },
                defaultExceptionHandle = {
                    if (it is TipNetworkException) {
                        handleFailure(it.error)
                    } else {
                        handleError(it)
                    }
                },
            )
        }

    private fun handleLogin() =
        lifecycleScope.launch {
            showLoading()

            SignalProtocol.initSignal(requireContext().applicationContext)
            val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
            val sessionKey = generateEd25519KeyPair()
            val publicKey = sessionKey.publicKey

            val sessionSecret = publicKey.base64Encode()
            val accountRequest =
                AccountRequest(
                    binding.pinVerificationView.code(),
                    registrationId = registrationId,
                    purpose = VerificationPurpose.SESSION.name,
                    sessionSecret = sessionSecret,
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
                },
            )
        }

    private fun handleVerifyMobileReminder() =
        lifecycleScope.launch {
            showLoading()
            val accountRequest =
                AccountRequest(
                    binding.pinVerificationView.code(),
                    purpose = VerificationPurpose.NONE.name,
                )
            handleMixinResponse(
                invokeNetwork = { viewModel.create(requireArguments().getString(ARGS_ID)!!, accountRequest) },
                successBlock = { r ->
                    withContext(Dispatchers.IO) {
                        r.data?.let { data ->
                            Session.storeAccount(data)
                        }
                    }
                    alert(
                        getString(R.string.verification_successful))
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            activity?.finish()
                            MainActivity.show(requireActivity())
                        }
                        .show()
                },
                doAfterNetworkSuccess = { hideLoading() },
                defaultErrorHandle = {
                    handleFailure(it)
                },
                defaultExceptionHandle = {
                    handleError(it)
                },
            )
        }

    override fun hideLoading() {
        super.hideLoading()
        captchaView?.webView?.visibility = GONE
    }

    private fun sendVerification(captchaResponse: Pair<CaptchaView.CaptchaType, String>? = null) {
        showLoading()
        val verificationRequest =
            VerificationRequest(
                requireArguments().getString(ARGS_PHONE_NUM),
                when {
                    from == FROM_DELETE_ACCOUNT -> VerificationPurpose.DEACTIVATED.name
                    isPhoneModification() -> VerificationPurpose.PHONE.name
                    from == FROM_VERIFY_MOBILE_REMINDER -> VerificationPurpose.NONE.name
                    else -> VerificationPurpose.SESSION.name
                },
            )
        if (captchaResponse != null) {
            if (captchaResponse.first.isG()) {
                verificationRequest.gRecaptchaResponse = captchaResponse.second
            } else if (captchaResponse.first.isH()) {
                verificationRequest.hCaptchaResponse = captchaResponse.second
            } else {
                val t = GTCaptcha4Utils.parseGTCaptchaResponse(captchaResponse.second)
                verificationRequest.lotNumber = t?.lotNumber
                verificationRequest.captchaOutput = t?.captchaOutput
                verificationRequest.passToken = t?.passToken
                verificationRequest.genTime = t?.genTime
            }
        }
        viewModel.verification(verificationRequest)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<VerificationResponse> ->
                    if (!r.isSuccess) {
                        if (r.errorCode == NEED_CAPTCHA) {
                            initAndLoadCaptcha(r.errorDescription)
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
                },
            )
    }

    private fun initAndLoadCaptcha(errorDescription: String) =
        lifecycleScope.launch {
            if (captchaView == null) {
                captchaView =
                    CaptchaView(
                        requireContext(),
                        object : CaptchaView.Callback {
                            override fun onStop() {
                                hideLoading()
                            }

                            override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                                sendVerification(value)
                            }
                        },
                    )
                (view as ViewGroup).addView(captchaView?.webView, MATCH_PARENT, MATCH_PARENT)
            }
            captchaView?.loadCaptcha(
                if (errorDescription.containsIgnoreCase(gtCAPTCHA)) CaptchaView.CaptchaType.GTCaptcha
                else if (errorDescription.containsIgnoreCase(hCAPTCHA)) CaptchaView.CaptchaType.HCaptcha
                else CaptchaView.CaptchaType.GCaptcha
            )
        }

    private fun startCountDown() {
        mCountDownTimer?.cancel()
        mCountDownTimer =
            object : CountDownTimer(60000, 1000) {
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

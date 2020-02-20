package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_verification.*
import kotlinx.android.synthetic.main.view_verification_bottom.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.crypto.CryptoPreference
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.crypto.getPublicKey
import one.mixin.android.extension.alert
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putInt
import one.mixin.android.ui.common.PinCodeFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.ui.landing.MobileFragment.Companion.ARGS_PHONE_NUM
import one.mixin.android.ui.setting.VerificationEmergencyIdFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_RECAPTCHA
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.RecaptchaView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class VerificationFragment : PinCodeFragment<MobileViewModel>() {
    companion object {
        const val TAG: String = "VerificationFragment"
        private const val ARGS_ID = "args_id"
        const val ARGS_HAS_EMERGENCY_CONTACT = "args_has_emergency_contact"

        fun newInstance(
            id: String,
            phoneNum: String,
            pin: String? = null,
            hasEmergencyContact: Boolean = false
        ): VerificationFragment = VerificationFragment().apply {
            arguments = bundleOf(
                ARGS_ID to id,
                ARGS_PHONE_NUM to phoneNum,
                ARGS_PIN to pin,
                ARGS_HAS_EMERGENCY_CONTACT to hasEmergencyContact
            )
        }
    }

    override fun getModelClass() = MobileViewModel::class.java

    private var mCountDownTimer: CountDownTimer? = null

    private val pin: String? by lazy {
        arguments!!.getString(ARGS_PIN)
    }
    private val phoneNum by lazy { arguments!!.getString(ARGS_PHONE_NUM)!! }

    private var recaptchaView: RecaptchaView? = null

    private var hasEmergencyContact = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_verification, container, false) as ViewGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasEmergencyContact = arguments!!.getBoolean(ARGS_HAS_EMERGENCY_CONTACT)
        pin_verification_title_tv.text = getString(R.string.landing_validation_title, phoneNum)
        verification_resend_tv.setOnClickListener { sendVerification() }
        verification_need_help_tv.setOnClickListener { showBottom() }

        startCountDown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCountDownTimer?.cancel()
    }

    override fun onBackPressed(): Boolean {
        if (recaptchaView?.isVisible() == true) {
            hideLoading()
            return true
        }
        return false
    }

    override fun clickNextFab() {
        if (isPhoneModification()) {
            handlePhoneModification()
        } else {
            handleLogin()
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
        view.lost_tv.isVisible = hasEmergencyContact && !isPhoneModification()
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.cant_tv.setOnClickListener {
            requireContext().openUrl(getString(R.string.landing_verification_tip_url))
            bottomSheet.dismiss()
        }
        view.lost_tv.setOnClickListener {
            navTo(VerificationEmergencyIdFragment.newInstance(phoneNum), VerificationEmergencyIdFragment.TAG)
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private fun handlePhoneModification() {
        showLoading()
        viewModel.changePhone(arguments!!.getString(ARGS_ID)!!, pin_verification_view.code(), pin = pin!!)
            .autoDispose(stopScope).subscribe({ r: MixinResponse<Account> ->
                verification_next_fab.hide()
                verification_cover.visibility = GONE
                if (!r.isSuccess) {
                    handleFailure(r)
                    return@subscribe
                }
                doAsync {
                    val a = Session.getAccount()
                    a?.let {
                        val phone = arguments!!.getString(ARGS_PHONE_NUM) ?: return@doAsync
                        viewModel.updatePhone(a.userId, phone)
                        a.phone = phone
                        Session.storeAccount(a)
                    }
                    uiThread {
                        alert(getString(R.string.change_phone_success))
                            .setPositiveButton(android.R.string.yes) { dialog, _ ->
                                dialog.dismiss()
                                activity?.finish()
                            }
                            .show()
                    }
                }
            }, { t: Throwable ->
                handleError(t)
            })
    }

    private fun handleLogin() = lifecycleScope.launch {
        showLoading()

        SignalProtocol.initSignal(requireContext().applicationContext)
        val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
        val sessionKey = generateRSAKeyPair()
        val sessionSecret = sessionKey.getPublicKey().base64Encode()
        val accountRequest = AccountRequest(pin_verification_view.code(),
            registration_id = registrationId,
            purpose = VerificationPurpose.SESSION.name,
            pin = Session.getPinToken()?.let { encryptPin(it, pin) },
            session_secret = sessionSecret)

        handleMixinResponse(
            invokeNetwork = { viewModel.create(arguments!!.getString(ARGS_ID)!!, accountRequest) },
            switchContext = Dispatchers.IO,
            successBlock = { response ->
                defaultSharedPreferences.putInt(PREF_LOGIN_FROM, FROM_LOGIN)
                handleAccount(response, sessionKey)
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
        recaptchaView?.webView?.visibility = GONE
    }

    private fun sendVerification(gRecaptchaResponse: String? = null) {
        showLoading()
        val verificationRequest = VerificationRequest(
            arguments!!.getString(ARGS_PHONE_NUM),
            if (isPhoneModification()) VerificationPurpose.PHONE.name else VerificationPurpose.SESSION.name,
            gRecaptchaResponse)
        viewModel.verification(verificationRequest)
            .autoDispose(stopScope).subscribe({ r: MixinResponse<VerificationResponse> ->
                if (!r.isSuccess) {
                    if (r.errorCode == NEED_RECAPTCHA) {
                        initAndLoadRecaptcha()
                    } else {
                        hideLoading()
                        ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                    }
                } else {
                    hasEmergencyContact = (r.data as VerificationResponse).hasEmergencyContact
                    hideLoading()
                    pin_verification_view?.clear()
                    startCountDown()
                }
            }, { t: Throwable ->
                handleError(t)
                verification_next_fab.visibility = GONE
                recaptchaView?.webView?.visibility = GONE
            })
    }

    private fun initAndLoadRecaptcha() {
        if (recaptchaView == null) {
            recaptchaView = RecaptchaView(requireContext(), object : RecaptchaView.Callback {
                override fun onStop() {
                    hideLoading()
                }

                override fun onPostToken(value: String) {
                    sendVerification(value)
                }
            })
            (view as ViewGroup).addView(recaptchaView?.webView, MATCH_PARENT, MATCH_PARENT)
        }
        recaptchaView?.loadRecaptcha()
    }

    private fun startCountDown() {
        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(60000, 1000) {

            override fun onTick(l: Long) {
                if (verification_resend_tv != null)
                    verification_resend_tv.text = getString(R.string.landing_resend_code_disable, l / 1000)
            }

            override fun onFinish() {
                resetCountDown()
            }
        }
        mCountDownTimer?.start()
        verification_resend_tv.isEnabled = false
        context?.getColor(R.color.colorGray)?.let { verification_resend_tv.setTextColor(it) }
    }

    private fun resetCountDown() {
        if (verification_resend_tv != null) {
            verification_resend_tv.setText(R.string.landing_resend_code_enable)
            verification_resend_tv.isEnabled = true
            context?.getColor(R.color.colorBlue)?.let { verification_resend_tv.setTextColor(it) }
        }
        verification_need_help_tv?.isVisible = true
    }
}

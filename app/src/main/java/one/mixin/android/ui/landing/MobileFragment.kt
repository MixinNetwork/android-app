package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.View
import android.view.View.AUTOFILL_HINT_PHONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.databinding.FragmentMobileBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.dp
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.web.WebFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.isAnonymousNumber
import one.mixin.android.util.isValidNumber
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.util.xinDialCode
import one.mixin.android.widget.CaptchaView
import one.mixin.android.widget.CaptchaView.Companion.gtCAPTCHA
import one.mixin.android.widget.CaptchaView.Companion.hCAPTCHA
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.countrypicker.Country
import one.mixin.android.widget.countrypicker.CountryPicker
import timber.log.Timber

@AndroidEntryPoint
class MobileFragment: BaseFragment(R.layout.fragment_mobile) {
    companion object {
        const val TAG: String = "MobileFragment"
        const val ARGS_PHONE_NUM = "args_phone_num"
        const val ARGS_FROM = "args_from"
        const val FROM_LANDING = 0
        const val FROM_LANDING_CREATE = 1
        const val FROM_CHANGE_PHONE_ACCOUNT = 2
        const val FROM_DELETE_ACCOUNT = 3
        const val FROM_VERIFY_MOBILE_REMINDER = 4

        fun newInstance(
            pin: String? = null,
            from: Int = FROM_LANDING,
            phoneNumber: String? = null,
        ): MobileFragment =
            MobileFragment().apply {
                val b =
                    Bundle().apply {
                        if (pin != null) {
                            putString(ARGS_PIN, pin)
                        }
                        putInt(ARGS_FROM, from)
                        if (!phoneNumber.isNullOrBlank()) {
                            putString(ARGS_PHONE_NUM, phoneNumber)
                        }
                    }
                arguments = b
            }
    }

    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentMobileBinding::bind)

    private lateinit var countryPicker: CountryPicker
    private var mCountry: Country? = null
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private var phoneNumber: Phonenumber.PhoneNumber? = null
    private var anonymousNumber: String? = null

    private var pin: String? = null
    private val from: Int by lazy {
        requireArguments().getInt(ARGS_FROM, FROM_LANDING)
    }

    private val presetPhoneNumber: String? by lazy {
        requireArguments().getString(ARGS_PHONE_NUM)
    }

    private var captchaView: CaptchaView? = null

    private val mixinCountry by lazy {
        Country().apply {
            name = getString(R.string.Mixin)
            code = getString(R.string.Mixin)
            flag = R.drawable.flag_mixin
            dialCode = xinDialCode
        }
    }

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        Timber.e("MobileFragment onViewCreated")
        binding.apply {
            pin = requireArguments().getString(ARGS_PIN)
            if (pin != null) {
                titleSwitcher.setCurrentText(getString(R.string.Enter_new_phone_number))
            }
            titleView.setOnLongClickListener{
                LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
                true
            }
            binding.titleView.leftIb.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            binding.titleView.rightIb.setOnClickListener {
                val bundle = Bundle().apply {
                    putString(WebFragment.URL, Constants.HelpLink.CUSTOMER_SERVICE)
                    putBoolean(WebFragment.ARGS_INJECTABLE, false)
                }
                navTo(WebFragment.newInstance(bundle), WebFragment.TAG)
            }
            val policy: String = requireContext().getString(R.string.Privacy_Policy)
            val termsService: String = requireContext().getString(R.string.Terms_of_Service)
            val policyWrapper = requireContext().getString(R.string.landing_introduction, "**$policy**", "**$termsService**")
            val policyUrl = getString(R.string.landing_privacy_policy_url)
            val termsUrl = getString(R.string.landing_terms_url)
            binding.introductionTv.highlightStarTag(
                policyWrapper,
                arrayOf(policyUrl, termsUrl),
            )
            binding.orLl.isVisible = from == FROM_LANDING
            binding.mnemonicPhrase.isVisible = from == FROM_LANDING
            binding.noAccount.isVisible = from == FROM_LANDING

            countryIconIv.setOnClickListener { showCountry() }
            countryCodeEt.addTextChangedListener(countryCodeWatcher)
            countryCodeEt.showSoftInputOnFocus = false
            continueBn.setOnClickListener {
                if (from == FROM_VERIFY_MOBILE_REMINDER
                    && presetPhoneNumber != null
                ) {
                    requestSend()
                } else {
                    showDialog()
                }
            }
            mobileEt.showSoftInputOnFocus = false
            mobileEt.addTextChangedListener(mWatcher)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mobileEt.setAutofillHints(AUTOFILL_HINT_PHONE)
            }
            mobileCover.isClickable = true
            countryPicker = CountryPicker.newInstance()
            countryPicker.setListener { _: String, code: String, dialCode: String, flagResId: Int ->
                mCountry = Country()
                mCountry?.code = code
                mCountry?.dialCode = dialCode
                mCountry?.flag = flagResId
                countryIconIv.setImageResource(flagResId)
                countryCodeEt.setText(dialCode)
                handleEditView()
                activity?.supportFragmentManager?.popBackStackImmediate()
                countryIconIv.hideKeyboard()

                mobileEt.requestFocus()
                mobileEt.setSelection(mobileEt.text?.length ?: 0)
                updateMobileOrAnonymous(dialCode)
            }
            getUserCountryInfo()

            if (from == FROM_VERIFY_MOBILE_REMINDER) {
                presetPhoneNumber?.let { phoneNumber ->
                    presetVerifyMobilePhoneNumber(phoneNumber)
                }
            }

            keyboard.tipTitleEnabled = false
            keyboard.initPinKeys()
            keyboard.setOnClickKeyboardListener(mKeyboardListener)
            mnemonicPhrase.setOnClickListener {
                AnalyticsTracker.trackLoginMnemonicPhrase()
                activity?.addFragment(
                    this@MobileFragment,
                    LandingMnemonicPhraseFragment.newInstance(),
                    LandingMnemonicPhraseFragment.TAG
                )
            }
            noAccount.setOnClickListener {
                CreateAccountConfirmBottomSheetDialogFragment.newInstance()
                    .setOnCreateAccount {
                        activity?.addFragment(
                            this@MobileFragment,
                            MnemonicPhraseFragment.newInstance(),
                            MnemonicPhraseFragment.TAG,
                        )
                    }
                    .setOnPrivacyPolicy {
                        activity?.openUrl(getString(R.string.landing_privacy_policy_url))
                    }
                    .setOnTermsOfService {
                        activity?.openUrl(getString(R.string.landing_terms_url))
                    }
                    .showNow(parentFragmentManager, CreateAccountConfirmBottomSheetDialogFragment.TAG)
            }
        }
        setupFocusListeners()
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

    private fun presetVerifyMobilePhoneNumber(phoneNumber: String) {
        if (viewDestroyed()) return

        val parsedPhoneNumber: Phonenumber.PhoneNumber? = runCatching {
            phoneUtil.parse(phoneNumber, null)
        }.getOrNull()

        if (parsedPhoneNumber == null) {
            return
        }

        val dialCode: String = "+${parsedPhoneNumber.countryCode}"
        val country: Country? =
            if (dialCode == xinDialCode) {
                mixinCountry
            } else {
                countryPicker.getCountryByDialCode(dialCode)
            }
        if (country != null) {
            mCountry = country
            binding.countryIconIv.setImageResource(country.flag)
            binding.countryCodeEt.setText(country.dialCode)
        }
        binding.mobileTv.setText(getString(R.string.Confirm_your_mobile_number))
        binding.mobileEt.setText(parsedPhoneNumber.nationalNumber.toString())
        binding.countryIconIv.isEnabled = false
        binding.countryCodeEt.isEnabled = false
        binding.mobileEt.isEnabled = false
        binding.mobileCover.isClickable = false
        handleEditView()
    }

    override fun onBackPressed(): Boolean {
        if (captchaView?.isVisible() == true) {
            hideLoading()
            return true
        }
        if (binding.keyboard.translationY == 0f) {
            binding.mobileEt.clearFocus()
            binding.countryCodeEt.clearFocus()
            return true
        }
        if (pin == null) {
            activity?.supportFragmentManager?.popBackStackImmediate()
            return true
        }
        return false
    }

    private fun showDialog() {
        alertDialogBuilder()
            .setMessage(
                getString(
                    if (anonymousNumber != null) R.string.landing_anonymous_dialog_content else R.string.landing_invitation_dialog_content,
                    mCountry?.dialCode + " " + binding.mobileEt.text.toString(),
                ),
            )
            .setNegativeButton(R.string.Change) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                requestSend()
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        requireActivity().currentFocus?.clearFocus()
        requireActivity().hideKeyboard()
        binding.mobileEt.hideKeyboard()
    }

    private fun requestSend(captchaResponse: Pair<CaptchaView.CaptchaType, String>? = null) {
        if (viewDestroyed()) return

        binding.continueBn.isEnabled = true
        binding.continueBn.displayedChild = 0
        binding.mobileCover.isVisible = true
        val phoneNum = anonymousNumber ?: phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
        val verificationRequest =
            VerificationRequest(
                phoneNum,
                when (from) {
                    FROM_DELETE_ACCOUNT -> {
                        VerificationPurpose.DEACTIVATED.name
                    }

                    FROM_CHANGE_PHONE_ACCOUNT -> {
                        VerificationPurpose.PHONE.name
                    }

                    FROM_VERIFY_MOBILE_REMINDER -> {
                        VerificationPurpose.NONE.name
                    }

                    else -> {
                        VerificationPurpose.SESSION.name
                    }
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
        binding.continueBn.displayedChild = 1
        mobileViewModel.loginVerification(verificationRequest)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<VerificationResponse> ->
                    if (!r.isSuccess) {
                        if (r.errorCode == NEED_CAPTCHA) {
                            initAndLoadCaptcha(r.errorDescription)
                        } else {
                            hideLoading()
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        }
                        return@subscribe
                    }
                    hideLoading()

                    val verificationResponse = r.data as VerificationResponse
                    if (!r.data?.deactivationEffectiveAt.isNullOrBlank() && from == FROM_LANDING) {
                        LandingDeleteAccountFragment.newInstance(r.data?.deactivationRequestedAt, r.data?.deactivationEffectiveAt)
                            .setContinueCallback {
                                activity?.addFragment(
                                    this@MobileFragment,
                                    VerificationFragment.newInstance(
                                        verificationResponse.id,
                                        phoneNum,
                                        pin,
                                        verificationResponse.hasEmergencyContact,
                                        from,
                                    ),
                                    VerificationFragment.TAG,
                                )
                            }.showNow(parentFragmentManager, LandingDeleteAccountFragment.TAG)
                    } else {
                        activity?.addFragment(
                            this@MobileFragment,
                            VerificationFragment.newInstance(
                                verificationResponse.id,
                                phoneNum,
                                pin,
                                verificationResponse.hasEmergencyContact,
                                from,
                            ),
                            VerificationFragment.TAG,
                        )
                    }
                },
                { t: Throwable ->
                    hideLoading()
                    ErrorHandler.handleError(t)
                    reportException("$TAG loginVerification", t)
                    Timber.e(t)
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
                                if (viewDestroyed()) return

                                binding.continueBn.isEnabled = true
                                binding.continueBn.displayedChild = 0
                                binding.mobileCover.isVisible = false
                            }

                            override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                                requestSend(value)
                            }
                        },
                    )
                (view as ViewGroup).addView(captchaView?.webView, MATCH_PARENT, MATCH_PARENT)
            }
            if (from == FROM_LANDING_CREATE) {
                AnalyticsTracker.trackSignUpCaptcha("phone_number")
            } else if (from == FROM_LANDING) {
                AnalyticsTracker.trackLoginCaptcha("phone_number")
            }
            captchaView?.loadCaptcha(
                if (errorDescription.containsIgnoreCase(gtCAPTCHA)) CaptchaView.CaptchaType.GTCaptcha
                else if (errorDescription.containsIgnoreCase(hCAPTCHA)) CaptchaView.CaptchaType.HCaptcha
                else CaptchaView.CaptchaType.GCaptcha
            )
        }

    private fun hideLoading() {
        if (viewDestroyed()) return
        binding.continueBn.isEnabled = true
        binding.continueBn.displayedChild = 0
        binding.mobileCover.isVisible = false
        captchaView?.hide()
    }

    private fun handleEditView() {
        if (viewDestroyed()) return

        binding.apply {
            val country = mCountry
            if (country == null) {
                continueBn.isEnabled = false
                return
            }

            val mobileText = mobileEt.text.toString()
            mobileEt.setSelection(mobileText.length)
            val dialCode = country.dialCode
            val number = country.dialCode + mobileText
            val valid =
                if (dialCode == xinDialCode) {
                    val r = isAnonymousNumber(number, dialCode)
                    anonymousNumber = if (r) number else null
                    r
                } else {
                    anonymousNumber = null
                    val validResult = isValidNumber(phoneUtil, number, country.code, country.dialCode)
                    phoneNumber = validResult.second
                    validResult.first
                }
            continueBn.isEnabled = (countryCodeEt.text?.length ?: 0) > 1 && mobileText.isNotEmpty() && valid
        }
    }

    private fun getUserCountryInfo() {
        if (viewDestroyed()) return

        countryPicker.getUserCountryInfo(context).apply {
            mCountry = this
            binding.countryIconIv.setImageResource(flag)
            binding.countryCodeEt.setText(dialCode)
            countryPicker.setLocationCountry(this)
        }
    }

    private fun showCountry() {
        try {
            activity?.supportFragmentManager?.inTransaction {
                setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                    .add(R.id.container, countryPicker).addToBackStack(null)
            }
        } catch (e: Exception) {
            val msg = "open countryPicker ${e.stackTraceToString()}"
            Timber.e(msg)
            reportException("$TAG show country picker", RuntimeException(msg))
        }
    }

    private fun updateMobileOrAnonymous(dialCode: String) {
        if (viewDestroyed()) return

        binding.apply {
            if (dialCode == xinDialCode) {
                mobileEt.hint = getString(R.string.Anonymous_Number)
                if (titleSwitcher.displayedChild != 1) {
                    if (pin != null) {
                        titleSwitcher.setText(getString(R.string.Enter_new_anonymous_number))
                    } else {
                        titleSwitcher.setText(getString(R.string.Enter_your_anonymous_number))
                    }
                }
            } else {
                mobileEt.hint = getString(R.string.Phone_Number)
                if (titleSwitcher.displayedChild != 0) {
                    if (pin != null && presetPhoneNumber == null) {
                        titleSwitcher.setText(getString(R.string.Enter_new_phone_number))
                    } else {
                        titleSwitcher.setText(getString(R.string.Enter_your_phone_number))
                    }
                }
            }
        }
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener =
        object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(
                position: Int,
                value: String,
            ) {
                context?.tickVibrate()
                if (viewDestroyed()) {
                    return
                }
                binding.apply {
                    if (mobileEt.isFocused) {
                        val editable = mobileEt.text
                        val start = mobileEt.selectionStart
                        val end = mobileEt.selectionEnd
                        try {
                            if (position == 11) {
                                if (editable.isNullOrEmpty()) {
                                    countryCodeEt.requestFocus()
                                    countryCodeEt.setSelection(countryCodeEt.text?.length ?: 0)
                                    return
                                }

                                if (start == end) {
                                    if (start == 0) {
                                        mobileEt.text?.delete(0, end)
                                    } else {
                                        mobileEt.text?.delete(start - 1, end)
                                    }
                                    if (start > 0) {
                                        mobileEt.setSelection(start - 1)
                                    }
                                } else {
                                    mobileEt.text?.delete(start, end)
                                    mobileEt.setSelection(start)
                                }
                            } else {
                                mobileEt.text = editable?.insert(start, value)
                                mobileEt.setSelection(start + 1)
                            }
                        } catch (e: IndexOutOfBoundsException) {
                            Timber.w(e)
                        }
                    } else if (countryCodeEt.isFocused) {
                        val editable = countryCodeEt.text
                        val start = countryCodeEt.selectionStart
                        val end = countryCodeEt.selectionEnd
                        try {
                            if (position == 11) {
                                if (editable.isNullOrEmpty() || editable.toString() == "+") return

                                if (start == end) {
                                    if (start <= 1) {
                                        countryCodeEt.text?.delete(1, end)
                                    } else {
                                        countryCodeEt.text?.delete(start - 1, end)
                                    }
                                    if (start > 1) {
                                        countryCodeEt.setSelection(start - 1)
                                    }
                                } else {
                                    countryCodeEt.text?.delete(start, end)
                                    countryCodeEt.setSelection(start)
                                }
                            } else {
                                countryCodeEt.text = editable?.insert(start, value)
                                if (start == 4) {
                                    mobileEt.requestFocus()
                                    mobileEt.setSelection(mobileEt.text?.length ?: 0)
                                } else {
                                    countryCodeEt.setSelection(start + 1)
                                }
                            }
                        } catch (e: IndexOutOfBoundsException) {
                            Timber.w(e)
                        }
                    }
                }
            }

            override fun onLongClick(
                position: Int,
                value: String,
            ) {
                context?.clickVibrate()
                if (viewDestroyed()) {
                    return
                }
                binding.apply {
                    if (mobileEt.isFocused) {
                        val editable = mobileEt.text
                        if (position == 11) {
                            if (editable.isNullOrEmpty()) {
                                countryCodeEt.requestFocus()
                                countryCodeEt.setSelection(countryCodeEt.text?.length ?: 0)
                                return
                            }

                            mobileEt.text?.clear()
                        } else {
                            val start = mobileEt.selectionStart
                            mobileEt.text = editable?.insert(start, value)
                            mobileEt.setSelection(start + 1)
                        }
                    } else if (countryCodeEt.isFocused) {
                        val editable = countryCodeEt.text
                        if (position == 11) {
                            if (editable.isNullOrEmpty() || editable.toString() == "+") return

                            countryCodeEt.text?.clear()
                        } else {
                            val start = countryCodeEt.selectionStart
                            countryCodeEt.text = editable?.insert(start, value)
                            if (start == 4) {
                                mobileEt.requestFocus()
                                mobileEt.setSelection(mobileEt.text?.length ?: 0)
                            } else {
                                countryCodeEt.setSelection(start + 1)
                            }
                        }
                    }
                }
            }
        }

    private val countryCodeWatcher: TextWatcher =
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

            override fun afterTextChanged(s: Editable?) {
                val et = binding.countryCodeEt
                if (!s.toString().startsWith("+")) {
                    et.setText("+")
                    Selection.setSelection(et.text, et.text?.length ?: 0)
                }
                val dialCode = et.text.toString()
                val country =
                    if (dialCode == xinDialCode) {
                        mixinCountry
                    } else {
                        countryPicker.getCountryByDialCode(dialCode)
                    }
                if (mCountry?.dialCode == country?.dialCode) {
                    handleEditView()
                    return
                }
                mCountry = country
                binding.apply {
                    if (country == null) {
                        countryIconIv.setImageResource(R.drawable.ic_arrow_down_info)
                    } else {
                        countryIconIv.setImageResource(country.flag)
                    }
                }
                updateMobileOrAnonymous(dialCode)
                handleEditView()
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

            override fun afterTextChanged(s: Editable?) {
                handleEditView()
                handleTextChange(s)
            }
        }

    private fun setupFocusListeners() {
        binding.countryCodeEt.setOnFocusChangeListener { _, hasFocus ->
            handleFocusChange(hasFocus)
        }

        binding.mobileEt.setOnFocusChangeListener { _, hasFocus ->
            handleFocusChange(hasFocus)
        }
    }

    private fun handleFocusChange(hasFocus: Boolean) {
        if (hasFocus) {
            binding.orLl.isVisible = false
            binding.mnemonicPhrase.isVisible = false
            binding.keyboard.animate().translationY(0f).start()
        } else {
            if (binding.mobileEt.text.isNullOrEmpty()) {
                binding.orLl.isVisible = from == FROM_LANDING
                binding.mnemonicPhrase.isVisible = from == FROM_LANDING
            }
            binding.keyboard.animate().translationY(300.dp.toFloat()).start()
        }
    }

    private fun handleTextChange(s: Editable?) {
        if (s.isNullOrEmpty() && !binding.mobileEt.hasFocus() && !binding.countryCodeEt.hasFocus()) {
            binding.orLl.isVisible = from == FROM_LANDING
            binding.mnemonicPhrase.isVisible = from == FROM_LANDING
        } else {
            binding.orLl.isVisible = false
            binding.mnemonicPhrase.isVisible = false
        }
    }
}

package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.AUTOFILL_HINT_PHONE
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.viewModels
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.mukesh.countrypicker.Country
import com.mukesh.countrypicker.CountryPicker
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.databinding.FragmentMobileBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.tapVibrate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.CaptchaView
import one.mixin.android.widget.Keyboard
import timber.log.Timber
import java.lang.IndexOutOfBoundsException

@AndroidEntryPoint
class MobileFragment : BaseFragment(R.layout.fragment_mobile) {

    companion object {
        const val TAG: String = "MobileFragment"
        const val ARGS_PHONE_NUM = "args_phone_num"

        fun newInstance(pin: String? = null): MobileFragment = MobileFragment().apply {
            val b = Bundle().apply {
                if (pin != null) {
                    putString(ARGS_PIN, pin)
                }
            }
            arguments = b
        }
    }

    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentMobileBinding::bind)

    private lateinit var countryPicker: CountryPicker
    private lateinit var mCountry: Country
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private var phoneNumber: Phonenumber.PhoneNumber? = null

    private var pin: String? = null

    private var captchaView: CaptchaView? = null

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            pin = requireArguments().getString(ARGS_PIN)
            if (pin != null) {
                mobileTitleTv.setText(R.string.landing_enter_new_mobile_number)
            }
            backIv.setOnClickListener { activity?.onBackPressed() }
            countryIconIv.setOnClickListener { showCountry() }
            countryCodeTv.setOnClickListener { showCountry() }
            mobileFab.setOnClickListener { showDialog() }
            mobileEt.showSoftInputOnFocus = false
            mobileEt.addTextChangedListener(mWatcher)
            mobileEt.requestFocus()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mobileEt.setAutofillHints(AUTOFILL_HINT_PHONE)
            }
            mobileCover.isClickable = true

            countryPicker = CountryPicker.newInstance()
            countryPicker.setListener { _: String, code: String, dialCode: String, flagResId: Int ->
                Unit
                mCountry = Country()
                mCountry.code = code
                mCountry.dialCode = dialCode
                mCountry.flag = flagResId
                countryIconIv.setImageResource(flagResId)
                countryCodeTv.text = dialCode
                handleEditView(mobileEt.text.toString())
                activity?.supportFragmentManager?.popBackStackImmediate()
                countryIconIv.hideKeyboard()
            }
            getUserCountryInfo()

            keyboard.setKeyboardKeys(KEYS)
            keyboard.setOnClickKeyboardListener(mKeyboardListener)
            keyboard.animate().translationY(0f).start()
        }
    }

    override fun onBackPressed(): Boolean {
        if (captchaView?.isVisible() == true) {
            hideLoading()
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
                    R.string.landing_invitation_dialog_content,
                    mCountry.dialCode + " " + binding.mobileEt.text.toString()
                )
            )
            .setNegativeButton(R.string.change) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                requestSend()
                dialog.dismiss()
            }
            .show()
    }

    private fun requestSend(captchaResponse: Pair<CaptchaView.CaptchaType, String>? = null) {
        if (viewDestroyed()) return

        binding.mobileFab.show()
        binding.mobileCover.visibility = VISIBLE
        val phoneNum = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
        val verificationRequest = VerificationRequest(
            phoneNum,
            if (pin == null) VerificationPurpose.SESSION.name else VerificationPurpose.PHONE.name
        )
        if (captchaResponse != null) {
            if (captchaResponse.first.isG()) {
                verificationRequest.gRecaptchaResponse = captchaResponse.second
            } else {
                verificationRequest.hCaptchaResponse = captchaResponse.second
            }
        }
        mobileViewModel.loginVerification(verificationRequest)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<VerificationResponse> ->
                    if (!r.isSuccess) {
                        if (r.errorCode == NEED_CAPTCHA) {
                            initAndLoadCaptcha()
                        } else {
                            hideLoading()
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        }
                        return@subscribe
                    }
                    hideLoading()
                    val verificationResponse = r.data as VerificationResponse
                    activity?.addFragment(
                        this@MobileFragment,
                        VerificationFragment.newInstance(
                            verificationResponse.id,
                            phoneNum,
                            pin,
                            verificationResponse.hasEmergencyContact
                        ),
                        VerificationFragment.TAG
                    )
                },
                { t: Throwable ->
                    hideLoading()
                    ErrorHandler.handleError(t)
                }
            )
    }

    private fun initAndLoadCaptcha() {
        if (captchaView == null) {
            captchaView = CaptchaView(
                requireContext(),
                object : CaptchaView.Callback {
                    override fun onStop() {
                        binding.mobileFab.hide()
                        binding.mobileCover.visibility = GONE
                    }

                    override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                        requestSend(value)
                    }
                }
            )
            (view as ViewGroup).addView(captchaView?.webView, MATCH_PARENT, MATCH_PARENT)
        }
        captchaView?.loadCaptcha(CaptchaView.CaptchaType.GCaptcha)
    }

    private fun hideLoading() {
        binding.mobileFab.hide()
        binding.mobileCover.visibility = GONE
        captchaView?.hide()
    }

    private fun handleEditView(str: String) {
        binding.apply {
            mobileEt.setSelection(mobileEt.text.toString().length)
            if (str.isNotEmpty() && isValidNumber(mCountry.dialCode + str)) {
                mobileFab.visibility = VISIBLE
            } else {
                mobileFab.visibility = INVISIBLE
            }
        }
    }

    private fun getUserCountryInfo() {
        mCountry = countryPicker.getUserCountryInfo(context)
        binding.countryIconIv.setImageResource(mCountry.flag)
        binding.countryCodeTv.text = mCountry.dialCode
        countryPicker.setLocationCountry(mCountry)
    }

    private fun isValidNumber(number: String): Boolean {
        val phone = Phone(number)
        return try {
            phoneNumber = phoneUtil.parse(phone.phone, mCountry.code)
            var isValid = phoneUtil.isValidNumber(phoneNumber)

            // workaround for old registered Ivory Coast user
            // https://issuetracker.google.com/issues/190630271
            if (!isValid && mCountry.code == "CI") {
                isValid = addPrefixAndTry(phone.phone)
            }

            isValid
        } catch (e: NumberParseException) {
            false
        }
    }

    private val prefixList = listOf("07", "05", "01", "27", "25", "21")

    private fun addPrefixAndTry(phoneNumber: String): Boolean {
        val num = phoneNumber.removePrefix(mCountry.dialCode)
        prefixList.forEach { p ->
            val phone = phoneUtil.parse("${mCountry.dialCode}$p$num", mCountry.code)
            if (phoneUtil.isValidNumber(phone)) {
                return true
            }
        }
        return false
    }

    private fun showCountry() {
        activity?.supportFragmentManager?.inTransaction {
            setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                .add(R.id.container, countryPicker).addToBackStack(null)
        }
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tapVibrate()
            if (viewDestroyed()) {
                return
            }
            binding.apply {
                val editable = mobileEt.text
                val start = mobileEt.selectionStart
                val end = mobileEt.selectionEnd
                try {
                    if (position == 11) {
                        if (editable.isNullOrEmpty()) return

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
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.tapVibrate()
            if (viewDestroyed()) {
                return
            }
            binding.apply {
                val editable = mobileEt.text
                if (position == 11) {
                    if (editable.isNullOrEmpty()) return

                    mobileEt.text?.clear()
                } else {
                    val start = mobileEt.selectionStart
                    mobileEt.text = editable?.insert(start, value)
                    mobileEt.setSelection(start + 1)
                }
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            handleEditView(s.toString())
        }
    }

    class Phone(var phone: String)
}

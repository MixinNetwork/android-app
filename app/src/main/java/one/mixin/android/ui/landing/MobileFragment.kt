package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.View
import android.view.View.AUTOFILL_HINT_PHONE
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
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
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_CAPTCHA
import one.mixin.android.util.isValidNumber
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.CaptchaView
import one.mixin.android.widget.Keyboard
import timber.log.Timber

@AndroidEntryPoint
class MobileFragment : BaseFragment(R.layout.fragment_mobile) {

    companion object {
        const val TAG: String = "MobileFragment"
        const val ARGS_PHONE_NUM = "args_phone_num"
        const val ARGS_FROM = "args_from"
        const val FROM_LANDING = 0
        const val FROM_CHANGE_PHONE_ACCOUNT = 1
        const val FROM_DELETE_ACCOUNT = 2

        fun newInstance(pin: String? = null, from: Int = FROM_LANDING): MobileFragment =
            MobileFragment().apply {
                val b = Bundle().apply {
                    if (pin != null) {
                        putString(ARGS_PIN, pin)
                    }
                    putInt(ARGS_FROM, from)
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

    private var pin: String? = null
    private val from: Int by lazy {
        requireArguments().getInt(ARGS_FROM, FROM_LANDING)
    }

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
            countryCodeEt.addTextChangedListener(countryCodeWatcher)
            countryCodeEt.showSoftInputOnFocus = false
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
                    mCountry?.dialCode + " " + binding.mobileEt.text.toString()
                )
            )
            .setNegativeButton(R.string.Change) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
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
            when (from) {
                FROM_DELETE_ACCOUNT -> {
                    VerificationPurpose.DEACTIVATED.name
                }
                FROM_CHANGE_PHONE_ACCOUNT -> {
                    VerificationPurpose.PHONE.name
                }
                else -> {
                    VerificationPurpose.SESSION.name
                }
            }
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
                    if (!r.data?.deactivatedAt.isNullOrBlank() && from == FROM_LANDING) {
                        LandingDeleteAccountFragment.newInstance(r.data?.deactivatedAt)
                            .setContinueCallback {
                                activity?.addFragment(
                                    this@MobileFragment,
                                    VerificationFragment.newInstance(
                                        verificationResponse.id,
                                        phoneNum,
                                        pin,
                                        verificationResponse.hasEmergencyContact,
                                        from
                                    ),
                                    VerificationFragment.TAG
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
                                from
                            ),
                            VerificationFragment.TAG
                        )
                    }
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

    private fun handleEditView() {
        binding.apply {
            val country = mCountry
            if (country == null) {
                mobileFab.isVisible = false
                return
            }

            val mobileText = mobileEt.text.toString()
            mobileEt.setSelection(mobileText.length)
            val validResult = isValidNumber(phoneUtil, country.dialCode + mobileText, country.code, country.dialCode)
            phoneNumber = validResult.second
            mobileFab.isVisible = (countryCodeEt.text?.length ?: 0) > 1 && mobileText.isNotEmpty() && validResult.first
        }
    }

    private fun getUserCountryInfo() {
        countryPicker.getUserCountryInfo(context).apply {
            mCountry = this
            binding.countryIconIv.setImageResource(flag)
            binding.countryCodeEt.setText(dialCode)
            countryPicker.setLocationCountry(this)
        }
    }

    private fun showCountry() {
        activity?.supportFragmentManager?.inTransaction {
            setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                .add(R.id.container, countryPicker).addToBackStack(null)
        }
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener =
        object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(position: Int, value: String) {
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

            override fun onLongClick(position: Int, value: String) {
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

    private val countryCodeWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val et = binding.countryCodeEt
            if (!s.toString().startsWith("+")) {
                et.setText("+")
                Selection.setSelection(et.text, et.text?.length ?: 0)
            }
            val country = countryPicker.getCountryByDialCode(et.text.toString())
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
            handleEditView()
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            handleEditView()
        }
    }
}

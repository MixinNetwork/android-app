package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.mukesh.countrypicker.Country
import com.mukesh.countrypicker.CountryPicker
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_mobile.*
import okio.Okio
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.API.DOMAIN
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.cancelRunOnUIThread
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.runOnUIThread
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.NEED_RECAPTCHA
import one.mixin.android.widget.Keyboard
import org.jetbrains.anko.support.v4.toast
import java.nio.charset.Charset
import javax.inject.Inject

class MobileFragment : BaseFragment() {

    companion object {
        const val TAG: String = "MobileFragment"
        const val ARGS_PHONE_NUM = "args_phone_num"

        private const val WEB_VIEW_TIME_OUT = 10000L

        fun newInstance(pin: String? = null): MobileFragment = MobileFragment().apply {
            val b = Bundle().apply {
                if (pin != null) {
                    putString(ARGS_PIN, pin)
                }
            }
            arguments = b
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val mobileViewModel: MobileViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(MobileViewModel::class.java)
    }
    private lateinit var countryPicker: CountryPicker
    private lateinit var mCountry: Country
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private var phoneNumber: Phonenumber.PhoneNumber? = null

    private var pin: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_mobile, container, false)

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pin = arguments!!.getString(ARGS_PIN)
        if (pin != null) {
            mobile_title_tv.setText(R.string.landing_enter_new_mobile_number)
        }
        back_iv.setOnClickListener { activity?.onBackPressed() }
        country_icon_iv.setOnClickListener { showCountry() }
        country_code_tv.setOnClickListener { showCountry() }
        mobile_fab.setOnClickListener { showDialog() }
        mobile_et.showSoftInputOnFocus = false
        mobile_et.addTextChangedListener(mWatcher)
        mobile_et.requestFocus()
        mobile_cover.isClickable = true

        countryPicker = CountryPicker.newInstance()
        countryPicker.setListener { _: String, code: String, dialCode: String, flagResId: Int ->
            Unit
            mCountry = Country()
            mCountry.code = code
            mCountry.dialCode = dialCode
            mCountry.flag = flagResId
            country_icon_iv.setImageResource(flagResId)
            country_code_tv.text = dialCode
            handleEditView(mobile_et.text.toString())
            activity?.supportFragmentManager?.popBackStackImmediate()
            country_icon_iv.hideKeyboard()
        }
        getUserCountryInfo()

        keyboard.setKeyboardKeys(KEYS)
        keyboard.setOnClickKeyboardListener(mKeyboardListener)
        keyboard.animate().translationY(0f).start()

        webView.settings.apply {
            defaultTextEncodingName = "utf-8"
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(this, "MixinContext")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                context?.runOnUIThread(stopWebViewRunnable, WEB_VIEW_TIME_OUT)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("javascript:gReCaptchaExecute()", null)
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (pin == null) {
            activity?.supportFragmentManager?.popBackStackImmediate()
            return true
        }
        return false
    }

    private fun showDialog() {
        AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setMessage(getString(R.string.landing_invitation_dialog_content,
                mCountry.dialCode + " " + mobile_et.text.toString()))
            .setNegativeButton(R.string.change, { dialog, _ -> dialog.dismiss() })
            .setPositiveButton(R.string.confirm, { dialog, _ ->
                requestSend()
                dialog.dismiss()
            })
            .show()
    }

    private fun requestSend(gRecaptchaResponse: String? = null) {
        mobile_fab.show()
        mobile_cover.visibility = VISIBLE
        val phoneNum = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
        val verificationRequest = VerificationRequest(
            phoneNum,
            null,
            if (pin == null) VerificationPurpose.SESSION.name else VerificationPurpose.PHONE.name,
            gRecaptchaResponse)
        mobileViewModel.verification(verificationRequest)
            .autoDisposable(scopeProvider).subscribe({ r: MixinResponse<VerificationResponse> ->
                mobile_fab?.hide()
                mobile_cover?.visibility = GONE
                if (!r.isSuccess) {
                    if (r.errorCode == NEED_RECAPTCHA) {
                        val input = requireContext().assets.open("recaptcha.html")
                        var html = Okio.buffer(Okio.source(input)).readByteString().string(Charset.forName("utf-8"))
                        html = html.replace("#apiKey", BuildConfig.RECAPTCHA_KEY)
                        webView.loadDataWithBaseURL(DOMAIN, html, "text/html", "UTF-8", null)
                    }
                    ErrorHandler.handleMixinError(r.errorCode)
                    return@subscribe
                }
                activity?.addFragment(this@MobileFragment,
                    VerificationFragment.newInstance(r.data!!.id, phoneNum, pin, gRecaptchaResponse), VerificationFragment.TAG)
            }, { t: Throwable ->
                mobile_fab?.hide()
                mobile_cover?.visibility = GONE
                webView.visibility = GONE
                ErrorHandler.handleError(t)
            })
    }

    private fun handleEditView(str: String) {
        mobile_et.setSelection(mobile_et.text.toString().length)
        if (str.isNotEmpty() && isValidNumber(mCountry.dialCode + str)) {
            mobile_fab.visibility = VISIBLE
        } else {
            mobile_fab.visibility = INVISIBLE
        }
    }

    private fun getUserCountryInfo() {
        mCountry = countryPicker.getUserCountryInfo(context)
        country_icon_iv.setImageResource(mCountry.flag)
        country_code_tv.text = mCountry.dialCode
        countryPicker.setLocationCountry(mCountry)
    }

    private fun isValidNumber(number: String): Boolean {
        val phone = Phone(number)
        return try {
            phoneNumber = phoneUtil.parse(phone.phone, mCountry.code)
            phoneUtil.isValidNumber(phoneNumber)
        } catch (e: NumberParseException) {
            false
        }
    }

    private fun showCountry() {
        activity?.supportFragmentManager?.inTransaction {
            setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
                .hide(this@MobileFragment).add(R.id.container, countryPicker).addToBackStack(null)
        }
    }

    private val stopWebViewRunnable = Runnable {
        mobile_fab?.hide()
        mobile_cover?.visibility = GONE
        webView.stopLoading()
        webView.visibility = GONE
        toast(R.string.error_recaptcha_timeout)
    }

    @JavascriptInterface
    fun postMessage(value: String) {
        if (value == "challenge_change") {
            context?.cancelRunOnUIThread(stopWebViewRunnable)
            webView.post {
                webView.visibility = VISIBLE
            }
        }
    }

    @JavascriptInterface
    fun postToken(value: String) {
        context?.cancelRunOnUIThread(stopWebViewRunnable)
        mobile_fab.post {
            requestSend(value)
        }
    }

    interface Callback {
        fun onMessage(value: String)
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            val editable = mobile_et.text
            if (position == 11 && editable.isNotEmpty()) {
                mobile_et.text = editable.subSequence(0, editable.length - 1) as Editable?
            } else {
                mobile_et.text = editable.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            val editable = mobile_et.text
            if (position == 11 && editable.isNotEmpty()) {
                mobile_et.text.clear()
            } else {
                mobile_et.text = editable.append(value)
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
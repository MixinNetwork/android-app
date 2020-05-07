package one.mixin.android.ui.common

import android.graphics.Point
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import java.security.KeyPair
import kotlinx.android.synthetic.main.fragment_verification.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_LAST_USER_ID
import one.mixin.android.Constants.KEYS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.crypto.getPrivateKeyPem
import one.mixin.android.crypto.rsaDecrypt
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.RestoreActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.VerificationCodeView
import org.jetbrains.anko.windowManager

abstract class PinCodeFragment<VH : ViewModel> : FabLoadingFragment<VH>() {
    companion object {
        const val PREF_LOGIN_FROM = "pref_login_from"

        const val FROM_LOGIN = 0
        const val FROM_EMERGENCY = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_iv.setOnClickListener { activity?.onBackPressed() }
        pin_verification_view.setOnCodeEnteredListener(mPinVerificationListener)
        verification_keyboard.setKeyboardKeys(KEYS)
        verification_keyboard.setOnClickKeyboardListener(mKeyboardListener)
        verification_cover.isClickable = true
        verification_next_fab.setOnClickListener { clickNextFab() }
    }

    protected fun handleFailure(r: MixinResponse<*>) {
        pin_verification_view.error()
        pin_verification_tip_tv.visibility = View.VISIBLE
        pin_verification_tip_tv.text = getString(R.string.landing_validation_error)
        if (r.errorCode == ErrorHandler.PHONE_VERIFICATION_CODE_INVALID ||
            r.errorCode == ErrorHandler.PHONE_VERIFICATION_CODE_EXPIRED
        ) {
            verification_next_fab.visibility = View.INVISIBLE
        }
        ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
    }

    private suspend fun saveQrCode(account: Account) = withContext(Dispatchers.IO) {
        val p = Point()
        val ctx = MixinApplication.appContext
        ctx.windowManager.defaultDisplay?.getSize(p)
        val size = minOf(p.x, p.y)
        val b = account.code_url.generateQRCode(size)
        b?.saveQRCode(ctx, account.userId)
    }

    protected suspend fun handleAccount(
        response: MixinResponse<Account>,
        sessionKey: KeyPair
    ) = withContext(Dispatchers.Main) {
        if (!response.isSuccess) {
            hideLoading()
            handleFailure(response)
            return@withContext
        }

        val account = response.data as Account
        if (account.code_id.isNotEmpty()) {
            saveQrCode(account)
        }

        val lastUserId = requireContext().defaultSharedPreferences.getString(PREF_LAST_USER_ID, null)
        val sameUser = lastUserId == null || lastUserId == account.userId
        if (!sameUser) {
            showLoading()

            withContext(Dispatchers.IO) {
                MixinDatabase.getDatabase(requireContext()).clearAllTables()
            }
        }
        Session.storeAccount(account)
        Session.storeToken(sessionKey.getPrivateKeyPem())
        val key = rsaDecrypt(sessionKey.private, account.session_id, account.pin_token)
        Session.storePinToken(key)
        verification_keyboard.animate().translationY(300f).start()
        MixinApplication.get().onlining.set(true)

        hideLoading()

        when {
            sameUser -> {
                insertUser(account.toUser())
                InitializeActivity.showLoading(requireContext())
            }
            account.full_name.isNullOrBlank() -> {
                insertUser(account.toUser())
                InitializeActivity.showSetupName(requireContext())
            }
            else -> {
                RestoreActivity.show(requireContext())
            }
        }
        activity?.finish()
    }

    abstract fun clickNextFab()

    abstract fun insertUser(u: User)

    private val mKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin_verification_view?.delete()
            } else {
                pin_verification_view?.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin_verification_view?.clear()
            } else {
                pin_verification_view?.append(value)
            }
        }
    }

    private val mPinVerificationListener = object : VerificationCodeView.OnCodeEnteredListener {
        override fun onCodeEntered(code: String) {
            pin_verification_tip_tv.visibility = View.INVISIBLE
            if (code.isEmpty() || code.length != pin_verification_view.count) {
                if (isAdded) {
                    hideLoading()
                }
                return
            }
            clickNextFab()
        }
    }
}

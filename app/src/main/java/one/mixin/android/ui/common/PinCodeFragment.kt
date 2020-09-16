package one.mixin.android.ui.common

import android.graphics.Point
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_verification.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import one.mixin.android.Constants.KEYS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clear
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.RestoreActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.util.database.clearDatabase
import one.mixin.android.util.database.getLastUserId
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.VerificationCodeView
import org.jetbrains.anko.windowManager
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519.BEST
import java.security.KeyPair

abstract class PinCodeFragment : FabLoadingFragment() {
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
        val b = account.codeUrl.generateQRCode(size)
        b?.saveQRCode(ctx, account.userId)
    }

    protected suspend fun handleAccount(
        response: MixinResponse<Account>,
        sessionKey: KeyPair,
        action: () -> Unit
    ) = withContext(Dispatchers.Main) {
        if (!response.isSuccess) {
            hideLoading()
            handleFailure(response)
            return@withContext
        }

        val account = response.data as Account
        if (account.codeId.isNotEmpty()) {
            saveQrCode(account)
        }

        val lastUserId = getLastUserId(requireContext())
        val sameUser = lastUserId != null && lastUserId == account.userId
        if (!sameUser) {
            showLoading()
            clearDatabase(requireContext())
            defaultSharedPreferences.clear()
        }
        val privateKey = sessionKey.private as EdDSAPrivateKey
        val key = try {
            Curve25519.getInstance(BEST).calculateAgreement(account.pinToken.decodeBase64(), privateKeyToCurve25519(privateKey.seed))
        } catch (t: Throwable) {
            reportException("Login calculateAgreement", t)
            return@withContext
        }
        Session.storeAccount(account)
        Session.storeEd25519PrivateKey(privateKey.seed.base64Encode())
        Session.storePinToken(key.base64Encode())

        verification_keyboard.animate().translationY(300f).start()
        MixinApplication.get().onlining.set(true)

        hideLoading()
        action.invoke()

        when {
            sameUser -> {
                insertUser(account.toUser())
                InitializeActivity.showLoading(requireContext())
            }
            account.fullName.isNullOrBlank() -> {
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

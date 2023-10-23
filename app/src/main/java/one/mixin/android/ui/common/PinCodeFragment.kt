package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.Constants.DataBase.SIGNAL_DB_NAME
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clear
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.moveTo
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.putString
import one.mixin.android.extension.tickVibrate
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.session.Session
import one.mixin.android.session.decryptPinToken
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.RestoreActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.VerificationCodeView
import java.io.File

abstract class PinCodeFragment(@LayoutRes contentLayoutId: Int) : FabLoadingFragment(contentLayoutId) {
    companion object {
        const val PREF_LOGIN_FROM = "pref_login_from"

        const val FROM_LOGIN = 0
        const val FROM_EMERGENCY = 1
    }

    protected val pinVerificationView: VerificationCodeView by lazy {
        _contentView.findViewById(R.id.pin_verification_view)
    }
    protected val pinVerificationTipTv: TextView by lazy {
        _contentView.findViewById(R.id.pin_verification_tip_tv)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backIv.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        pinVerificationView.setOnCodeEnteredListener(mPinVerificationListener)
        verificationKeyboard.tipTitleEnabled = false
        verificationKeyboard.initPinKeys(requireContext())
        verificationKeyboard.setOnClickKeyboardListener(mKeyboardListener)
        verificationCover.isClickable = true
        verificationNextFab.setOnClickListener { clickNextFab() }
    }

    protected fun handleFailure(r: MixinResponse<*>) {
        return handleFailure(requireNotNull(r.error))
    }

    protected fun handleFailure(error: ResponseError) {
        pinVerificationView.error()
        pinVerificationTipTv.visibility = View.VISIBLE
        pinVerificationTipTv.text = getString(R.string.The_code_is_incorrect)
        if (error.code == ErrorHandler.PHONE_VERIFICATION_CODE_INVALID ||
            error.code == ErrorHandler.PHONE_VERIFICATION_CODE_EXPIRED
        ) {
            verificationNextFab.visibility = View.INVISIBLE
        }
        ErrorHandler.handleMixinError(error.code, error.description)
    }

    protected suspend fun handleAccount(
        response: MixinResponse<Account>,
        sessionKey: EdKeyPair,
        action: () -> Unit,
    ) = withContext(Dispatchers.Main) {
        if (!response.isSuccess) {
            hideLoading()
            handleFailure(response)
            return@withContext
        }

        val account = response.data as Account

        showLoading()
        // Release the singleton and re-inject
        MixinDatabase.release()
        PendingDatabaseImp.release()
        FtsDatabase.release()
        SignalDatabase.release()
        defaultSharedPreferences.clear()

        val privateKey = sessionKey.privateKey
        val pinToken = decryptPinToken(account.pinToken.decodeBase64(), privateKey)
        Session.storeEd25519Seed(privateKey.base64Encode())
        Session.storePinToken(pinToken.base64Encode())
        Session.storeAccount(account) // After that, you can use the database.
        defaultSharedPreferences.putString(DEVICE_ID, requireContext().getStringDeviceId())

        verificationKeyboard.animate().translationY(300f).start()
        MixinApplication.get().isOnline.set(true)

        hideLoading()
        action.invoke()
        lifecycleScope.launch {
            // Move the initialization created Signal database to the account path
            withContext(Dispatchers.IO) {
                val context = requireContext()
                val identityNumber = Session.getAccount()?.identityNumber ?: return@withContext
                val dbDir = context.getDatabasePath(SIGNAL_DB_NAME).parentFile
                val toDir = File(dbDir, identityNumber)
                if (!toDir.exists()) {
                    toDir.mkdirs()
                }
                dbDir.listFiles().forEach { file ->
                    if (file.name.startsWith(SIGNAL_DB_NAME)) {
                        file.moveTo(File(toDir, file.name))
                    }
                }
                SignalDatabase.release()

            }
            when {
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
    }

    abstract fun clickNextFab()

    abstract fun insertUser(u: User)

    private val mKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tickVibrate()
            if (position == 11) {
                pinVerificationView.delete()
            } else {
                pinVerificationView.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.clickVibrate()
            if (position == 11) {
                pinVerificationView.clear()
            } else {
                pinVerificationView.append(value)
            }
        }
    }

    private val mPinVerificationListener = object : VerificationCodeView.OnCodeEnteredListener {
        override fun onCodeEntered(code: String) {
            pinVerificationTipTv.visibility = View.INVISIBLE
            if (code.isEmpty() || code.length != pinVerificationView.count) {
                if (isAdded) {
                    hideLoading()
                }
                return
            }
            clickNextFab()
        }
    }
}

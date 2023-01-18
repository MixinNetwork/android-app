@file:Suppress("DEPRECATION")

package one.mixin.android.ui.auth

import android.app.Activity
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import com.mattprecious.swirl.SwirlView
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityAppAuthBinding
import one.mixin.android.event.AppAuthEvent
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.url.UrlInterpreterActivity

class AppAuthActivity : BaseActivity() {
    companion object {
        fun show(activity: Activity) {
            Intent(activity, AppAuthActivity::class.java).apply {
                data = if (activity is UrlInterpreterActivity) activity.intent.data else null
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(this)
            }
        }
    }

    private lateinit var binding: ActivityAppAuthBinding

    private var fingerprintManager: FingerprintManagerCompat? = null
    private var biometricManager: BiometricManager? = null

    private var hasEnrolledFingerprints = false

    private var cancellationSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fingerprintManager = FingerprintManagerCompat.from(this).apply {
            this@AppAuthActivity.hasEnrolledFingerprints = hasEnrolledFingerprints()
        }
        if (!hasEnrolledFingerprints) {
            biometricManager = BiometricManager.from(this)
        }

        binding.swirl.setState(SwirlView.State.ON)
        binding.swirl.setOnClickListener {
            showPrompt()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        binding.swirl.setState(SwirlView.State.ON)
    }

    override fun onStart() {
        super.onStart()
        binding.swirl.postDelayed(showPromptRunnable, 100)
    }

    override fun onStop() {
        super.onStop()
        binding.swirl.removeCallbacks(resetSwirlRunnable)
        binding.swirl.removeCallbacks(showPromptRunnable)
        cancellationSignal?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        biometricManager = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    private fun showPrompt() {
        cancellationSignal?.cancel()
        cancellationSignal = CancellationSignal()
        if (hasEnrolledFingerprints) {
            fingerprintManager?.authenticate(null, 0, cancellationSignal, fingerprintCallback, null)
        } else {
            showAppAuthPrompt(
                this,
                getString(R.string.Confirm_fingerprint),
                getString(R.string.Cancel),
                biometricCallback,
            )
        }
    }

    private fun refreshSwirl(errString: CharSequence, show: Boolean) {
        showError(errString)
        binding.swirl.postDelayed(resetSwirlRunnable, 1000)
        if (show) {
            binding.swirl.postDelayed(showPromptRunnable, 1000)
        }
    }

    private fun showError(errString: CharSequence) {
        binding.info.text = errString
        binding.info.setTextColor(getColor(R.color.colorRed))
        binding.swirl.setState(SwirlView.State.ERROR)
    }

    private fun finishAndCheckNeed2GoUrlInterpreter() {
        val data = intent.data
        if (data != null) {
            UrlInterpreterActivity.show(this, data)
        }
        finish()

        RxBus.publish(AppAuthEvent())
    }

    private val resetSwirlRunnable = Runnable {
        binding.info.text = getString(R.string.Confirm_fingerprint)
        binding.info.setTextColor(colorFromAttribute(R.attr.text_minor))
        binding.swirl.setState(SwirlView.State.ON)
    }

    private val showPromptRunnable = Runnable {
        showPrompt()
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            when (errorCode) {
                BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                    moveTaskToBack(true)
                }
                BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                    showError(errString)
                }
                BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                    defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, -1)
                    defaultSharedPreferences.putLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, 0)
                    finishAndCheckNeed2GoUrlInterpreter()
                }
                else -> {
                    refreshSwirl(errString, true)
                }
            }
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.Not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            finishAndCheckNeed2GoUrlInterpreter()
        }
    }

    private val fingerprintCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            when (errorCode) {
                FingerprintManager.FINGERPRINT_ERROR_CANCELED, FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED, 1010 -> {
                    moveTaskToBack(true)
                }
                FingerprintManager.FINGERPRINT_ERROR_LOCKOUT, FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT -> {
                    showError(errString)
                }
                FingerprintManager.FINGERPRINT_ERROR_NO_FINGERPRINTS -> {
                    defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, -1)
                    defaultSharedPreferences.putLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, 0)
                    finishAndCheckNeed2GoUrlInterpreter()
                }
                else -> {
                    refreshSwirl(errString, true)
                }
            }
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.Not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            finishAndCheckNeed2GoUrlInterpreter()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            refreshSwirl(helpString.toString(), true)
        }
    }
}

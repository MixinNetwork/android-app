package one.mixin.android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricPrompt
import com.mattprecious.swirl.SwirlView
import one.mixin.android.R
import one.mixin.android.databinding.ActivityAppAuthBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.ui.common.BaseActivity

class AppAuthActivity : BaseActivity() {
    companion object {
        fun show(context: Context) {
            Intent(context, AppAuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }

    private lateinit var binding: ActivityAppAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.swirl.setState(SwirlView.State.ON)
    }

    override fun onStart() {
        super.onStart()
        showPrompt()
    }

    override fun onStop() {
        super.onStop()
        binding.swirl.removeCallbacks(resetSwirlRunnable)
        binding.swirl.removeCallbacks(showPromptRunnable)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        pressHome()
    }

    private fun showPrompt() {
        showAppAuthPrompt(
            this,
            getString(R.string.fingerprint_confirm),
            getString(R.string.cancel),
            authCallback
        )
    }

    private fun pressHome() {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            startActivity(this)
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

    private val resetSwirlRunnable = Runnable {
        binding.info.text = getString(R.string.fingerprint_confirm)
        binding.info.setTextColor(colorFromAttribute(R.attr.text_minor))
        binding.swirl.setState(SwirlView.State.ON)
    }

    private val showPromptRunnable = Runnable {
        showPrompt()
    }

    private val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
            ) {
                pressHome()
            } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
            ) {
                showError(errString)
            } else {
                refreshSwirl(errString, true)
            }
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            finish()
        }
    }
}

package one.mixin.android.ui.auth

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.addCallback
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import com.mattprecious.swirl.SwirlView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityAppAuthBinding
import one.mixin.android.event.AppAuthEvent
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.putInt
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.UrlInterpreterActivity

@AndroidEntryPoint
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

    private var biometricPrompt: BiometricPrompt? = null

    private var clearWhenPinSuccess = false

    private var promptGeneration = 0
    private var promptActive = false
    private var promptScheduled = false
    private var retryPromptOnResume = false
    private var authenticated = false
    private var pendingPinUnlock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateLayout()
        clearWhenPinSuccess = savedInstanceState?.getBoolean("clear_when_pin_success") ?: false
        pendingPinUnlock = savedInstanceState?.getBoolean("pending_pin_unlock") ?: false
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }
        (supportFragmentManager.findFragmentByTag(VerifyBottomSheetDialogFragment.TAG) as? VerifyBottomSheetDialogFragment)
            ?.let(::bindPinVerification)

        binding.swirl.setState(SwirlView.State.ON)
        binding.swirl.setOnClickListener {
            showPrompt()
        }
        binding.pinTv.setOnClickListener {
            if (supportFragmentManager.isStateSaved) return@setOnClickListener
            cancelPrompt()
            val dialog = supportFragmentManager.findFragmentByTag(VerifyBottomSheetDialogFragment.TAG) as? VerifyBottomSheetDialogFragment
                ?: VerifyBottomSheetDialogFragment.newInstance(disableBiometric = true)
            bindPinVerification(dialog)
            if (!dialog.isAdded) {
                dialog.showNow(supportFragmentManager, VerifyBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) setIntent(intent)
        binding.swirl.setState(SwirlView.State.ON)
    }

    override fun onStart() {
        super.onStart()
        if (pendingPinUnlock && !authenticated && !isFinishing) {
            pendingPinUnlock = false
            applyPinSuccessUnlock()
            return
        }
        retryPromptOnResume = false
        schedulePrompt()
    }

    override fun onResume() {
        super.onResume()
        if (retryPromptOnResume) {
            retryPromptOnResume = false
            schedulePrompt()
        }
    }

    override fun onPause() {
        if (promptScheduled) {
            promptScheduled = false
            binding.swirl.removeCallbacks(showPromptRunnable)
            retryPromptOnResume = true
        }
        super.onPause()
    }

    override fun onStop() {
        cancelPrompt()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("clear_when_pin_success", clearWhenPinSuccess)
        outState.putBoolean("pending_pin_unlock", pendingPinUnlock)
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayout()
    }

    private fun schedulePrompt(delayMs: Long = 100) {
        if (promptActive || authenticated || isFinishing) return
        if (supportFragmentManager.findFragmentByTag(VerifyBottomSheetDialogFragment.TAG) != null) return
        binding.swirl.removeCallbacks(showPromptRunnable)
        promptScheduled = true
        binding.swirl.postDelayed(showPromptRunnable, delayMs)
    }

    private fun showPrompt() {
        promptScheduled = false
        if (promptActive || authenticated || isFinishing) return
        if (supportFragmentManager.findFragmentByTag(VerifyBottomSheetDialogFragment.TAG) != null) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                retryPromptOnResume = true
            }
            return
        }
        retryPromptOnResume = false
        binding.swirl.removeCallbacks(showPromptRunnable)
        val generation = ++promptGeneration
        promptActive = true
        biometricPrompt = showAppAuthPrompt(
            this,
            getString(R.string.Confirm_fingerprint),
            getString(R.string.Cancel),
            biometricCallback(generation),
            getString(R.string.Unlock_with_fingerprint),
        )
    }

    private fun cancelPrompt() {
        ++promptGeneration
        promptActive = false
        promptScheduled = false
        retryPromptOnResume = false
        binding.swirl.removeCallbacks(resetSwirlRunnable)
        binding.swirl.removeCallbacks(showPromptRunnable)
        biometricPrompt?.cancelAuthentication()
        biometricPrompt = null
    }

    private fun isCurrentPrompt(generation: Int): Boolean =
        generation == promptGeneration && promptActive && !isFinishing && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun applyPinSuccessUnlock() {
        if (clearWhenPinSuccess) {
            defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, -1)
        }
        finishAndCheckNeed2GoUrlInterpreter()
    }

    private fun bindPinVerification(dialog: VerifyBottomSheetDialogFragment) {
        dialog.setOnPinSuccess {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                applyPinSuccessUnlock()
            } else {
                pendingPinUnlock = true
            }
        }.setOnResult { success ->
            if (!success && !authenticated && !pendingPinUnlock) {
                schedulePrompt()
            }
        }
    }

    private fun refreshSwirl(errString: CharSequence) {
        showError(errString)
        binding.swirl.removeCallbacks(resetSwirlRunnable)
        binding.swirl.postDelayed(resetSwirlRunnable, 1000)
    }

    private fun showError(errString: CharSequence) {
        binding.info.text = errString
        binding.info.setTextColor(getColor(R.color.colorRed))
        binding.swirl.setState(SwirlView.State.ERROR)
        binding.pinTv.isVisible = true
    }

    private fun finishAndCheckNeed2GoUrlInterpreter() {
        if (authenticated || isFinishing) return
        authenticated = true
        cancelPrompt()
        val data = intent.data
        val wasTaskRoot = isTaskRoot
        MixinApplication.get().appAuth.onAuthenticated()
        if (data != null) {
            UrlInterpreterActivity.show(this, data)
        } else if (wasTaskRoot) {
            startActivity(MainActivity.getWakeUpIntent(this))
        }
        finish()
        RxBus.publish(AppAuthEvent())
    }

    private fun updateLayout() {
        binding.title.updateLayoutParams<RelativeLayout.LayoutParams> {
            if (isLandscape()) {
                removeRule(RelativeLayout.BELOW)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                setMargins(0, 0, 0, 32.dp)
            } else {
                removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.BELOW, binding.icon.id)
                setMargins(0, 12.dp, 0, 0)
            }
        }
    }

    private val resetSwirlRunnable =
        Runnable {
            binding.info.text = getString(R.string.Confirm_fingerprint)
            binding.info.setTextColor(colorFromAttribute(R.attr.text_assist))
            binding.swirl.setState(SwirlView.State.ON)
        }

    private val showPromptRunnable =
        Runnable {
            showPrompt()
        }

    private fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        promptActive = false
        when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                moveTaskToBack(true)
            }
            BiometricPrompt.ERROR_CANCELED -> {
                retryPromptOnResume = true
            }
            BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                clearWhenPinSuccess = true
                showError(errString)
            }
            BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> showError(errString)
            else -> {
                refreshSwirl(errString)
                schedulePrompt(1000)
            }
        }
    }

    private fun biometricCallback(generation: Int) =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (isCurrentPrompt(generation)) this@AppAuthActivity.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                if (isCurrentPrompt(generation)) refreshSwirl(getString(R.string.Not_recognized))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (isCurrentPrompt(generation)) finishAndCheckNeed2GoUrlInterpreter()
            }
        }
}

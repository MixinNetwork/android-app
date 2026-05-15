package one.mixin.android.ui.landing

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.crypto.PrivacyPreference
import one.mixin.android.databinding.FragmentSetupNameBinding
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.toUser
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SetupNameFragment : BaseFragment(R.layout.fragment_setup_name) {
    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentSetupNameBinding::bind)
    private var isSubmitting = false
    private var showRetry = false

    @Inject
    lateinit var jobManager: MixinJobManager

    companion object {
        fun newInstance() = SetupNameFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Timber.e("SetupNameFragment onViewCreated")
        AnalyticsTracker.trackSignUpFullName()
        MixinApplication.get().isOnline.set(true)
        binding.apply {
            updateContinueButton()

            debug.setOnLongClickListener {
                LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
                true
            }

            continueBtn.setOnClickListener {
                if (isSubmitting) return@setOnClickListener

                val name = nameEt.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    performNameUpdate(name)
                }
            }

            nameEt.addTextChangedListener(mWatcher)
            nameEt.setOnEditorActionListener { _, _, _ ->
                if (!isSubmitting && nameEt.text.isNotBlank()) {
                    continueBtn.performClick()
                }
                true
            }
            nameCover.isClickable = true

            nameEt.postDelayed({
                nameEt.requestFocus()
                nameEt.showKeyboard()
            }, 200)
        }
        setupSimpleKeyboardListener()
    }

    private fun performNameUpdate(name: String) {
        binding.apply {
            setSubmitting(true)

            val accountUpdateRequest = AccountUpdateRequest(name)
            mobileViewModel.update(accountUpdateRequest)
                .autoDispose(stopScope).subscribe(
                    { r: MixinResponse<Account> ->
                        if (!r.isSuccess) {
                            setSubmitting(false, canRetry = true)
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                            return@subscribe
                        }
                        setSubmitting(false)
                        r.data?.let { data ->
                            Session.storeAccount(data)
                            mobileViewModel.insertUser(data.toUser())
                        }

                        nameEt.hideKeyboard()
                        val context = requireContext()
                        if (!PrivacyPreference.getIsLoaded(requireContext(), false) ||
                            !PrivacyPreference.getIsSyncSession(context, false)
                        ) {
                            InitializeActivity.showLoading(context, false, source = InitializeActivity.SOURCE_SIGN_UP)
                        } else {
                            startActivity(Intent(context, MainActivity::class.java))
                        }
                        activity?.finish()
                    },
                    { t: Throwable ->
                        setSubmitting(false, canRetry = true)
                        ErrorHandler.handleError(t)
                        reportException("SetupNameFragment update", t)
                    },
                )
        }
    }

    private fun setupSimpleKeyboardListener() {
        val rootView = binding.root
        val continueBtn = binding.continueBtn
        val originalMargin = (continueBtn.layoutParams as RelativeLayout.LayoutParams).bottomMargin

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val keypadHeight = rootView.height - rect.bottom

            val layoutParams = continueBtn.layoutParams as RelativeLayout.LayoutParams
            layoutParams.bottomMargin = if (keypadHeight > 200) {
                originalMargin + keypadHeight + 16.dpToPx()
            } else {
                originalMargin
            }
            continueBtn.layoutParams = layoutParams
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setSubmitting(
        submitting: Boolean,
        canRetry: Boolean = false,
    ) {
        isSubmitting = submitting
        showRetry = canRetry
        binding.nameCover.visibility = if (submitting) VISIBLE else INVISIBLE
        updateContinueButton()
    }

    private fun updateContinueButton() {
        val hasName = binding.nameEt.text?.toString()?.trim()?.isNotBlank() == true
        binding.continueBtn.displayedChild = if (isSubmitting) 1 else 0
        binding.continueBtn.isEnabled = !isSubmitting && hasName
        binding.continueBtn.alpha = if (!isSubmitting && hasName) 1f else 0.5f
        binding.continueTv.text = getString(if (showRetry) R.string.Retry else R.string.Continue)
    }

    private fun handleEditView(str: String) {
        binding.apply {
            nameEt.setSelection(nameEt.text.toString().length)
            if (showRetry) {
                showRetry = false
            }
            updateContinueButton()
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
                handleEditView(s.toString())
            }
        }
}

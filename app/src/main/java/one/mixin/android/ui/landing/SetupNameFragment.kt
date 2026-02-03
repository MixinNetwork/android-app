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
            continueBtn.isEnabled = false
            continueBtn.alpha = 0.5f
            
            debug.setOnLongClickListener {
                LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
                true
            }
            
            continueBtn.setOnClickListener {
                if (!nameEt.text.isNullOrBlank()) {
                    performNameUpdate()
                }
            }
            
            nameEt.addTextChangedListener(mWatcher)
            nameEt.setOnEditorActionListener {  _, _, _ ->
                if (nameEt.text.isNotBlank()) {
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

    private fun performNameUpdate() {
        binding.apply {
            continueBtn.isEnabled = false
            nameCover.visibility = VISIBLE
            
            val accountUpdateRequest = AccountUpdateRequest(nameEt.text.toString())
            mobileViewModel.update(accountUpdateRequest)
                .autoDispose(stopScope).subscribe(
                    { r: MixinResponse<Account> ->
                        continueBtn.isEnabled = true
                        nameCover.visibility = INVISIBLE
                        if (!r.isSuccess) {
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                            return@subscribe
                        }
                        r.data?.let { data ->
                            Session.storeAccount(data)
                            mobileViewModel.insertUser(data.toUser())
                        }

                        nameEt.hideKeyboard()
                        initializeBots()
                        val context = requireContext()
                        if (!PrivacyPreference.getIsLoaded(requireContext(), false) ||
                            !PrivacyPreference.getIsSyncSession(context, false)
                        ) {
                            InitializeActivity.showLoading(context, false)
                        } else {
                            startActivity(Intent(context, MainActivity::class.java))
                        }
                        activity?.finish()
                    },
                    { t: Throwable ->
                        continueBtn.isEnabled = true
                        nameCover.visibility = INVISIBLE
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

    private fun initializeBots() {
    }

    private fun handleEditView(str: String) {
        binding.apply {
            nameEt.setSelection(nameEt.text.toString().length)
            if (str.isNotBlank()) {
                continueBtn.isEnabled = true
                continueBtn.alpha = 1.0f
            } else {
                continueBtn.isEnabled = false
                continueBtn.alpha = 0.5f
            }
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

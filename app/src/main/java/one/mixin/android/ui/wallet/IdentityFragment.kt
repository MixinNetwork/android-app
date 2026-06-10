package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.sumsub.sns.core.data.model.SNSCompletionResult
import com.sumsub.sns.core.data.model.SNSException
import com.sumsub.sns.core.data.model.SNSSDKState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentIdentityBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.setting.AppearanceFragment
import one.mixin.android.ui.setting.getLanguagePos
import one.mixin.android.ui.wallet.fiatmoney.FiatMoneyViewModel
import one.mixin.android.util.isFollowSystem
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.sumsub.KycState
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class IdentityFragment : BaseFragment(R.layout.fragment_identity) {
    companion object {
        const val TAG = "IdentityFragment"
        const val ARGS_TOKEN = "args_token"
        const val ARGS_KYC_STATE = "args_kyc_state"
    }

    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val token = requireNotNull(requireArguments().getString(ARGS_TOKEN)) { "required token can not be null" }
        val kycState = requireNotNull(requireArguments().getString(ARGS_KYC_STATE)) { "required kycState can no be null" }
        binding.apply {
            titleView.leftIb.setOnClickListener {
                toCalculate()
            }
            binding.apply {
                when (kycState) {
                    KycState.PENDING.value -> {
                        imageView.setImageResource(R.drawable.ic_identity_verifying)
                        tipTitle.setText(R.string.Identity_Verifying)
                        tipTv.setText(R.string.identity_verifying_description)
                        okTv.setText(R.string.OK)
                        updateTip(false)
                        okTv.setOnClickListener {
                            toCalculate()
                        }
                    }
                    KycState.RETRY.value -> {
                        imageView.setImageResource(R.drawable.ic_verification_failed)
                        tipTitle.setText(R.string.verification_failed)
                        tipTv.setText(R.string.verification_failed_description)
                        okTv.setText(R.string.Continue)
                        updateTip(false)
                        okTv.setOnClickListener {
                            presentSDK(token)
                            toCalculate()
                        }
                    }
                    KycState.BLOCKED.value -> {
                        imageView.setImageResource(R.drawable.ic_verification_failed)
                        tipTitle.setText(R.string.verification_failed)
                        tipTv.setText(R.string.verification_blocked_description)
                        okTv.setText(R.string.chat_with_us)
                        updateTip(true)
                        okTv.setOnClickListener {
                            lifecycleScope.launch {
                                val userTeamMixin = fiatMoneyViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                                if (userTeamMixin == null) {
                                    toast(R.string.Data_error)
                                } else {
                                    ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun toCalculate() {
        findNavController().navigateUp()
    }

    private fun updateTip(isWarning: Boolean) {
        binding.apply {
            if (isWarning) {
                tipTv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 18.dp
                    bottomMargin = 32.dp
                    leftMargin = 22.dp
                    rightMargin = 22.dp
                }
                tipTv.setBackgroundResource(R.drawable.bg_round_8_solid_gray)
                tipTv.setTextColor(requireContext().getColor(R.color.colorRed))
                tipTv.setPadding(14.dp, 12.dp, 14.dp, 12.dp)
            } else {
                tipTv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 32.dp
                    bottomMargin = 68.dp
                    leftMargin = 36.dp
                    rightMargin = 36.dp
                }
                tipTv.setBackgroundResource(0)
                tipTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                tipTv.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun presentSDK(accessToken: String) {
        val tokenExpirationHandler =
            object : TokenExpirationHandler {
                override fun onTokenExpired(): String? {
                    return try {
                        fiatMoneyViewModel.callSumsubToken().execute().body()?.data?.token
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        val onSDKStateChangedHandler: (SNSSDKState, SNSSDKState) -> Unit = { newState, prevState ->
            Timber.e("onSDKStateChangedHandler: $prevState -> $newState")

            when (newState) {
                is SNSSDKState.Ready -> Timber.e("SDK is ready")
                is SNSSDKState.Failed -> {
                    when (newState) {
                        is SNSSDKState.Failed.ApplicantNotFound -> Timber.e(newState.message)
                        is SNSSDKState.Failed.ApplicantMisconfigured -> Timber.e(newState.message)
                        is SNSSDKState.Failed.InitialLoadingFailed ->
                            Timber.e(
                                newState.exception,
                                "Initial loading error",
                            )

                        is SNSSDKState.Failed.InvalidParameters -> Timber.e(newState.message)
                        is SNSSDKState.Failed.NetworkError ->
                            Timber.e(
                                newState.exception,
                                newState.message,
                            )

                        is SNSSDKState.Failed.Unauthorized ->
                            Timber.e(
                                newState.exception,
                                "Invalid token or a token can't be refreshed by the SDK. Please, check your token expiration handler",
                            )

                        is SNSSDKState.Failed.Unknown ->
                            Timber.e(
                                newState.exception,
                                "Unknown error",
                            )
                    }
                }

                is SNSSDKState.Initial -> Timber.e("No verification steps are passed yet")
                is SNSSDKState.Incomplete -> Timber.e("Some but not all verification steps are passed over")
                is SNSSDKState.Pending -> Timber.e("Verification is in pending state")
                is SNSSDKState.FinallyRejected -> Timber.e("Applicant has been finally rejected")
                is SNSSDKState.TemporarilyDeclined -> Timber.e("Applicant has been declined temporarily")
                is SNSSDKState.Approved -> Timber.e("Applicant has been approved")
                else -> Timber.e("Unknown")
            }
        }

        val onSDKErrorHandler: (SNSException) -> Unit = { exception ->
            Timber.e("The SDK throws an exception. Exception: $exception")

            when (exception) {
                is SNSException.Api -> Timber.e("Api exception. ${exception.description}")
                is SNSException.Network -> Timber.e(exception, "Network exception.")
                is SNSException.Unknown -> Timber.e(exception, "Unknown exception.")
            }
        }

        val onSDKCompletedHandler: (SNSCompletionResult, SNSSDKState) -> Unit = { result, state ->
            when (result) {
                is SNSCompletionResult.SuccessTermination -> Timber.e("The SDK finished successfully")
                is SNSCompletionResult.AbnormalTermination -> Timber.e(result.exception, "The SDK got closed because of errors")
                else -> Timber.e("Unknown")
            }
        }

        val snsSdk =
            SNSMobileSDK.Builder(requireActivity())
                .withHandlers(onStateChanged = onSDKStateChangedHandler, onError = onSDKErrorHandler, onCompleted = onSDKCompletedHandler)
                .withAccessToken(accessToken, onTokenExpiration = tokenExpirationHandler)
                .withLocale(
                    if (isFollowSystem()) {
                        Locale.getDefault()
                    } else {
                        val languagePos = getLanguagePos()
                        val selectedLang =
                            when (languagePos) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.language
                                AppearanceFragment.POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.language
                                AppearanceFragment.POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.language
                                AppearanceFragment.POS_RUSSIAN -> Constants.Locale.Russian.Language
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Language
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Language
                                AppearanceFragment.POS_Spanish -> Constants.Locale.Spanish.Language
                                else -> Locale.US.language
                            }
                        val selectedCountry =
                            when (languagePos) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                                AppearanceFragment.POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.country
                                AppearanceFragment.POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.country
                                AppearanceFragment.POS_RUSSIAN -> Constants.Locale.Russian.Country
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Country
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Country
                                AppearanceFragment.POS_Spanish -> Constants.Locale.Spanish.Country
                                else -> Locale.US.country
                            }
                        Locale(selectedLang, selectedCountry)
                    },
                )
                .build()

        snsSdk.launch()
    }
}

package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.sumsub.sns.core.data.model.SNSCompletionResult
import com.sumsub.sns.core.data.model.SNSException
import com.sumsub.sns.core.data.model.SNSSDKState
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentIdentityBinding
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.IdentityVerificationStateBottomSheetDialogFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.viewBinding
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class IdentityFragment : BaseFragment(R.layout.fragment_identity) {
    companion object {
        const val TAG = "IdentityFragment"
        const val ARGS_IS_RETRY = "args_is_retry"
    }

    private val binding by viewBinding(FragmentIdentityBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = requireNotNull(requireArguments().getString(ARGS_TOKEN)) { "required token can not be null" }
        val isRetry = requireArguments().getBoolean(ARGS_IS_RETRY)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            if (isRetry) {
                innerVa.displayedChild = 1
                presentSDK(token)
            } else {
                innerVa.displayedChild = 0
                innerVa.setOnClickListener {
                    innerVa.displayedChild = 1
                    presentSDK(token)
                }
            }
        }
    }

    private fun presentSDK(accessToken: String) {
        val tokenExpirationHandler = object : TokenExpirationHandler {
            override fun onTokenExpired(): String {
                // Access token expired
                // get a new one and pass it to the callback to re-initiate the SDK
                val newToken = "..." // get a new token from your backend
                return newToken
            }
        }
        val onSDKStateChangedHandler: (SNSSDKState, SNSSDKState) -> Unit = { newState, prevState ->
            Timber.d("onSDKStateChangedHandler: $prevState -> $newState")

            when (newState) {
                is SNSSDKState.Ready -> Timber.d("SDK is ready")
                is SNSSDKState.Failed -> {
                    when (newState) {
                        is SNSSDKState.Failed.ApplicantNotFound -> Timber.e(newState.message)
                        is SNSSDKState.Failed.ApplicantMisconfigured -> Timber.e(newState.message)
                        is SNSSDKState.Failed.InitialLoadingFailed -> Timber.e(
                            newState.exception,
                            "Initial loading error",
                        )

                        is SNSSDKState.Failed.InvalidParameters -> Timber.e(newState.message)
                        is SNSSDKState.Failed.NetworkError -> Timber.e(
                            newState.exception,
                            newState.message,
                        )

                        is SNSSDKState.Failed.Unauthorized -> Timber.e(
                            newState.exception,
                            "Invalid token or a token can't be refreshed by the SDK. Please, check your token expiration handler",
                        )

                        is SNSSDKState.Failed.Unknown -> Timber.e(
                            newState.exception,
                            "Unknown error",
                        )
                    }
                }

                is SNSSDKState.Initial -> Timber.d("No verification steps are passed yet")
                is SNSSDKState.Incomplete -> Timber.d("Some but not all verification steps are passed over")
                is SNSSDKState.Pending -> Timber.d("Verification is in pending state")
                is SNSSDKState.FinallyRejected -> Timber.d("Applicant has been finally rejected")
                is SNSSDKState.TemporarilyDeclined -> Timber.d("Applicant has been declined temporarily")
                is SNSSDKState.Approved -> Timber.d("Applicant has been approved")
                else -> Timber.e("Unknown")
            }
        }

        val onSDKErrorHandler: (SNSException) -> Unit = { exception ->
            Timber.d("The SDK throws an exception. Exception: $exception")

            when (exception) {
                is SNSException.Api -> Timber.d("Api exception. ${exception.description}")
                is SNSException.Network -> Timber.d(exception, "Network exception.")
                is SNSException.Unknown -> Timber.d(exception, "Unknown exception.")
            }
        }

        val onSDKCompletedHandler: (SNSCompletionResult, SNSSDKState) -> Unit = { result, state ->
            when (result) {
                is SNSCompletionResult.SuccessTermination -> Timber.d("The SDK finished successfully")
                is SNSCompletionResult.AbnormalTermination -> Timber.e(result.exception, "The SDK got closed because of errors")
                else -> Timber.e("Unknown")
            }
        }

        val snsSdk = SNSMobileSDK.Builder(requireActivity())
            .withHandlers(onStateChanged = onSDKStateChangedHandler, onError = onSDKErrorHandler, onCompleted = onSDKCompletedHandler)
            .withAccessToken(accessToken, onTokenExpiration = tokenExpirationHandler)
            .withLocale(Locale("en")).build()

        snsSdk.launch()
    }
}

package one.mixin.android.ui.landing

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.crypto.clearPendingImportMnemonic
import one.mixin.android.crypto.isMnemonicValid
import one.mixin.android.crypto.mnemonicChecksum
import one.mixin.android.crypto.savePendingImportMnemonic
import one.mixin.android.crypto.toMnemonicWithChecksum
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openCustomerService
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import timber.log.Timber

enum class LoginMnemonicMode(
    val shortWordCount: Int,
    val legacyWordCount: Int,
) {
    TWELVE_OR_TWENTY_FOUR(12, 24),
    THIRTEEN_OR_TWENTY_FIVE(13, 25),
}

private fun LoginMnemonicMode.loginStartType(): String =
    when (this) {
        LoginMnemonicMode.TWELVE_OR_TWENTY_FOUR -> AnalyticsTracker.LoginStartType.LOGIN_MNEMONIC_PHRASE_12
        LoginMnemonicMode.THIRTEEN_OR_TWENTY_FIVE -> AnalyticsTracker.LoginStartType.LOGIN_MNEMONIC_PHRASE_13
    }

private fun LoginMnemonicMode.customerServiceSource(): String =
    when (this) {
        LoginMnemonicMode.TWELVE_OR_TWENTY_FOUR -> AnalyticsTracker.CustomerServiceSource.LOGIN_MNEMONIC_PHRASE_12
        LoginMnemonicMode.THIRTEEN_OR_TWENTY_FIVE -> AnalyticsTracker.CustomerServiceSource.LOGIN_MNEMONIC_PHRASE_13
    }

@AndroidEntryPoint
class LandingMnemonicPhraseFragment : BaseFragment(R.layout.fragment_landing_mnemonic_phrase) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"
        private const val ARGS_MODE = "args_mode"
        private const val ARGS_LOGIN_START_SOURCE = "args_login_start_source"

        fun newInstance(
            mode: LoginMnemonicMode = LoginMnemonicMode.THIRTEEN_OR_TWENTY_FIVE,
            loginStartSource: String = AnalyticsTracker.LoginStartSource.LOGIN_BY,
        ): LandingMnemonicPhraseFragment =
            LandingMnemonicPhraseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_MODE, mode.name)
                    putString(ARGS_LOGIN_START_SOURCE, loginStartSource)
                }
            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val mode: LoginMnemonicMode by lazy {
        arguments?.getString(ARGS_MODE)?.let(LoginMnemonicMode::valueOf)
            ?: LoginMnemonicMode.THIRTEEN_OR_TWENTY_FIVE
    }
    private val loginStartSource: String by lazy {
        arguments?.getString(ARGS_LOGIN_START_SOURCE) ?: AnalyticsTracker.LoginStartSource.LOGIN_BY
    }
    private var scannedMnemonicList by mutableStateOf<List<String>>(emptyList())
    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getScanResult = registerForActivityResult(
            CaptureActivity.CaptureContract()
        ) { intent ->
            intent?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?.let { scannedMnemonicList = it }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        Timber.i("LoginFlow mnemonic_input_open mode=${mode.name}")
        AnalyticsTracker.trackLoginStart(mode.loginStartType(), loginStartSource)
        binding.titleView.titleTv.setTextOnly(R.string.Log_in)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightIb.setOnClickListener {
            openCustomerService(source = mode.customerServiceSource())
        }
        binding.titleView.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        binding.compose.setContent {
            MnemonicPhraseInput(
                state = MnemonicState.Input,
                mnemonicList = scannedMnemonicList,
                inputWordCounts = mode.shortWordCount to mode.legacyWordCount,
                compactInput = true,
                onComplete = { words ->
                    val preparedMnemonic = prepareMnemonicForLogin(words) { sourceWords ->
                        toMnemonicWithChecksum(sourceWords)
                    }
                    val pendingImportWords = preparedMnemonic.pendingImportWords
                    Timber.i(
                        "LoginFlow mnemonic_input_complete mode=${mode.name} word_count=${words.size} pending_import=${pendingImportWords != null}"
                    )
                    if (pendingImportWords != null) {
                        savePendingImportMnemonic(requireContext(), pendingImportWords)
                    } else {
                        clearPendingImportMnemonic(requireContext())
                    }
                    navTo(
                        MnemonicPhraseFragment.newInstance(
                            ArrayList(preparedMnemonic.completedWords),
                            pendingImportWords?.let { ArrayList(it) },
                        ),
                        MnemonicPhraseFragment.TAG,
                    )
                },
                onScan = {
                    getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true))
                },
                validate = ::validateLoginMnemonic,
            )
        }
    }

    private fun validateLoginMnemonic(words: List<String>): String? {
        if (words.size != mode.shortWordCount && words.size != mode.legacyWordCount) {
            return getString(R.string.invalid_mnemonic_phrase)
        }
        val valid = when (words.size) {
            12, 24 -> runCatching { isMnemonicValid(words) }.getOrDefault(false)
            13 -> mnemonicChecksum(words) && runCatching { isMnemonicValid(words.subList(0, 12)) }.getOrDefault(false)
            25 -> mnemonicChecksum(words) && runCatching { isMnemonicValid(words.subList(0, 24)) }.getOrDefault(false)
            else -> false
        }
        return if (valid) null else getString(R.string.invalid_mnemonic_phrase)
    }

    private fun applySafeTopPadding(rootView: View) {
        val originalPaddingTop: Int = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, insets: WindowInsetsCompat ->
            val topInset: Int = maxOf(
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top,
            )
            v.setPadding(v.paddingLeft, originalPaddingTop + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }
}

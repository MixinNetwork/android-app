package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLogDebugBinding
import one.mixin.android.databinding.ViewCaptchaPreviewBottomBinding
import one.mixin.android.db.DatabaseMonitor
import one.mixin.android.db.property.PropertyHelper.findValueByKey
import one.mixin.android.db.property.PropertyHelper.updateKeyValue
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.reminder.RecoveryReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.reminder.ReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.reminder.VerifyMobileReminderBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.TradeFragment
import one.mixin.android.ui.home.web3.trade.perps.PREF_HIDE_SL_GUIDE_UNTIL
import one.mixin.android.ui.home.web3.trade.perps.PREF_HIDE_TP_GUIDE_UNTIL
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.ui.wallet.PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED
import one.mixin.android.ui.wallet.PREF_WALLET_HOME_REFERRAL_CLOSED
import one.mixin.android.util.debug.FileLogTree
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.CaptchaView
import javax.inject.Inject

private const val PREF_WALLET_HOME_CASHBACK_BANNER_CLOSED = "pref_wallet_home_cashback_banner_closed"

@AndroidEntryPoint
class LogAndDebugFragment : BaseFragment(R.layout.fragment_log_debug) {
    companion object {
        const val TAG = "LogAndDebugFragment"

        fun newInstance() = LogAndDebugFragment()
    }

    private val binding by viewBinding(FragmentLogDebugBinding::bind)
    private val viewModel by viewModels<LogAndDebugViewModel>()
    private var captchaView: CaptchaView? = null

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.apply {
                root.setOnClickListener {
                    // do nothing
                }
                titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                webDebugSc.isChecked =
                    defaultSharedPreferences.getBoolean(Constants.Debug.DB_DEBUG, false)
                webDebugSc.setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        if (isChecked) {
                            defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG, true)
                        } else {
                            defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG, false)
                            defaultSharedPreferences.putBoolean(
                                Constants.Debug.DB_DEBUG_WARNING,
                                true,
                            )
                        }
                    }
                }
                webDebug.setOnClickListener {
                    webDebugSc.performClick()
                }

                diagnosis.setOnClickListener {
                    navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
                }

                logs.setOnClickListener {
                    shareLogsFile()
                }
                updateFcmToken.setOnClickListener {
                    updateFcmToken()
                }
                database.setOnClickListener {
                    navTo(
                        DatabaseDebugFragment.newInstance(),
                        DatabaseDebugFragment.TAG,
                    )
                }
                databaseDebugLogsSc.isChecked = findValueByKey(Constants.Debug.DB_DEBUG_LOGS, false)
                databaseDebugLogsSc.setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        updateKeyValue(Constants.Debug.DB_DEBUG_LOGS, isChecked)
                        DatabaseMonitor.reset()
                    }
                }
                databaseDebugLogs.setOnClickListener {
                    databaseDebugLogsSc.performClick()
                }
                safe.setOnClickListener {
                    navTo(
                        SafeDebugFragment.newInstance(),
                        SafeDebugFragment.TAG,
                    )
                }

                previewVerifyMobileReminder.setOnClickListener {
                    VerifyMobileReminderBottomSheetDialogFragment.allowDebugShowOnce(requireContext())
                    toast(R.string.Verify_Mobile_Reminder_Will_Show_Once)
                }

                previewRecoveryReminder.setOnClickListener {
                    RecoveryReminderBottomSheetDialogFragment.allowDebugShowOnce(requireContext())
                    toast(R.string.Recovery_Reminder_Will_Show_Once)
                }

                previewNewUpdateReminder.setOnClickListener {
                    ReminderBottomSheetDialogFragment.allowDebugShowOnce(requireContext())
                    toast(R.string.New_Update_Reminder_Will_Show_Once)
                }

                previewCaptcha.setOnClickListener {
                    showCaptchaPreviewBottom()
                }

                resetTpslGuide.setOnClickListener {
                    resetDebugSharedPreferences()
                    toast(R.string.Reset_TpSl_Guide)
                }

                deleteWeb3Transactions.setOnClickListener {
                    context?.let { ctx ->
                        alertDialogBuilder()
                            .setMessage(R.string.Delete_Web3_Transactions_Confirmation)
                            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                                dialog.dismiss()
                                
                                val progressDialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                    setCancelable(false)
                                }
                                progressDialog.show()
                                
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        viewModel.deleteWallets()
                                        viewModel.deleteAllWeb3Transactions()
                                        viewModel.deleteAllOrders()
                                        withContext(Dispatchers.Main) {
                                            progressDialog.dismiss()
                                            toast(R.string.Web3_Transactions_Deleted)
                                        }
                                        jobManager.addJobInBackground(RefreshWeb3TransactionsJob())
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            progressDialog.dismiss()
                                            toast(getString(R.string.Delete_Failed, e.message))
                                        }
                                    }
                                }
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun updateFcmToken() {
        val googlePlayServicesAvailable = requireContext().isGooglePlayServicesAvailable()
        val progressDialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result =
                try {
                    if (googlePlayServicesAvailable) {
                        withContext(Dispatchers.IO) {
                            viewModel.updateFcmToken()
                        }
                    } else {
                        FcmTokenUpdateResult.Failure("Google Play services unavailable")
                    }
                } finally {
                    progressDialog.dismiss()
                }
            when (result) {
                FcmTokenUpdateResult.Success -> toast(R.string.FCM_Token_Updated)
                is FcmTokenUpdateResult.Failure -> showFcmTokenUpdateError(result.message)
            }
        }
    }

    private fun showFcmTokenUpdateError(message: String) {
        alertDialogBuilder()
            .setTitle(R.string.Update_FCM_Token)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        captchaView?.release()
        captchaView = null
        super.onDestroyView()
    }

    @SuppressLint("InflateParams")
    private fun showCaptchaPreviewBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomBinding = ViewCaptchaPreviewBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(requireActivity(), R.style.Custom),
                R.layout.view_captcha_preview_bottom,
                null,
            ),
        )
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            gCaptcha.setOnClickListener {
                bottomSheet.dismiss()
                previewCaptcha(CaptchaView.CaptchaType.GCaptcha)
            }
            hCaptcha.setOnClickListener {
                bottomSheet.dismiss()
                previewCaptcha(CaptchaView.CaptchaType.HCaptcha)
            }
            gtCaptcha.setOnClickListener {
                bottomSheet.dismiss()
                previewCaptcha(CaptchaView.CaptchaType.GTCaptcha)
            }
        }
        bottomSheet.show()
    }

    private fun previewCaptcha(captchaType: CaptchaView.CaptchaType) {
        val view =
            captchaView ?: CaptchaView(
                requireContext(),
                object : CaptchaView.Callback {
                    override fun onStop() {
                    }

                    override fun onPostToken(value: Pair<CaptchaView.CaptchaType, String>) {
                        toast(getString(R.string.Captcha_Preview_Token_Received, value.first.name))
                    }
                },
            ).also {
                captchaView = it
            }
        view.loadCaptchaWithoutFallback(captchaType)
    }

    private fun resetDebugSharedPreferences() {
        val editor = defaultSharedPreferences.edit()
        debugSharedPreferenceKeys().forEach { key ->
            editor.remove(key)
        }
        editor.apply()
    }

    private fun debugSharedPreferenceKeys(): List<String> =
        buildList {
            add(PREF_HIDE_TP_GUIDE_UNTIL)
            add(PREF_HIDE_SL_GUIDE_UNTIL)
            add(TradeFragment.PREF_TRADE_SPOT_GUIDE_SHOWN)
            add(TradeFragment.PREF_TRADE_PERPETUAL_GUIDE_SHOWN)
            add(Constants.Account.PREF_GLOBAL_MARKET)
            add(Constants.Account.PREF_MARKET_TYPE)
            add(Constants.Account.PREF_MARKET_ORDER)
            add(Constants.Account.PREF_MARKET_TOP_PERCENTAGE)
            add(Constants.Account.PREF_HAS_USED_BUY)
            add(Constants.Account.PREF_HAS_USED_SWAP)
            add(PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED)
            add(PREF_WALLET_HOME_REFERRAL_CLOSED)
            add(PREF_WALLET_HOME_CASHBACK_BANNER_CLOSED)
            Session.getAccountId()?.let { accountId ->
                add("${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}$accountId")
                add("${Constants.Account.PREF_TRADE_LIMIT_ORDER_BADGE_DISMISSED}_$accountId")
                add("${Constants.Account.PREF_TRADE_PERPETUAL_BADGE_DISMISSED}_$accountId")
                add("${Constants.Account.PREF_TRADE_PERPETUAL_ORDER_BADGE_DISMISSED}_$accountId")
            }
        }

    private fun shareLogsFile() {
        val dialog =
            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                setCancelable(false)
            }
        dialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val logFile = FileLogTree.getLogFile()
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                if (logFile.length() <= 0) {
                    toast(R.string.File_does_not_exist)
                }

                Intent().apply {
                    val uri = logFile.absolutePath.toUri()
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val extraMimeTypes = arrayOf("text/plain", "audio/*", "image/*", "video/*")
                    putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
                    type = "application/*"

                    val resInfoList =
                        requireContext().packageManager.queryIntentActivities(
                            this,
                            PackageManager.MATCH_DEFAULT_ONLY,
                        )
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        requireContext().grantUriPermission(
                            packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    try {
                        startActivity(Intent.createChooser(this, logFile.name))
                    } catch (ignored: ActivityNotFoundException) {
                    }
                }
            }
        }
    }
}

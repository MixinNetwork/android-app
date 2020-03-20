package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_wallet_setting.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRIC_INTERVAL
import one.mixin.android.Constants.BIOMETRIC_INTERVAL_DEFAULT
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.setting.BiometricTimeFragment.Companion.X_HOUR
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.Session
import one.mixin.android.vo.Fiats

class WalletSettingFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "WalletSettingFragment"

        fun newInstance() = WalletSettingFragment()
    }

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_wallet_setting, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        change_tv.setOnClickListener {
            navTo(OldPasswordFragment.newInstance(), OldPasswordFragment.TAG)
        }
        current_tv.text = getString(R.string.wallet_setting_currency_desc, Session.getFiatCurrency(), Fiats.getSymbol())
        currency_rl.setOnClickListener {
            val currencyBottom = CurrencyBottomSheetDialogFragment.newInstance()
            currencyBottom.callback = object : CurrencyBottomSheetDialogFragment.Callback {
                override fun onCurrencyClick(currency: Currency) {
                    current_tv?.text = getString(R.string.wallet_setting_currency_desc, currency.name, currency.symbol)
                    refreshLargeAmount(Session.getAccount()!!.transferConfirmationThreshold)
                }
            }
            currencyBottom.showNow(parentFragmentManager, CurrencyBottomSheetDialogFragment.TAG)
        }
        pin_log_tv.setOnClickListener {
            navTo(PinLogsFragment.newInstance(), PinLogsFragment.TAG)
        }
        time_rl.setOnClickListener {
            navTo(BiometricTimeFragment.newInstance(), BiometricTimeFragment.TAG)
        }
        biometrics_sc.isClickable = false
        biometrics_rl.setOnClickListener(biometricsClickListener)
        val open = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        if (open) {
            biometrics_sc.isChecked = true
            time_rl.visibility = VISIBLE
            setTimeDesc()
        } else {
            biometrics_sc.isChecked = false
            time_rl.visibility = GONE
        }
        large_amount_rl.setOnClickListener {
            editDialog {
                titleText = this@WalletSettingFragment.getString(R.string.wallet_transaction_tip_title_with_symbol, Fiats.getSymbol())
                editText = Session.getAccount()!!.transferConfirmationThreshold.toString()
                editHint = this@WalletSettingFragment.getString(R.string.wallet_transaction_tip_title)
                editInputType = InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER
                allowEmpty = false
                rightAction = {
                    savePreference(it.toDouble())
                }
            }
        }
        refreshLargeAmount(Session.getAccount()!!.transferConfirmationThreshold)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == AppCompatActivity.RESULT_OK) {
            updateWhenSuccess()
        }
    }

    private fun savePreference(threshold: Double) = lifecycleScope.launch {
        val pb = indeterminateProgressDialog(message = R.string.pb_dialog_message,
            title = R.string.wallet_transaction_tip_title).apply {
            setCancelable(false)
        }
        pb.show()

        handleMixinResponse(
            invokeNetwork = {
                viewModel.preferences(
                    AccountUpdateRequest(fiatCurrency = Session.getFiatCurrency(),
                        transferConfirmationThreshold = threshold)
                )
            },
            switchContext = Dispatchers.IO,
            successBlock = {
                it.data?.let { account ->
                    Session.storeAccount(account)
                    refreshLargeAmount(account.transferConfirmationThreshold)
                }
            },
            doAfterNetworkSuccess = {
                pb.dismiss()
            },
            exceptionBlock = {
                pb.dismiss()
                return@handleMixinResponse false
            }
        )
    }

    fun setTimeDesc() {
        val biometricInterval = defaultSharedPreferences.getLong(BIOMETRIC_INTERVAL, BIOMETRIC_INTERVAL_DEFAULT)
        val hour = biometricInterval / X_HOUR.toFloat()
        time_desc_tv.text = if (hour < 1) {
            getString(R.string.wallet_pin_pay_interval_minutes, (hour * 60).toInt())
        } else {
            getString(R.string.wallet_pin_pay_interval_hours, hour.toInt())
        }
    }

    private val biometricsClickListener = View.OnClickListener {
        val isSupportWithErrorInfo = BiometricUtil.isSupportWithErrorInfo(requireContext())
        val isSupport = isSupportWithErrorInfo.first
        if (!isSupport) {
            isSupportWithErrorInfo.second?.let {
                biometric_error_tv.text = it
                biometric_error_tv.isVisible = true
            }
            resetBiometricLayout()
            return@OnClickListener
        } else {
            biometric_error_tv.isVisible = false
        }
        if (biometrics_sc.isChecked) {
            resetBiometricLayout()
        } else {
            val bottomSheet =
                PinBiometricsBottomSheetDialogFragment.newInstance(true)
            bottomSheet.callback = object : BiometricBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    updateWhenSuccess()
                }
            }
            bottomSheet.showNow(parentFragmentManager,
                PinBiometricsBottomSheetDialogFragment.TAG
            )
        }
    }

    private fun updateWhenSuccess() {
        biometrics_sc.isChecked = true
        time_rl.visibility = VISIBLE
        setTimeDesc()
        defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_BIOMETRICS, true)
    }

    private fun resetBiometricLayout() {
        biometrics_sc.isChecked = false
        time_rl.visibility = GONE
        BiometricUtil.deleteKey(requireContext())
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLargeAmount(largeAmount: Double) {
        if (!isAdded) return
        val symbol = Fiats.getSymbol()
        large_amount_tv.text = getString(R.string.wallet_setting_currency_desc, largeAmount.toString(), symbol)
        large_amount_desc_tv.text = getString(R.string.setting_transfer_large_summary, "$largeAmount$symbol")
    }
}

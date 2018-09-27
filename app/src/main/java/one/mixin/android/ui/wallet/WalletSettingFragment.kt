package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_wallet_setting.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRIC_INTERVAL
import one.mixin.android.Constants.BIOMETRIC_INTERVAL_DEFAULT
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.BiometricTimeFragment.Companion.X_HOUR
import one.mixin.android.util.BiometricUtil
import org.jetbrains.anko.support.v4.defaultSharedPreferences

class WalletSettingFragment : BaseFragment() {
    companion object {
        const val TAG = "WalletSettingFragment"

        fun newInstance() = WalletSettingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_wallet_setting, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title.left_ib.setOnClickListener { activity?.onBackPressed() }
        change_tv.setOnClickListener {
            activity?.addFragment(this@WalletSettingFragment, OldPasswordFragment.newInstance(), OldPasswordFragment.TAG)
        }
        time_rl.setOnClickListener {
            activity?.addFragment(this@WalletSettingFragment, BiometricTimeFragment.newInstance(), BiometricTimeFragment.TAG)
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
        if (biometrics_sc.isChecked) {
            biometrics_sc.isChecked = false
            time_rl.visibility = GONE
            BiometricUtil.deleteKey(requireContext())
        } else {
            val bottomSheet = PinBiometricsBottomSheetDialogFragment.newInstance(true)
            bottomSheet.callback = object : PinBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    biometrics_sc.isChecked = true
                    time_rl.visibility = VISIBLE
                    setTimeDesc()
                    defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
                    defaultSharedPreferences.putBoolean(Constants.Account.PREF_BIOMETRICS, true)
                }
            }
            bottomSheet.showNow(requireFragmentManager(), PinBiometricsBottomSheetDialogFragment.TAG)
        }
    }
}
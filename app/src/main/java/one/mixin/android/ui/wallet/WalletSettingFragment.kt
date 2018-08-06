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
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.remove
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
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
        time_tv.setOnClickListener {
            activity?.addFragment(this@WalletSettingFragment, BiometricTimeFragment.newInstance(), BiometricTimeFragment.TAG)
        }
        biometrics_sc.isClickable = false
        biometrics_rl.setOnClickListener(biometricsClickListener)
        val open = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        biometrics_sc.isChecked = open
        time_tv.visibility = if (open) VISIBLE else GONE
    }

    private val biometricsClickListener = View.OnClickListener {
        if (biometrics_sc.isChecked) {
            biometrics_sc.isChecked = false
            time_tv.visibility = GONE
            defaultSharedPreferences.remove(Constants.BIOMETRICS_IV)
            defaultSharedPreferences.remove(Constants.BIOMETRICS_ALIAS)
            BiometricUtil.deleteKey()
            save2Pref(false)
        } else {
            val bottomSheet = PinBiometricsBottomSheetDialogFragment.newInstance(true)
            bottomSheet.callback = object : PinBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    biometrics_sc.isChecked = true
                    time_tv.visibility = VISIBLE
                    defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
                    save2Pref(true)
                }
            }
            bottomSheet.showNow(requireFragmentManager(), PinBiometricsBottomSheetDialogFragment.TAG)
        }
    }

    private fun save2Pref(open: Boolean) {
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_BIOMETRICS, open)
    }
}
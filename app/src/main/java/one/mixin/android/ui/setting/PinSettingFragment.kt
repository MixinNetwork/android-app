package one.mixin.android.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRIC_INTERVAL
import one.mixin.android.Constants.BIOMETRIC_INTERVAL_DEFAULT
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPinSettingBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.setting.BiometricTimeFragment.Companion.X_HOUR
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil

@AndroidEntryPoint
class PinSettingFragment : BaseSettingFragment<FragmentPinSettingBinding>() {
    companion object {
        const val TAG = "PinSettingFragment"

        fun newInstance() = PinSettingFragment()
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPinSettingBinding.inflate(inflater, container, false).apply {
            _titleBinding = ViewTitleBinding.bind(title)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleBinding.leftIb.setOnClickListener { activity?.onBackPressed() }
        binding.apply {
            changeTv.setOnClickListener {
                navTo(OldPasswordFragment.newInstance(), OldPasswordFragment.TAG)
            }
            timeRl.setOnClickListener {
                navTo(BiometricTimeFragment.newInstance(), BiometricTimeFragment.TAG)
            }
            biometricsSc.isClickable = false
            biometricsRl.setOnClickListener(biometricsClickListener)
            val open = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
            if (open) {
                biometricsSc.isChecked = true
                timeRl.visibility = VISIBLE
                setTimeDesc()
            } else {
                biometricsSc.isChecked = false
                timeRl.visibility = GONE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BiometricUtil.REQUEST_CODE_CREDENTIALS && resultCode == AppCompatActivity.RESULT_OK) {
            updateWhenSuccess()
        }
    }

    fun setTimeDesc() {
        val biometricInterval = defaultSharedPreferences.getLong(BIOMETRIC_INTERVAL, BIOMETRIC_INTERVAL_DEFAULT)
        val hour = biometricInterval / X_HOUR.toFloat()
        binding.timeDescTv.text = if (hour < 1) {
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
                binding.biometricErrorTv.text = it
                binding.biometricErrorTv.isVisible = true
            }
            resetBiometricLayout()
            return@OnClickListener
        } else {
            binding.biometricErrorTv.isVisible = false
        }
        if (binding.biometricsSc.isChecked) {
            resetBiometricLayout()
        } else {
            val bottomSheet =
                PinBiometricsBottomSheetDialogFragment.newInstance(true)
            bottomSheet.callback = object : BiometricBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    updateWhenSuccess()
                }
            }
            bottomSheet.showNow(
                parentFragmentManager,
                PinBiometricsBottomSheetDialogFragment.TAG
            )
        }
    }

    private fun updateWhenSuccess() {
        binding.biometricsSc.isChecked = true
        binding.timeRl.visibility = VISIBLE
        setTimeDesc()
        defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_BIOMETRICS, true)
    }

    private fun resetBiometricLayout() {
        binding.biometricsSc.isChecked = false
        binding.timeRl.visibility = GONE
        BiometricUtil.deleteKey(requireContext())
    }
}

package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.biometric.BiometricManager
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRIC_INTERVAL
import one.mixin.android.Constants.BIOMETRIC_INTERVAL_DEFAULT
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPinSettingBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.BiometricTimeFragment.Companion.X_HOUR
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class PinSettingFragment : BaseFragment(R.layout.fragment_pin_setting) {
    companion object {
        const val TAG = "PinSettingFragment"

        fun newInstance() = PinSettingFragment()
    }

    private val binding by viewBinding(FragmentPinSettingBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.apply {
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            }
            changeTv.setOnClickListener {
                TipActivity.show(requireActivity(), TipType.Change)
            }
            timeRl.setOnClickListener {
                navTo(BiometricTimeFragment.newInstance(), BiometricTimeFragment.TAG)
            }

            randomSc.isClickable = false
            randomRl.setOnClickListener(randomClickListener)
            val randomKeyboardEnabled = defaultSharedPreferences.getBoolean(Constants.Account.PREF_RANDOM, false)
            randomSc.isChecked = randomKeyboardEnabled

            biometricsSc.isClickable = false
            biometricsRl.setOnClickListener(biometricsClickListener)
            val open = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
            if (open) {
                biometricsRl.setBackgroundResource(R.drawable.ripple_round_window_top)
                biometricsSc.isChecked = true
                timeRl.visibility = VISIBLE
                setTimeDesc()
            } else {
                biometricsRl.setBackgroundResource(R.drawable.ripple_round_window)
                biometricsSc.isChecked = false
                timeRl.visibility = GONE
            }
            val url = Constants.HelpLink.TIP
            val desc = getString(R.string.wallet_pin_tops_desc)
            tipTv.highlightStarTag(desc, arrayOf(url))
        }
    }

    fun setTimeDesc() {
        val biometricInterval = defaultSharedPreferences.getLong(BIOMETRIC_INTERVAL, BIOMETRIC_INTERVAL_DEFAULT)
        val hour = biometricInterval / X_HOUR.toFloat()
        binding.timeDescTv.text =
            if (hour < 1) {
                requireContext().resources.getQuantityString(R.plurals.Minute, (hour * 60).toInt(), (hour * 60).toInt())
            } else {
                requireContext().resources.getQuantityString(R.plurals.Hour, hour.toInt(), hour.toInt())
            }
    }

    private val randomClickListener =
        View.OnClickListener {
            binding.randomSc.isChecked = !binding.randomSc.isChecked
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_RANDOM, binding.randomSc.isChecked)
        }

    private val biometricsClickListener =
        View.OnClickListener {
            val isSupportWithErrorInfo = BiometricUtil.isSupportWithErrorInfo(requireContext(), BiometricManager.Authenticators.BIOMETRIC_STRONG)
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
                val bottomSheet = PinBiometricsBottomSheetDialogFragment.newInstance(true)
                bottomSheet.onSavePinSuccess = {
                    updateWhenSuccess()
                }
                bottomSheet.showNow(parentFragmentManager, PinBiometricsBottomSheetDialogFragment.TAG)
            }
        }

    private fun updateWhenSuccess() {
        binding.biometricsSc.isChecked = true
        binding.timeRl.visibility = VISIBLE
        binding.biometricsRl.setBackgroundResource(R.drawable.ripple_round_window_top)
        setTimeDesc()
        defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_BIOMETRICS, true)
    }

    private fun resetBiometricLayout() {
        binding.biometricsSc.isChecked = false
        binding.timeRl.visibility = GONE
        binding.biometricsRl.setBackgroundResource(R.drawable.ripple_round_window)
        BiometricUtil.deleteKey(requireContext())
    }
}

package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.databinding.FragmentWalletPasswordBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.putLong
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.toUser
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class WalletPasswordFragment : BaseFragment(R.layout.fragment_wallet_password), PinView.OnPinListener {

    companion object {
        const val TAG = "WalletPasswordFragment"
        const val ARGS_CHANGE = "args_change"
        const val ARGS_OLD_PASSWORD = "args_old_password"

        private const val STEP1 = 0
        private const val STEP2 = 1
        private const val STEP3 = 2
        private const val STEP4 = 3

        fun newInstance(change: Boolean = false, oldPassword: String? = null): WalletPasswordFragment {
            return WalletPasswordFragment().withArgs {
                putBoolean(ARGS_CHANGE, change)
                if (change) {
                    putString(ARGS_OLD_PASSWORD, oldPassword)
                }
            }
        }
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private val binding by viewBinding(FragmentWalletPasswordBinding::bind)

    private val change: Boolean by lazy {
        requireArguments().getBoolean(ARGS_CHANGE)
    }
    private val oldPassword: String? by lazy {
        if (change) requireArguments().getString(ARGS_OLD_PASSWORD) else null
    }

    private var step = STEP1

    private var lastPassword: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding.apply {
            if (change) {
                titleView.setSubTitle(getString(R.string.wallet_password_set_new_pin), "2/5")
                tipTv.text = getString(R.string.wallet_password_set_new_pin_desc)
            } else {
                titleView.setSubTitle(getString(R.string.Set_PIN), "1/4")
                val url = Constants.HelpLink.TIP
                val target = getString(R.string.learn_more)
                val desc = getString(R.string.wallet_password_set_pin_desc)
                tipTv.highlightLinkText(desc, arrayOf(target), arrayOf(url))
            }
            titleView.leftIb.setOnClickListener {
                when (step) {
                    STEP1 -> activity?.onBackPressed()
                    STEP2 -> toStep1()
                    STEP3 -> toStep2()
                    STEP4 -> toStep3()
                }
            }
            disableTitleRight()
            titleView.rightAnimator.setOnClickListener { createPin() }
            pin.setListener(this@WalletPasswordFragment)
            keyboard.setKeyboardKeys(Constants.KEYS)
            keyboard.setOnClickKeyboardListener(keyboardListener)
            keyboard.animate().translationY(0f).start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onUpdate(index: Int) {
        if (index == binding.pin.getCount()) {
            binding.titleView.rightTv.setTextColor(resources.getColor(R.color.colorBlue, null))
            binding.titleView.rightAnimator.isEnabled = true
        } else {
            disableTitleRight()
        }
    }

    override fun onBackPressed(): Boolean {
        when (step) {
            STEP1 -> return false
            STEP2 -> {
                toStep1()
                return true
            }
            STEP3 -> {
                toStep2()
                return true
            }
            STEP4 -> {
                toStep3()
                return true
            }
            else -> return false
        }
    }

    private fun disableTitleRight() {
        binding.titleView.apply {
            rightTv.setTextColor(resources.getColor(R.color.text_gray, null))
            rightAnimator.isEnabled = false
        }
    }

    private fun toStep1() {
        step = STEP1
        lastPassword = null
        binding.pin.clear()
        binding.titleView.setSubTitle(
            getString(if (change) R.string.wallet_password_set_new_pin else R.string.Set_PIN),
            getSubTitle()
        )
        if (change) {
            binding.tipTv.text = getString(R.string.wallet_password_set_new_pin_desc)
        } else {
            val url = Constants.HelpLink.TIP
            val target = getString(R.string.learn_more)
            val desc = getString(R.string.wallet_password_set_pin_desc)
            binding.tipTv.highlightLinkText(desc, arrayOf(target), arrayOf(url))
        }
    }

    private fun toStep2(check: Boolean = false) {
        if (check && !validatePin()) {
            binding.pin.clear()
            return
        }

        step = STEP2
        lastPassword = binding.pin.code()
        binding.apply {
            pin.clear()
            titleView.setSubTitle(getString(R.string.Confirm_PIN), getSubTitle())
            tipTv.text = getString(R.string.wallet_password_confirm_1)
        }
    }

    private fun toStep3(check: Boolean = false) {
        if (check && checkEqual()) return

        step = STEP3
        binding.apply {
            pin.clear()
            titleView.setSubTitle(getString(R.string.Confirm_PIN), getSubTitle())
            tipTv.text = getString(R.string.wallet_password_confirm_2)
        }
    }

    private fun toStep4(check: Boolean = false) {
        if (check && checkEqual()) return

        step = STEP4
        binding.apply {
            pin.clear()
            titleView.setSubTitle(getString(R.string.Confirm_PIN), getSubTitle())
            tipTv.text = getString(R.string.wallet_password_confirm_3)
        }
    }

    private fun getSubTitle(): String {
        return when (step) {
            STEP1 -> if (change) "2/5" else "1/4"
            STEP2 -> if (change) "3/5" else "2/4"
            STEP3 -> if (change) "4/5" else "3/4"
            STEP4 -> if (change) "5/5" else "4/4"
            else -> throw IllegalArgumentException("")
        }
    }

    private fun validatePin(): Boolean {
        val pin = binding.pin.code()
        if (pin == "123456") {
            toast(R.string.wallet_password_unsafe)
            return false
        }

        val numKind = arrayListOf<Char>()
        pin.forEach {
            if (!numKind.contains(it)) {
                numKind.add(it)
            }
        }
        if (numKind.size <= 2) {
            toast(R.string.wallet_password_unsafe)
            return false
        }

        return true
    }

    private fun createPin() {
        when (step) {
            STEP1 -> toStep2(true)
            STEP2 -> toStep3(true)
            STEP3 -> toStep4(true)
            STEP4 -> {
                if (checkEqual()) return

                val dialog = indeterminateProgressDialog(
                    message = getString(R.string.Please_wait_a_bit),
                    title = if (change) getString(R.string.changing) else getString(R.string.Creating)
                )
                dialog.setCancelable(false)
                dialog.show()

                if (viewDestroyed()) return

                walletViewModel.updatePin(binding.pin.code(), oldPassword)
                    .autoDispose(stopScope).subscribe(
                        { r: MixinResponse<Account> ->
                            if (r.isSuccess) {
                                r.data?.let {
                                    Session.storeAccount(it)
                                    walletViewModel.insertUser(it.toUser())

                                    val cur = System.currentTimeMillis()
                                    defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
                                    putPrefPinInterval(requireContext(), INTERVAL_10_MINS)

                                    val openBiometrics = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
                                    if (openBiometrics) {
                                        BiometricUtil.savePin(requireContext(), binding.pin.code(), this@WalletPasswordFragment)
                                    }

                                    activity?.let { activity ->
                                        if (activity is ConversationActivity ||
                                            activity is ContactsActivity
                                        ) {
                                            toast(R.string.wallet_set_password_success)
                                            parentFragmentManager.popBackStackImmediate()
                                        } else if (activity is MainActivity) {
                                            toast(R.string.wallet_set_password_success)
                                            parentFragmentManager.popBackStackImmediate()
                                            WalletActivity.show(activity)
                                        } else {
                                            if (change) {
                                                toast(R.string.wallet_change_password_success)
                                            } else {
                                                toast(R.string.wallet_set_password_success)
                                            }
                                            parentFragmentManager.popBackStackImmediate()
                                        }
                                    }
                                }
                            } else {
                                ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                            }
                            dialog.dismiss()
                        },
                        { t ->
                            dialog.dismiss()
                            ErrorHandler.handleError(t)
                        }
                    )
            }
        }
    }

    private fun checkEqual(): Boolean {
        if (lastPassword != binding.pin.code()) {
            toast(R.string.wallet_password_not_equal)
            toStep1()
            return true
        }
        return false
    }

    private val keyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tickVibrate()
            if (position == 11) {
                binding.pin.delete()
            } else {
                binding.pin.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.clickVibrate()
            if (position == 11) {
                binding.pin.clear()
            } else {
                binding.pin.append(value)
            }
        }
    }
}

package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_wallet_password.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toast
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.toUser
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class WalletPasswordFragment : BaseFragment(), PinView.OnPinListener {

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

    private val change: Boolean by lazy {
        requireArguments().getBoolean(ARGS_CHANGE)
    }
    private val oldPassword: String? by lazy {
        if (change) requireArguments().getString(ARGS_OLD_PASSWORD) else null
    }

    private var step = STEP1

    private var lastPassword: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_wallet_password, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (change) {
            title_view.setSubTitle(getString(R.string.wallet_password_set_new_pin), "2/5")
            tip_tv.text = getString(R.string.wallet_password_set_new_pin_desc)
        } else {
            title_view.setSubTitle(getString(R.string.wallet_password_set_pin), "1/4")
        }
        title_view.left_ib.setOnClickListener {
            when (step) {
                STEP1 -> activity?.onBackPressed()
                STEP2 -> toStep1()
                STEP3 -> toStep2()
                STEP4 -> toStep3()
            }
        }
        disableTitleRight()
        title_view.right_animator.setOnClickListener { createPin() }
        pin.setListener(this)
        keyboard.setKeyboardKeys(Constants.KEYS)
        keyboard.setOnClickKeyboardListener(keyboardListener)
        keyboard.animate().translationY(0f).start()
    }

    override fun onUpdate(index: Int) {
        if (index == pin.getCount()) {
            title_view.right_tv.setTextColor(resources.getColor(R.color.colorBlue, null))
            title_view.right_animator.isEnabled = true
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
        title_view.right_tv.setTextColor(resources.getColor(R.color.text_gray, null))
        title_view.right_animator.isEnabled = false
    }

    private fun toStep1() {
        step = STEP1
        lastPassword = null
        pin.clear()
        title_view.setSubTitle(
            getString(if (change) R.string.wallet_password_set_new_pin else R.string.wallet_password_set_pin),
            getSubTitle()
        )
        tip_tv.text = getString(if (change) R.string.wallet_password_set_new_pin_desc else R.string.wallet_password_set_pin_desc)
    }

    private fun toStep2(check: Boolean = false) {
        if (check && !validatePin()) {
            pin.clear()
            return
        }

        step = STEP2
        lastPassword = pin.code()
        pin.clear()
        title_view.setSubTitle(getString(R.string.wallet_password_confirm_pin), getSubTitle())
        tip_tv.text = getString(R.string.wallet_password_confirm_1)
    }

    private fun toStep3(check: Boolean = false) {
        if (check && checkEqual()) return

        step = STEP3
        pin.clear()
        title_view.setSubTitle(getString(R.string.wallet_password_confirm_pin), getSubTitle())
        tip_tv.text = getString(R.string.wallet_password_confirm_2)
    }

    private fun toStep4(check: Boolean = false) {
        if (check && checkEqual()) return

        step = STEP4
        pin.clear()
        title_view.setSubTitle(getString(R.string.wallet_password_confirm_pin), getSubTitle())
        tip_tv.text = getString(R.string.wallet_password_confirm_3)
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
        val pin = pin.code()
        if (pin == "123456") {
            context?.toast(R.string.wallet_password_unsafe)
            return false
        }

        val numKind = arrayListOf<Char>()
        pin.forEach {
            if (!numKind.contains(it)) {
                numKind.add(it)
            }
        }
        if (numKind.size <= 2) {
            context?.toast(R.string.wallet_password_unsafe)
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
                    message = getString(R.string.pb_dialog_message),
                    title = if (change) getString(R.string.changing) else getString(R.string.group_creating)
                )
                dialog.setCancelable(false)
                dialog.show()

                walletViewModel.updatePin(pin.code(), oldPassword)
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
                                        BiometricUtil.savePin(requireContext(), pin.code(), this@WalletPasswordFragment)
                                    }

                                    activity?.let { activity ->
                                        if (activity is ConversationActivity ||
                                            activity is ContactsActivity
                                        ) {
                                            context?.toast(R.string.wallet_set_password_success)
                                            parentFragmentManager.popBackStackImmediate()
                                        } else if (activity is MainActivity) {
                                            context?.toast(R.string.wallet_set_password_success)
                                            parentFragmentManager.popBackStackImmediate()
                                            WalletActivity.show(activity)
                                        } else {
                                            if (change) {
                                                context?.toast(R.string.wallet_change_password_success)
                                            } else {
                                                context?.toast(R.string.wallet_set_password_success)
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
        if (lastPassword != pin.code()) {
            context?.toast(R.string.wallet_password_not_equal)
            toStep1()
            return true
        }
        return false
    }

    private val keyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin.delete()
            } else {
                pin.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                pin.clear()
            } else {
                pin.append(value)
            }
        }
    }
}

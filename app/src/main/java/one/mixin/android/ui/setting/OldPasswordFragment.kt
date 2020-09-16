package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_old_password.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class OldPasswordFragment : BaseFragment(), PinView.OnPinListener {

    companion object {
        const val TAG = "OldPasswordFragment"

        fun newInstance(): OldPasswordFragment =
            OldPasswordFragment()
    }

    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_old_password, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.setSubTitle(getString(R.string.wallet_password_old_title), "1/5")
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener { verify(pin.code()) }
        disableTitleRight()
        pin.setListener(this)
        keyboard.setKeyboardKeys(KEYS)
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

    private fun disableTitleRight() {
        title_view.right_tv.setTextColor(resources.getColor(R.color.text_gray, null))
        title_view.right_animator.isEnabled = false
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(
            message = getString(R.string.pb_dialog_message),
            title = getString(R.string.wallet_verifying)
        )
        dialog.setCancelable(false)
        dialog.show()
        handleMixinResponse(
            invokeNetwork = { walletViewModel.verifyPin(pinCode) },
            successBlock = { response ->
                dialog.dismiss()
                context?.updatePinCheck()
                response.data?.let {
                    val pin = pin.code()
                    activity?.onBackPressed()
                    navTo(WalletPasswordFragment.newInstance(true, pin), WalletPasswordFragment.TAG)
                }
            },
            exceptionBlock = {
                dialog.dismiss()
                pin.clear()
                return@handleMixinResponse false
            },
            failureBlock = {
                pin.clear()
                if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                    dialog.dismiss()
                    toast(R.string.error_pin_check_too_many_request)
                    return@handleMixinResponse true
                } else if (it.errorCode == ErrorHandler.PIN_INCORRECT) {
                    val errorCount = walletViewModel.errorCount()
                    toast(getString(R.string.error_pin_incorrect_with_times, ErrorHandler.PIN_INCORRECT, errorCount))
                    dialog.dismiss()
                    return@handleMixinResponse true
                }
                dialog.dismiss()
                return@handleMixinResponse false
            }
        )
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

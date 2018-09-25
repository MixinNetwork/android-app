package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_old_password.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

class OldPasswordFragment : BaseFragment(), PinView.OnPinListener {

    companion object {
        const val TAG = "OldPasswordFragment"

        fun newInstance(): OldPasswordFragment = OldPasswordFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_old_password, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.setSubTitle(getString(R.string.wallet_password_old_title), "1/5")
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener { verify() }
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

    private fun verify() {
        val dialog = indeterminateProgressDialog(message = getString(R.string.pb_dialog_message),
            title = getString(R.string.wallet_verifying))
        dialog.setCancelable(false)
        dialog.show()
        walletViewModel.verifyPin(pin.code()).autoDisposable(scopeProvider).subscribe({ r: MixinResponse<Account> ->
            dialog.dismiss()
            if (r.isSuccess) {
                context?.updatePinCheck()
                r.data?.let {
                    activity?.addFragment(this@OldPasswordFragment,
                        WalletPasswordFragment.newInstance(true, pin.code()), WalletPasswordFragment.TAG)
                }
            } else {
                pin.clear()
                ErrorHandler.handleMixinError(r.errorCode)
            }
        }, { t ->
            dialog.dismiss()
            pin.clear()
            ErrorHandler.handleError(t)
        })
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
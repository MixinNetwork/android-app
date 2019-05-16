package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.uber.autodispose.autoDisposable
import kotlinx.android.synthetic.main.fragment_verify_pin.*
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.repository.AccountRepository
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

class VerifyFragment : BaseFragment(), PinView.OnPinListener {

    companion object {
        val TAG = VerifyFragment::class.java.simpleName

        fun newInstance() = VerifyFragment()
    }

    @Inject
    lateinit var accountRepository: AccountRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_verify_pin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        close_iv.setOnClickListener { activity?.onBackPressed() }
        pin.setListener(this)
        keyboard.setKeyboardKeys(KEYS)
        keyboard.setOnClickKeyboardListener(keyboardListener)
        keyboard.animate().translationY(0f).start()
    }

    override fun onUpdate(index: Int) {
        if (index == pin.getCount()) {
            verify_fab.visibility = VISIBLE
            verify_fab.show()
            verify_cover.visibility = VISIBLE
            accountRepository.verifyPin(pin.code())
                .autoDisposable(scopeProvider)
                .subscribe({ r: MixinResponse<Account> ->
                    verify_fab?.hide()
                    verify_fab?.visibility = GONE
                    verify_cover?.visibility = GONE
                    if (r.isSuccess) {
                        context?.updatePinCheck()
                        LandingActivity.show(context!!, pin.code())
                    } else {
                        pin.clear()
                        ErrorHandler.handleMixinError(r.errorCode)
                    }
                }, { t: Throwable ->
                    verify_fab?.hide()
                    verify_cover?.visibility = GONE
                    ErrorHandler.handleError(t)
                })
        }
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
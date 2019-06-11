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
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.AccountRepository
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.setting.FriendsNoBotFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

class VerifyFragment : BaseFragment(), PinView.OnPinListener {

    companion object {
        val TAG = VerifyFragment::class.java.simpleName

        const val FROM_PHONE = 0
        const val FROM_EMERGENCY = 1

        const val ARGS_FROM = "args_from"

        fun newInstance(from: Int) = VerifyFragment().withArgs {
            putInt(ARGS_FROM, from)
        }
    }

    private val from by lazy { arguments!!.getInt(ARGS_FROM) }

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
                .autoDisposable(stopScope)
                .subscribe({ r: MixinResponse<Account> ->
                    verify_fab?.hide()
                    verify_fab?.visibility = GONE
                    verify_cover?.visibility = GONE
                    if (r.isSuccess) {
                        context?.updatePinCheck()
                        if (from == FROM_PHONE) {
                            LandingActivity.show(context!!, pin.code())
                        } else if (from == FROM_EMERGENCY) {
                            activity?.supportFragmentManager?.inTransaction {
                                remove(this@VerifyFragment)
                            }
                            val f = FriendsNoBotFragment.newInstance(pin.code())
                            activity?.addFragment(this@VerifyFragment, f, FriendsNoBotFragment.TAG)
                        }
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
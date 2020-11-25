package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_verify_pin.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.tapVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.AccountRepository
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.setting.FriendsNoBotFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

@AndroidEntryPoint
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

    private val from by lazy { requireArguments().getInt(ARGS_FROM) }

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
            verify(pin.code())
        }
    }

    private fun showLoading() {
        verify_fab.visibility = VISIBLE
        verify_fab.show()
        verify_cover.visibility = VISIBLE
    }

    private fun hideLoading() {
        verify_fab?.hide()
        verify_fab?.visibility = GONE
        verify_cover?.visibility = GONE
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        showLoading()
        handleMixinResponse(
            invokeNetwork = { accountRepository.verifyPin(pinCode) },
            successBlock = {
                hideLoading()
                pin?.clear()
                context?.updatePinCheck()
                activity?.supportFragmentManager?.inTransaction {
                    remove(this@VerifyFragment)
                }
                if (from == FROM_PHONE) {
                    LandingActivity.show(requireContext(), pinCode)
                } else if (from == FROM_EMERGENCY) {
                    val f = FriendsNoBotFragment.newInstance(pinCode)
                    activity?.addFragment(this@VerifyFragment, f, FriendsNoBotFragment.TAG)
                }
            },
            failureBlock = {
                pin?.clear()
                if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                    hideLoading()
                    toast(R.string.error_pin_check_too_many_request)
                    return@handleMixinResponse true
                } else if (it.errorCode == ErrorHandler.PIN_INCORRECT) {
                    val errorCount = accountRepository.errorCount()
                    hideLoading()
                    toast(getString(R.string.error_pin_incorrect_with_times, ErrorHandler.PIN_INCORRECT, errorCount))
                    return@handleMixinResponse true
                }
                hideLoading()
                return@handleMixinResponse false
            },
            exceptionBlock = {
                hideLoading()
                pin?.clear()
                return@handleMixinResponse false
            }
        )
    }

    private val keyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tapVibrate()
            if (position == 11) {
                pin.delete()
            } else {
                pin.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.tapVibrate()
            if (position == 11) {
                pin.clear()
            } else {
                pin.append(value)
            }
        }
    }
}

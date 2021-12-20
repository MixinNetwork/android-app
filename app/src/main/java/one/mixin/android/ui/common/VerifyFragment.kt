package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentVerifyPinBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.AccountRepository
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.landing.MobileFragment
import one.mixin.android.ui.setting.FriendsNoBotFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

@AndroidEntryPoint
class VerifyFragment : BaseFragment(R.layout.fragment_verify_pin), PinView.OnPinListener {

    companion object {
        val TAG = VerifyFragment::class.java.simpleName

        const val FROM_PHONE = 0
        const val FROM_EMERGENCY = 1
        const val FROM_DELETE_ACCOUNT = 2

        const val ARGS_FROM = "args_from"

        fun newInstance(from: Int) = VerifyFragment().withArgs {
            putInt(ARGS_FROM, from)
        }
    }

    private val from by lazy { requireArguments().getInt(ARGS_FROM) }

    @Inject
    lateinit var accountRepository: AccountRepository

    private val binding by viewBinding(FragmentVerifyPinBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding.apply {
            closeIv.setOnClickListener { activity?.onBackPressed() }
            pin.setListener(this@VerifyFragment)
            keyboard.setKeyboardKeys(KEYS)
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
            verify(binding.pin.code())
        }
    }

    private fun showLoading() {
        if (viewDestroyed()) return
        binding.apply {
            verifyFab.visibility = VISIBLE
            verifyFab.show()
            verifyCover.visibility = VISIBLE
        }
    }

    private fun hideLoading() {
        if (viewDestroyed()) return
        binding.apply {
            verifyFab.hide()
            verifyFab.visibility = GONE
            verifyCover.visibility = GONE
        }
    }

    private fun clearPin() {
        if (viewDestroyed()) return
        binding.pin.clear()
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        showLoading()
        handleMixinResponse(
            invokeNetwork = { accountRepository.verifyPin(pinCode) },
            successBlock = {
                hideLoading()
                clearPin()
                context?.updatePinCheck()
                activity?.supportFragmentManager?.inTransaction {
                    remove(this@VerifyFragment)
                }
                when (from) {
                    FROM_PHONE -> {
                        LandingActivity.show(requireContext(), pinCode)
                    }
                    FROM_EMERGENCY -> {
                        val f = FriendsNoBotFragment.newInstance(pinCode)
                        activity?.addFragment(this@VerifyFragment, f, FriendsNoBotFragment.TAG)
                    }
                    FROM_DELETE_ACCOUNT -> {
                        val f = MobileFragment.newInstance(from = from)
                        activity?.addFragment(this@VerifyFragment, f, MobileFragment.TAG)
                    }
                    else -> {
                        throw IllegalArgumentException("Illegal argument")
                    }
                }
            },
            failureBlock = {
                clearPin()
                if (it.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                    hideLoading()
                    toast(R.string.error_pin_check_too_many_request)
                    return@handleMixinResponse true
                } else if (it.errorCode == ErrorHandler.PIN_INCORRECT) {
                    val errorCount = accountRepository.errorCount()
                    hideLoading()
                    toast(
                        getString(
                            R.string.error_pin_incorrect_with_times,
                            ErrorHandler.PIN_INCORRECT,
                            errorCount
                        )
                    )
                    return@handleMixinResponse true
                }
                hideLoading()
                return@handleMixinResponse false
            },
            exceptionBlock = {
                hideLoading()
                clearPin()
                return@handleMixinResponse false
            }
        )
    }

    private val keyboardListener: Keyboard.OnClickKeyboardListener =
        object : Keyboard.OnClickKeyboardListener {
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

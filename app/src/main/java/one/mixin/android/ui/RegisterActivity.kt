package one.mixin.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.ResponseError
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.databinding.ActivityRegisterBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toHex
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.PinCheckDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RegisterActivity::class.java))
        }
    }

    private lateinit var binding: ActivityRegisterBinding

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var assetRepository: AssetRepository

    @Inject
    lateinit var accountRepository: AccountRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            pin.setListener(
                object : PinView.OnPinListener {
                    override fun onUpdate(index: Int) {
                        if (index == pin.getCount()) {
                            verify(pin.code())
                        }
                    }
                },
            )
            gotItTv.setOnClickListener { finish() }
            keyboard.apply {
                initPinKeys(this@RegisterActivity)
                setOnClickKeyboardListener(mKeyboardListener)
                animate().translationY(0f).start()
            }
        }
    }

    private fun verify(pinCode: String) = lifecycleScope.launch(CoroutineExceptionHandler { _, e ->
        if (e is TipNetworkException) {
            handleFailure(e.error)
        } else if (e.isTipNodeException()) {
            binding.pinVa.displayedChild = PinCheckDialogFragment.POS_PIN
            binding.pin.error(e.getTipExceptionMsg(this@RegisterActivity, null))
        } else {
            Timber.e(e)
            binding.pin.clear()
            binding.pinVa.displayedChild = PinCheckDialogFragment.POS_PIN
        }
    }) {
        binding.apply {
            pinVa.displayedChild = PinCheckDialogFragment.POS_PB
            with(Dispatchers.IO) {
                val seed = tip.getOrRecoverTipPriv(this@RegisterActivity, pinCode).getOrThrow()
                Timber.e("seed $seed")
                val keyPair = newKeyPairFromSeed(seed)
                val registerRp = assetRepository.registerPublicKey(
                    registerRequest = RegisterRequest(
                        keyPair.publicKey.toHex(), Session.registerPublicKey(
                            Session.getAccountId()!!, seed
                        )
                    )
                )
                if (registerRp.isSuccess) {
                    with(Dispatchers.Main) {
                        MainActivity.show(this@RegisterActivity)
                        defaultSharedPreferences.putBoolean("RegisterActivity", true)
                        finish()
                    }
                } else {
                    with(Dispatchers.Main) {
                        handleFailure(registerRp.error!!)
                        pin.clear()
                        pinVa.displayedChild = PinCheckDialogFragment.POS_PIN
                    }
                }
            }
        }
    }

    private fun handleFailure(error: ResponseError) = lifecycleScope.launch {
        binding.apply {
            pin.clear()
            when (error.code) {
                ErrorHandler.PIN_INCORRECT -> {
                    val errorCount = accountRepository.errorCount()
                    pinVa.displayedChild = PinCheckDialogFragment.POS_PIN
                    pin.error(
                        resources.getQuantityString(
                            R.plurals.error_pin_incorrect_with_times,
                            errorCount,
                            errorCount,
                        ),
                    )
                }

                ErrorHandler.TOO_MANY_REQUEST -> {
                    pinVa.displayedChild = PinCheckDialogFragment.POS_TIP
                    tipVa.showNext()
                    val transY = root.height / 2 - topLl.translationY * 2
                    topLl.animate()?.translationY(transY)?.start()
                    keyboard.animate()?.translationY(keyboard.height.toFloat())?.start()
                }

                else -> {
                    pinVa.displayedChild = PinCheckDialogFragment.POS_PIN
                    pin.error(getMixinErrorStringByCode(error.code, error.description))
                }
            }
        }
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener =
        object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(position: Int, value: String) {
                tickVibrate()
                binding.apply {
                    if (position == 11) {
                        pin.delete()
                    } else {
                        pin.append(value)
                    }
                }
            }

            override fun onLongClick(position: Int, value: String) {
                clickVibrate()
                binding.apply {
                    if (position == 11) {
                        pin.clear()
                    } else {
                        pin.append(value)
                    }
                }
            }
        }
}
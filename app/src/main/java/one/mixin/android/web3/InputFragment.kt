package one.mixin.android.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.databinding.FragmentInputBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.shaking
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.LoadingProgressDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.Keyboard
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class InputFragment : BaseFragment(R.layout.fragment_input) {
    companion object {
        const val TAG = "InputFragment"
        const val ARGS_ADDRESS = "args_address"
        const val ARGS_TOKEN = "args_token"
        fun newInstance(address: String, web3Token: Web3Token) = InputFragment().apply {
            withArgs {
                putString(ARGS_ADDRESS, address)
                putParcelable(ARGS_TOKEN, web3Token)
            }
        }
    }

    private val binding by viewBinding(FragmentInputBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()
    private var isReverse: Boolean = false
    
    private val address by lazy {
        requireNotNull(requireArguments().getString(ARGS_ADDRESS))
    }
    private val token by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_TOKEN, Web3Token::class.java))
    }
    private val price: Float by lazy {
        token.symbol.toFloatOrNull() ?: 1f
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.apply {
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.setSubTitle(getString(R.string.Send_transfer), address.formatPublicKey())
                keyboard.tipTitleEnabled = false
                keyboard.disableNestedScrolling()
                keyboard.setOnClickKeyboardListener(
                    object : Keyboard.OnClickKeyboardListener {
                        override fun onKeyClick(
                            position: Int,
                            value: String,
                        ) {
                            context?.tickVibrate()
                            if (position == 11) {
                                v =
                                    if (v == "0") {
                                        "0"
                                    } else if (v.length == 1) {
                                        "0"
                                    } else {
                                        v.substring(0, v.length - 1)
                                    }
                            } else {
                                if (v == "0" && value != ".") {
                                    v = value
                                } else if (isReverse && isEightDecimal(v)) {
                                    // do noting
                                    return
                                } else if (!isReverse && isTwoDecimal(v)) {
                                    // do noting
                                    return
                                } else if (value == "." && v.contains(".")) {
                                    // do noting
                                    return
                                } else {
                                    v += value
                                }
                            }
                            updateUI()
                        }

                        override fun onLongClick(
                            position: Int,
                            value: String,
                        ) {
                            context?.clickVibrate()
                        }
                    },
                )
                keyboard.initPinKeys(
                    requireContext(),
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<<"),
                    force = true,
                    white = true,
                )
                continueVa.setOnClickListener {

                }
                switchIv.setOnClickListener {
                    isReverse = !isReverse
                    v = "0"
                    updateUI()
                }
                updateUI()
            }
        }
    }

    private fun isTwoDecimal(string: String): Boolean {
        return string.matches(Regex("\\d+\\.\\d{2}"))
    }

    private fun isEightDecimal(string: String): Boolean {
        return string.matches(Regex("\\d+\\.\\d{8}"))
    }

    private var v = "0"

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        if (viewDestroyed()) return
        binding.apply {
            val value =
                if (v.endsWith(".")) {
                    v.substring(0, v.length)
                } else {
                    v
                }
            if (isReverse) {
                val currentValue = value.toFloat() * price
                if (value == "0") {
                    primaryTv.text = "0 ${token.symbol}"
                    minorTv.text = "0 USD"
                } else {
                    primaryTv.text = "$value ${token.symbol}"
                    minorTv.text =
                        "≈ ${getNumberFormat(String.format("%.2f", currentValue))} USD"
                }
            } else {
                val currentValue = value.toFloat()
                if (value == "0") {
                    primaryTv.text = "0 USD"
                    minorTv.text = "0 ${token.symbol}"
                } else {
                    primaryTv.text = "${getNumberFormat(value)} USD"
                    minorTv.text =
                        "≈ ${BigDecimal((currentValue / price).toDouble()).numberFormat8()} ${token.symbol}"
                }
            }
        }
        updatePrimarySize()
    }

    private fun getNumberFormat(value: String): String {
        return value.numberFormat2().let {
            if (v.endsWith(".")) {
                "$it."
            } else if (v.endsWith(".00")) {
                "$it.00"
            } else if (v.endsWith(".0")) {
                "$it.0"
            } else {
                it
            }
        }
    }

    private fun updatePrimarySize() {
        if (viewDestroyed()) return
        binding.apply {
            val length = primaryTv.text.length
            val size =
                if (length <= 8) {
                    56f
                } else {
                    max(56f - 2 * (length - 4), 12f)
                }
            primaryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }
    }

}

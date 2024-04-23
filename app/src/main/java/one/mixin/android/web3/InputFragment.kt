package one.mixin.android.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.buildTransaction
import one.mixin.android.databinding.FragmentInputBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.textColor
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.Keyboard
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max

@AndroidEntryPoint
class InputFragment : BaseFragment(R.layout.fragment_input) {
    companion object {
        const val TAG = "InputFragment"
        const val ARGS_TO_ADDRESS = "args_to_address"
        const val ARGS_FROM_ADDRESS = "args_from_address"
        const val ARGS_TOKEN = "args_token"
        fun newInstance(fromAddress:String, toAddress: String, web3Token: Web3Token) = InputFragment().apply {
            withArgs {
                putString(ARGS_FROM_ADDRESS, fromAddress)
                putString(ARGS_TO_ADDRESS, toAddress)
                putParcelable(ARGS_TOKEN, web3Token)
            }
        }
    }

    private val binding by viewBinding(FragmentInputBinding::bind)

    private var isReverse: Boolean = false
    
    private val toAddress by lazy {
        requireNotNull(requireArguments().getString(ARGS_TO_ADDRESS))
    }
    private val fromAddress by lazy {
        requireNotNull(requireArguments().getString(ARGS_FROM_ADDRESS))
    }
    private val token by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_TOKEN, Web3Token::class.java))
    }
    private val price: BigDecimal by lazy {
        (token.price.toBigDecimalOrNull() ?: BigDecimal.ONE).multiply(Fiats.getRate().toBigDecimal())
    }
    private val currencyName by lazy {
        Fiats.getAccountCurrencyAppearance()
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
                titleView.setSubTitle(getString(R.string.Send_transfer), toAddress.formatPublicKey())
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
                                } else if (!isReverse && isEightDecimal(v)) {
                                    // do noting
                                    return
                                } else if (isReverse && isTwoDecimal(v)) {
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
                avatar.bg.loadImage(token.iconUrl, R.drawable.ic_avatar_place_holder)
                avatar.badge.loadImage(token.chainIconUrl, R.drawable.ic_avatar_place_holder)
                name.text = token.name
                balance.text = "${token.balance} ${token.symbol}"
                max.setOnClickListener {
                    v = if (isReverse) {
                        // Todo No price token and chain token gas
                        BigDecimal(token.balance).multiply(BigDecimal(token.price)).setScale(2, RoundingMode.DOWN).numberFormat2()
                    } else {
                        token.balance
                    }
                    updateUI()
                }
                keyboard.initPinKeys(
                    requireContext(),
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<<"),
                    force = true,
                    white = true,
                )
                continueVa.setOnClickListener {
                    val transaction = token.buildTransaction(
                        fromAddress, toAddress, if (isReverse) {
                            binding.minorTv.text.toString().split(" ")[1].replace(",", "")
                        } else {
                            v
                        }
                    )

                    showBrowserBottomSheetDialogFragment(
                        requireActivity(),
                        transaction,
                        token = token
                    )
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
                val currentValue = value.toFloat()
                if (value == "0") {
                    primaryTv.text = "0 $currencyName"
                    minorTv.text = "0 ${token.symbol}"
                } else {
                    primaryTv.text = "${getNumberFormat(value)} $currencyName"
                    minorTv.text =
                        "≈ ${(value.toBigDecimal().divide(price, 8, RoundingMode.UP)).numberFormat8()} ${token.symbol}"
                }
            } else {
                val currentValue = price.multiply(value.toBigDecimal())
                if (value == "0") {
                    primaryTv.text = "0 ${token.symbol}"
                    minorTv.text = "0 $currencyName"
                } else {
                    primaryTv.text = "$value ${token.symbol}"
                    minorTv.text =
                        "≈ ${getNumberFormat(String.format("%.2f", currentValue))} $currencyName"
                }
            }

            if (value == "0") {
                continueVa.isEnabled = false
                binding.continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
            } else {
                val v = if (isReverse) {
                    minorTv.text.toString().split(" ")[1].replace(",", "")
                } else {
                    value
                }
                if (BigDecimal(v) > BigDecimal(token.balance)) {
                    continueVa.isEnabled = false
                    binding.continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                } else {
                    continueVa.isEnabled = true
                    binding.continueTv.textColor = requireContext().getColor(R.color.white)
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

package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.databinding.FragmentSelectPaymentBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.round
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.LoadingProgressDialogFragment
import one.mixin.android.ui.wallet.PaymentFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.hideGooglePay
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Card
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class SelectPaymentFragment : BaseFragment(R.layout.fragment_select_payment) {
    companion object {
        const val TAG = "SelectPaymentFragment"
    }

    private val binding by viewBinding(FragmentSelectPaymentBinding::bind)

    private lateinit var asset: TokenItem
    private lateinit var currency: Currency

    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    private val amount by lazy {
        requireArguments().getLong(OrderConfirmFragment.ARGS_AMOUNT)
    }

    private val loading by lazy {
        LoadingProgressDialogFragment()
    }

    private fun showLoading() {
        loading.showNow(parentFragmentManager, LoadingProgressDialogFragment.TAG)
    }

    private fun dismissLoading() {
        if (loading.isAdded) {
            loading.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        asset =
            requireNotNull(
                requireArguments().getParcelableCompat(
                    TransactionsFragment.ARGS_ASSET,
                    TokenItem::class.java,
                ),
            )
        currency =
            requireNotNull(
                requireArguments().getParcelableCompat(
                    OrderConfirmFragment.ARGS_CURRENCY,
                    Currency::class.java,
                ),
            )
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.setSubTitle(getString(R.string.Select_Payment_Method), "")
            firstRl.round(8.dp)
            secondRl.round(8.dp)
            firstRl.isVisible = requireContext().isGooglePlayServicesAvailable() && !hideGooglePay
            firstRl.setOnClickListener {
                if (fiatMoneyViewModel.state.value.googlePayAvailable != true) {
                    // toast(R.string.Google_Pay_error)
                    // return@setOnClickListener
                }
                view.navigate(
                    R.id.action_wallet_payment_to_order_confirm,
                    requireArguments().apply {
                        putBoolean(OrderConfirmFragment.ARGS_GOOGLE_PAY, true)
                    },
                )
            }
            secondRl.setOnClickListener {
                openSelectCard()
            }
        }
    }

    private fun openSelectCard() {
        SelectCardBottomSheetDialogFragment.newInstance(requireArguments()).apply {
            addCallback = {
                showCheckoutPayment()
            }
            paymentCallback = { instrumentId, scheme, cardNumber ->
                this@SelectPaymentFragment.view?.navigate(
                    R.id.action_wallet_payment_to_order,
                    Bundle().apply {
                        putLong(OrderConfirmFragment.ARGS_AMOUNT, amount)
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                        putParcelable(OrderConfirmFragment.ARGS_CURRENCY, currency)
                        putString(OrderConfirmFragment.ARGS_INSTRUMENT_ID, instrumentId)
                        putString(OrderConfirmFragment.ARGS_SCHEME, scheme)
                        putString(OrderConfirmFragment.ARGS_LAST, cardNumber)
                    },
                )
            }
        }.show(parentFragmentManager, SelectCardBottomSheetDialogFragment.TAG)
    }

    private fun showCheckoutPayment() {
        navTo(
            PaymentFragment().apply {
                val paymentFragment = this
                onBack = {
                    dismissLoading()
                }
                onSuccess = { token, scheme, cardholderName ->
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                        .remove(this).commitNow()
                    lifecycleScope.launch {
                        requestRouteAPI(
                            invokeNetwork = {
                                fiatMoneyViewModel.createInstrument(RouteInstrumentRequest(token, cardholderName, Session.getAccount()?.phone))
                            },
                            endBlock = {
                                dismissLoading()
                            },
                            successBlock = { response ->
                                if (response.isSuccess) {
                                    val cardData = response.data
                                    if (cardData != null) {
                                        saveCards(cardData)
                                        toast(R.string.Save_success)
                                        openSelectCard()
                                    } else {
                                        toast(R.string.error_bad_data)
                                    }
                                } else {
                                    toast(response.errorDescription)
                                    parentFragmentManager.beginTransaction()
                                        .setCustomAnimations(
                                            0,
                                            R.anim.slide_out_right,
                                            R.anim.stay,
                                            0,
                                        )
                                        .remove(paymentFragment).commitNow()
                                }
                            },
                            requestSession = {
                                fiatMoneyViewModel.fetchSessionsSuspend(
                                    listOf(
                                        Constants.RouteConfig.ROUTE_BOT_USER_ID,
                                    ),
                                )
                            },
                        )
                    }
                }
                onLoading = {
                    showLoading()
                }
                onFailure = {
                    toast(it)
                    dismissLoading()
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                        .remove(paymentFragment).commitNow()
                }
            },
            PaymentFragment.TAG,
        )
    }

    private fun saveCards(card: Card) {
        lifecycleScope.launch {
            fiatMoneyViewModel.addCard(card)
        }
    }
}

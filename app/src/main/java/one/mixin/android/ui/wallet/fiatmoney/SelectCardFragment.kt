package one.mixin.android.ui.wallet.fiatmoney

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.databinding.ItemCardBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.PaymentFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem

@AndroidEntryPoint
class SelectCardFragment : BaseFragment(R.layout.fragment_select_card) {

    companion object {
        const val TAG = "SelectCardFragment"

        fun newInstance(bundle: Bundle) = SelectCardFragment().apply {
            arguments = bundle
        }
    }

    private val binding by viewBinding(FragmentSelectCardBinding::bind)

    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val asset = requireNotNull(
            requireArguments().getParcelableCompat(
                TransactionsFragment.ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        val currency = requireNotNull(
            requireArguments().getParcelableCompat(
                OrderConfirmFragment.ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
        val amount = requireArguments().getInt(OrderConfirmFragment.ARGS_AMOUNT)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            addVa.setOnClickListener {
                addVa.displayedChild = 1
                navTo(
                    PaymentFragment().apply {
                        onSuccess = { token, scheme ->
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                                .remove(this).commitNow()
                            lifecycleScope.launch {
                                handleMixinResponse(
                                    invokeNetwork = {
                                        walletViewModel.createSession(
                                            CreateSessionRequest(
                                                token,
                                                currency.name,
                                                scheme,
                                                Session.getAccountId()!!,
                                                "965e5c6e-434c-3fa9-b780-c50f43cd955c",
                                                amount,
                                            ),
                                        )
                                    },
                                    successBlock = { response ->
                                        if (response.isSuccess) {
                                            root.navigate(
                                                R.id.action_wallet_card_to_order,
                                                requireArguments().apply {
                                                    putString(
                                                        OrderConfirmFragment.ARGS_INSTRUMENT_ID,
                                                        response.data!!.instrumentId,
                                                    )
                                                    putString(
                                                        OrderConfirmFragment.ARGS_SCHEME,
                                                        scheme,
                                                    )
                                                },
                                            )
                                        } else {
                                            // Todo
                                            // showError(response.errorDescription)
                                        }
                                    },
                                )
                            }
                        }
                        onLoading = {
                        }
                        onFailure = {
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                                .remove(this).commitNow()
                            // Todo
                            // showError(it)
                        }
                    },
                    PaymentFragment.TAG,
                )
            }
            cardRv.adapter = CardAdapter { instrumentId, scheme ->
                root.navigate(
                    R.id.action_wallet_card_to_order,
                    requireArguments().apply {
                        putString(OrderConfirmFragment.ARGS_INSTRUMENT_ID, instrumentId)
                        putString(OrderConfirmFragment.ARGS_SCHEME, scheme)
                    },
                )
            }
        }
    }

    class CardAdapter(val callback: (String, String) -> Unit) : RecyclerView.Adapter<CardViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val binding =
                ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return 1
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            holder.bind()
            holder.itemView.setOnClickListener {
                // todo real data
                callback.invoke("src_gbk4fsgbflyujn5hkw6ij2dptm", "visa")
            }
        }
    }

    class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Todo real data
            binding.cardNumber.text = "Visa...4242"
        }
    }
}

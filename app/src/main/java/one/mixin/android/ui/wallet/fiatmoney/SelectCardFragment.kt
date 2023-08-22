package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Card
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.databinding.ItemCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.round
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.PaymentFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_CURRENCY
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_INSTRUMENT_ID
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_LAST
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_SCHEME
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import timber.log.Timber
import kotlin.random.Random

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

    private val asset by lazy {
        requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
    }
    private val currency by lazy {
        requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
    }
    private val amount by lazy {
        requireArguments().getInt(ARGS_AMOUNT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            addVa.setOnClickListener {
                addVa.displayedChild = 1
                navTo(
                    PaymentFragment().apply {
                        val paymentFragment = this
                        onBack = { addVa.displayedChild = 0 }
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
                                                asset.assetId,
                                                amount,
                                            ),
                                        )
                                    },
                                    endBlock = { addVa.displayedChild = 0 },
                                    successBlock = { response ->
                                        if (response.isSuccess) {
                                            val last4 = response.data?.last4
                                            val instrumentId = response.data?.instrumentId
                                            val cardScheme = response.data?.scheme
                                            if (last4 != null && instrumentId != null && cardScheme != null) {
                                                saveCards(
                                                    Card.newBuilder().setNumber(last4)
                                                        .setScheme(cardScheme)
                                                        .setInstrumentId(instrumentId).build(),
                                                )
                                            } else {
                                                toast(R.string.error_bad_data)
                                            }
                                        } else {
                                            // Todo
                                            toast(response.errorDescription)
                                            parentFragmentManager.beginTransaction()
                                                .setCustomAnimations(
                                                    0,
                                                    R.anim.slide_out_right,
                                                    R.anim.stay,
                                                    0,
                                                )
                                                .remove(paymentFragment).commitNow()
                                            view.navigate(
                                                R.id.action_wallet_card_to_payment,
                                                Bundle().apply {
                                                    putInt(ARGS_AMOUNT, amount)
                                                    putParcelable(ARGS_ASSET, asset)
                                                    putParcelable(ARGS_CURRENCY, currency)
                                                },
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        onLoading = {
                            addVa.displayedChild = 1
                        }
                        onFailure = {
                            toast(it)
                            addVa.displayedChild = 0
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                                .remove(paymentFragment).commitNow()
                            view.navigate(
                                R.id.action_wallet_card_to_payment,
                                Bundle().apply {
                                    putInt(ARGS_AMOUNT, amount)
                                    putParcelable(ARGS_ASSET, asset)
                                    putParcelable(ARGS_CURRENCY, currency)
                                },
                            )
                        }
                    },
                    PaymentFragment.TAG,
                )
            }
            ItemTouchHelper(
                ItemCallback(object : ItemCallback.ItemCallbackListener {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        lifecycleScope.launch {
                            walletViewModel.removeCard(viewHolder.absoluteAdapterPosition)
                        }
                    }
                }),
            ).apply { attachToRecyclerView(cardRv) }
            cardRv.adapter = cardAdapter
        }
        lifecycleScope.launch {
            try {
                initCards()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private val cardAdapter by lazy {
        CardAdapter { instrumentId, scheme, cardNumber ->
            this@SelectCardFragment.view?.navigate(
                R.id.action_wallet_card_to_order,
                Bundle().apply {
                    putInt(ARGS_AMOUNT, amount)
                    putParcelable(ARGS_ASSET, asset)
                    putParcelable(ARGS_CURRENCY, currency)
                    putString(ARGS_INSTRUMENT_ID, instrumentId)
                    putString(ARGS_SCHEME, scheme)
                    putString(ARGS_LAST, cardNumber)
                },
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun initCards() {
        walletViewModel.initSafeBox()
        walletViewModel.cards().observe(this@SelectCardFragment.viewLifecycleOwner) { safeBox ->
            if (safeBox.cardCount > 0) {
                binding.cardRv.visibility = View.VISIBLE
                binding.empty.visibility = View.GONE
            } else {
                binding.cardRv.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
            cardAdapter.data = safeBox.cardList
            cardAdapter.notifyDataSetChanged()
        }
    }

    private fun saveCards(card: Card) {
        lifecycleScope.launch {
            Timber.e("addCard")
            walletViewModel.addCard(card)
        }
    }

    class CardAdapter(val callback: (String, String, String) -> Unit) :
        RecyclerView.Adapter<CardViewHolder>() {

        var data: List<Card>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val binding =
                ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return data?.size ?: 0
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            val card = data?.get(position) ?: return
            holder.bind(card)
            holder.itemView.setOnClickListener {
                callback.invoke(card.instrumentId, card.scheme, card.number)
            }
        }
    }

    class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.round(8.dp)
        }

        @SuppressLint("SetTextI18n")
        fun bind(card: Card) {
            binding.cardNumber.text = "${card.scheme}...${card.number}"
            binding.logo.setImageResource(if (card.scheme == "visa") R.drawable.ic_visa else R.drawable.ic_mastercard)
        }
    }
}

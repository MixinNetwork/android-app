package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.annotations.SerializedName
import com.snappydb.SnappyDB
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.TEST_ASSET_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.databinding.ItemCardBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.PaymentFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_CURRENCY
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_INSTRUMENT_ID
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_SCHEME
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import timber.log.Timber

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

    private val snappyDB by lazy {
        SnappyDB.Builder(context).build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val asset = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        val currency = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
        val amount = requireArguments().getInt(ARGS_AMOUNT)
        initCards()
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            addVa.setOnClickListener {
                addVa.displayedChild = 1
                navTo(
                    PaymentFragment().apply {
                        val paymentFragment = this
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
                                                TEST_ASSET_ID,
                                                amount,
                                            ),
                                        )
                                    },
                                    successBlock = { response ->
                                        addVa.displayedChild = 0
                                        if (response.isSuccess) {
                                            val currency = response.data?.currency
                                            val instrumentId = response.data?.instrumentId
                                            val scheme = response.data?.scheme
                                            if (currency != null && instrumentId != null && scheme != null) {
                                                saveCards(Card(currency, scheme, instrumentId))
                                            } else {
                                                toast(R.string.error_bad_data)
                                            }
                                        } else {
                                            // Todo
                                            toast(response.errorDescription)
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
                                )
                            }
                        }
                        onLoading = {
                            addVa.displayedChild = 1
                        }
                        onFailure = {
                            // Todo
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

            cardRv.adapter = CardAdapter(cards) { instrumentId, scheme ->
                root.navigate(
                    R.id.action_wallet_card_to_order,
                    Bundle().apply {
                        putInt(ARGS_AMOUNT, amount)
                        putParcelable(ARGS_ASSET, asset)
                        putParcelable(ARGS_CURRENCY, currency)
                        putString(ARGS_INSTRUMENT_ID, instrumentId)
                        putString(ARGS_SCHEME, scheme)
                    },
                )
            }
        }
    }

    private fun initCards() {
        try {
            cards.clear()
            val local = snappyDB.getObjectArray("cards", Card::class.java)
            if (local.isNotEmpty()) {
                cards.addAll(local)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        if (cards.isEmpty()) {
            binding.cardRv.visibility = View.GONE
            binding.empty.visibility = View.VISIBLE
        } else {
            binding.cardRv.visibility = View.VISIBLE
            binding.empty.visibility = View.GONE
        }
    }

    private fun saveCards(card: Card) {
        cards.add(card)
        binding.cardRv.visibility = View.VISIBLE
        binding.empty.visibility = View.GONE
        (binding.cardRv.adapter as CardAdapter).apply {
            data = cards
            notifyDataSetChanged()
        }
        snappyDB.put("cards", cards.toTypedArray())
    }

    override fun onDetach() {
        super.onDetach()
        snappyDB.close()
    }

    private val cards: MutableList<Card> = mutableListOf()

    class CardAdapter(var data: MutableList<Card>, val callback: (String, String) -> Unit) : RecyclerView.Adapter<CardViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val binding =
                ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            val card = data[position]
            holder.bind(data[position])
            holder.itemView.setOnClickListener {
                callback.invoke(card.instrumentId, card.scheme)
            }
        }
    }

    class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(card: Card) {
            binding.cardNumber.text = "${card.scheme}...4242"
            binding.logo.setImageResource(if (card.scheme == "visa") R.drawable.ic_visa else R.drawable.ic_mastercard)
        }
    }

    class Card(
        val number: String,
        val scheme: String,
        @SerializedName("instrument_id")
        val instrumentId: String,
    ) {
        constructor() : this("", "", "")
    }
}

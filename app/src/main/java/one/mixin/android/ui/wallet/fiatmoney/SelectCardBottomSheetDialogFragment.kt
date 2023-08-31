package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.databinding.ItemCardBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.config
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.round
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Card
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

@AndroidEntryPoint
class SelectCardBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "SelectCardBottomSheetDialogFragment"

        fun newInstance(bundle: Bundle) = SelectCardBottomSheetDialogFragment().apply {
            arguments = bundle
        }
    }

    private val asset by lazy {
        requireNotNull(
            requireArguments().getParcelableCompat(
                TransactionsFragment.ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
    }
    private val currency by lazy {
        requireNotNull(
            requireArguments().getParcelableCompat(
                OrderConfirmFragment.ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
    }
    private val amount by lazy {
        requireArguments().getInt(OrderConfirmFragment.ARGS_AMOUNT)
    }

    var addCallback: (() -> Unit)? = null
    override fun onStart() {
        try {
            super.onStart()
        } catch (ignored: WindowManager.BadTokenException) {
        }
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    private val binding by viewBinding(FragmentSelectCardBinding::inflate)

    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.apply {
            title.rightIv.setOnClickListener {
                dismiss()
            }
            addVa.setOnClickListener {
                addCallback?.invoke()
                dismiss()
            }
        }
        binding.apply {
            ItemTouchHelper(
                ItemCallback(object : ItemCallback.ItemCallbackListener {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        lifecycleScope.launch {
                            val anchorView = cardRv
                            val card = cardAdapter.data?.get(viewHolder.absoluteAdapterPosition) ?: return@launch
                            fiatMoneyViewModel.removeCard(viewHolder.absoluteAdapterPosition)
                            snackbar = Snackbar.make(anchorView, getString(R.string.wallet_already_deleted, "${card.scheme.capitalize()}...${card.number}"), 3500)
                                .setAction(R.string.UNDO) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        saveCards(card)
                                    }
                                }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                                    (this.view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView)
                                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                                }.apply {
                                    snackbar?.config(anchorView.context)
                                }
                            snackbar?.show()
                        }
                    }
                }),
            ).apply { attachToRecyclerView(cardRv) }
            cardRv.adapter = cardAdapter
        }
        lifecycleScope.launch {
            initCards()
        }
    }

    private var snackbar: Snackbar? = null

    private val cardAdapter by lazy {
        CardAdapter { instrumentId, scheme, cardNumber ->
            paymentCallback?.invoke(instrumentId, scheme, cardNumber)
            dismiss()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun initCards() {
        fiatMoneyViewModel.initSafeBox()
        fiatMoneyViewModel.cards().collect { safeBox ->
            if (safeBox.cards.isNotEmpty()) {
                binding.cardRv.visibility = View.VISIBLE
                binding.empty.visibility = View.GONE
            } else {
                binding.cardRv.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
            cardAdapter.data = safeBox.cards
            cardAdapter.notifyDataSetChanged()
        }
    }

    private fun saveCards(card: Card) {
        lifecycleScope.launch {
            fiatMoneyViewModel.addCard(card)
        }
    }

    var paymentCallback: ((String, String, String) -> Unit)? = null

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

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(card: Card) {
            binding.cardNumber.text = "${card.scheme.capitalize()}...${card.number}"
            binding.logo.setImageResource(if (card.scheme.equals("visa", true)) R.drawable.ic_visa else R.drawable.ic_mastercard)
        }
    }

    private val mBottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                try {
                    dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    Timber.w(e)
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }
}

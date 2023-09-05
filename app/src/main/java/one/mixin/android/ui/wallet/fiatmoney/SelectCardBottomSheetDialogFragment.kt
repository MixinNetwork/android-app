package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.databinding.ItemCardBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.round
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Card
import one.mixin.android.widget.MixinBottomSheetDialog

@AndroidEntryPoint
class SelectCardBottomSheetDialogFragment : BottomSheetDialogFragment() {

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

    var addCallback: (() -> Unit)? = null

    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()
    override fun getTheme() = R.style.MixinBottomSheet

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MixinBottomSheetDialog(requireContext(), theme).apply {
            dismissWithAnimation = true
        }
    }

    private var _binding: FragmentSelectCardBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var contentView: View

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = FragmentSelectCardBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as BottomSheetBehavior<*>
        behavior.peekHeight = 380.dp
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setGravity(Gravity.BOTTOM)
        binding.apply {
            title.rightIv.setOnClickListener {
                dismiss()
            }
            title.titleTv.setTypeface(null, Typeface.BOLD)
            ItemTouchHelper(
                ItemCallback(object : ItemCallback.ItemCallbackListener {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        lifecycleScope.launch {
                            val anchorView = cardRv
                            val card = cardAdapter.data?.get(viewHolder.absoluteAdapterPosition) ?: return@launch
                            delete(card)
                        }
                    }
                }) { viewHolder ->
                    if (viewHolder.absoluteAdapterPosition >= (cardAdapter.data?.size ?: 0)) {
                        0
                    } else {
                        ItemTouchHelper.LEFT
                    }
                },
            ).apply { attachToRecyclerView(cardRv) }
            cardRv.adapter = cardAdapter
        }
        lifecycleScope.launch {
            initCards()
        }
    }

    private val cardAdapter by lazy {
        CardAdapter({
            addCallback?.invoke()
            dismiss()
        }, { instrumentId, scheme, cardNumber ->
            paymentCallback?.invoke(instrumentId, scheme, cardNumber)
            dismiss()
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun initCards() {
        fiatMoneyViewModel.cards().collect { safeBox ->
            cardAdapter.data = safeBox?.cards
            cardAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun delete(card: Card) {
        alertDialogBuilder()
            .setTitle(getString(R.string.card_delete_title, "${card.scheme.capitalize()}...${card.number}"))
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                cardAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                lifecycleScope.launch {
                    cardAdapter.deletingIds.add(card.instrumentId)
                    cardAdapter.notifyDataSetChanged()
                    handleMixinResponse(
                        invokeNetwork = {
                            fiatMoneyViewModel.deleteInstruments(card.instrumentId)
                        },
                        endBlock = {
                            cardAdapter.deletingIds.remove(card.instrumentId)
                        },
                        successBlock = { response ->
                            if (response.isSuccess) {
                                val index = cardAdapter.data?.indexOf(card) ?: return@handleMixinResponse
                                fiatMoneyViewModel.removeCard(index)
                            }
                        },
                    )
                }
                dialog.dismiss()
            }
            .show()
    }

    var paymentCallback: ((String, String, String) -> Unit)? = null

    class CardAdapter(val addCallback: () -> Unit, val callback: (String, String, String) -> Unit) :
        RecyclerView.Adapter<CardViewHolder>() {

        var data: List<Card>? = null
        var deletingIds: ArraySet<String> = arraySetOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val binding =
                ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return (data?.size ?: 0) + 1
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            if (position == data?.size) {
                holder.bind()
                holder.itemView.setOnClickListener {
                    addCallback.invoke()
                }
            } else {
                val card = data?.get(position) ?: return
                holder.bind(card, deletingIds.contains(card.instrumentId))
                holder.itemView.setOnClickListener {
                    callback.invoke(card.instrumentId, card.scheme, card.number)
                }
            }
        }
    }

    class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.round(8.dp)
        }

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(card: Card? = null, deleting: Boolean = false) {
            if (card == null) {
                binding.cardNumber.setText(R.string.Add_debit_or_credit_card)
                binding.logo.setImageResource(R.drawable.ic_select_add)
                binding.va.displayedChild = 0
            } else {
                binding.cardNumber.text = "${card.scheme.capitalize()}...${card.number}"
                binding.logo.setImageResource(if (card.scheme.equals("visa", true)) R.drawable.ic_visa else R.drawable.ic_mastercard)
                binding.va.displayedChild = if (deleting) 1 else 0
            }
        }
    }
}

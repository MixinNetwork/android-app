package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.databinding.FragmentSelectCardBottomSheetBinding
import one.mixin.android.databinding.ItemCardBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.PaymentFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import timber.log.Timber
@AndroidEntryPoint
class SelectCardBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SelectCardBottomSheetDialogFragment"

        fun newInstance(bundle: Bundle) = SelectCardBottomSheetDialogFragment().apply {
            arguments = bundle
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val binding by viewBinding(FragmentSelectCardBottomSheetBinding::inflate)

    private lateinit var contentView: View

    private val walletViewModel by viewModels<WalletViewModel>()

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

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        contentView = binding.root
        dialog.setContentView(contentView)
        val behavior = ((contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.peekHeight = requireContext().dpToPx(300f)
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, requireContext().dpToPx(300f))
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
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
            closeIv.setOnClickListener {
                dismiss()
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
                                            callback?.invoke(response.data?.instrumentId, response.data?.scheme)
                                            dismiss()
                                        } else {
                                            dismiss()
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
                            // showError(it)
                        }
                    },
                    PaymentFragment.TAG,
                )
            }
            cardRv.adapter = CardAdapter{instrumentId, scheme->
                callback?.invoke(instrumentId,scheme)
                dismiss()
            }
        }
    }

    class CardAdapter(val callback:(String,String)->Unit) : RecyclerView.Adapter<CardViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return 1
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            holder.bind()
            holder.itemView.setOnClickListener{
                callback.invoke("src_gbk4fsgbflyujn5hkw6ij2dptm", "visa")
            }
        }
    }

    var callback: ((String?, String?) -> Unit)? = null

    class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Todo real data
            binding.cardNumber.text = "Visa...4242"
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

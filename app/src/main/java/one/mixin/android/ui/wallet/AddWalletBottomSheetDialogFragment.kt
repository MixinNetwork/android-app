package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddWalletBottomSheetBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

class AddWalletBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    enum class Action {
        IMPORT_MNEMONIC,
        IMPORT_PRIVATE_KEY,
        ADD_WATCH_ADDRESS,
        CREATE_WALLET,
    }

    companion object {
        const val TAG = "AddWalletBottomSheetDialogFragment"
        private const val PREF_FREE_TRANSFER_CARD_DISMISSED = "pref_free_transfer_card_dismissed"
        fun newInstance() = AddWalletBottomSheetDialogFragment()
    }

    private val binding by lazy {
        FragmentAddWalletBottomSheetBinding.inflate(LayoutInflater.from(context))
    }

    var callback: ((Action) -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        binding.apply {
            val openFreeTransferDoc = {
                requireContext().openUrl(getString(R.string.url_cross_wallet_transaction_free))
            }
            val preferences = requireContext().defaultSharedPreferences

            rightIv.setOnClickListener { dismiss() }
            addWatchAddress.setOnClickListener {
                callback?.invoke(Action.ADD_WATCH_ADDRESS)
                dismiss()
            }
            importMnemonicPhrase.setOnClickListener {
                callback?.invoke(Action.IMPORT_MNEMONIC)
                dismiss()
            }
            importPrivateKey.setOnClickListener {
                callback?.invoke(Action.IMPORT_PRIVATE_KEY)
                dismiss()
            }
            createNewWallet.setOnClickListener {
                callback?.invoke(Action.CREATE_WALLET)
                dismiss()
            }
            freeTransferCard.visibility = if (preferences.getBoolean(PREF_FREE_TRANSFER_CARD_DISMISSED, false)) {
                View.GONE
            } else {
                View.VISIBLE
            }
            freeTransferCloseIv.setOnClickListener {
                preferences.putBoolean(PREF_FREE_TRANSFER_CARD_DISMISSED, true)
                freeTransferCard.visibility = View.GONE
            }
            bindOptionalView("free_transfer_card") {
                openFreeTransferDoc()
            }
            bindOptionalView("free_transfer_learn_more") {
                openFreeTransferDoc()
            }
        }
    }

    private fun bindOptionalView(
        idName: String,
        onClick: () -> Unit,
    ) {
        val id = binding.root.resources.getIdentifier(idName, "id", requireContext().packageName)
        if (id == 0) return
        binding.root.findViewById<View>(id)?.setOnClickListener { onClick() }
    }
}

package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import one.mixin.android.databinding.FragmentAddWalletBottomSheetBinding
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
        }
    }
}


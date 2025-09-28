package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class ImportKeyBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ImportKeyBottomSheetDialogFragment"
        private const val ARGS_POPUP_TYPE = "args_popup_type"
        private const val ARGS_WALLET_ID = "args_wallet_id"
        private const val ARGS_CHAIN_ID = "args_chain_id"

        fun newInstance(
            popupType: PopupType,
            walletId: String,
            chainId: String?
        ) =
            ImportKeyBottomSheetDialogFragment().withArgs {
                putString(ARGS_POPUP_TYPE, popupType::class.java.simpleName)
                putString(ARGS_WALLET_ID, walletId)
                putString(ARGS_CHAIN_ID, chainId)
            }
    }

    private val popupType by lazy {
        val typeName = requireArguments().getString(ARGS_POPUP_TYPE)
        when (typeName) {
            PopupType.ImportPrivateKey::class.java.simpleName -> PopupType.ImportPrivateKey
            PopupType.ImportMnemonicPhrase::class.java.simpleName -> PopupType.ImportMnemonicPhrase
            else -> throw IllegalArgumentException("Unknown PopupType")
        }
    }

    private val walletId by lazy {
        requireArguments().getString(ARGS_WALLET_ID) ?: ""
    }

    private val chainId by lazy {
        requireArguments().getString(ARGS_CHAIN_ID)
    }

    private var behavior: BottomSheetBehavior<*>? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            roundTopOrBottom(8.dp.toFloat(), top = true, bottom = false)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            fitsSystemWindows = true
            setContent {
                MixinAppTheme {
                    when (popupType) {
                        is PopupType.ImportPrivateKey -> {
                            ImportKeyPage(
                                R.drawable.bg_import_private_key,
                                R.string.import_private_key,
                                R.string.Import_Private_Key_Desc,
                                action = {
                                    WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY, walletId = walletId, chainId = chainId)
                                    dismissAllowingStateLoss()
                                },
                                dismiss = {
                                    dismissAllowingStateLoss()
                                },
                                learnMoreAction = {
                                    context.openUrl(getString(R.string.import_mnemonic_phrase_url))
                                }
                            )
                        }
                        is PopupType.ImportMnemonicPhrase -> {
                            ImportKeyPage(
                                R.drawable.bg_import_mnemonic,
                                R.string.import_mnemonic_phrase,
                                R.string.Import_Mnemonic_Phrase_Desc,
                                action = {
                                    WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.RE_IMPORT_MNEMONIC, walletId = walletId, chainId = chainId)
                                    dismissAllowingStateLoss()
                                },
                                dismiss = {
                                    dismissAllowingStateLoss()
                                },
                                learnMoreAction = {
                                    context.openUrl(getString(R.string.import_private_key_url))
                                }
                            )
                        }
                    }
                }
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                behavior?.peekHeight = requireContext().screenHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }

    sealed class PopupType {
        object ImportPrivateKey : PopupType()
        object ImportMnemonicPhrase : PopupType()
    }
}

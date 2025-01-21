package one.mixin.android.ui.conversation.link

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class CollectionBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "AuthBottomSheetDialogFragment"
        const val ARGS_COLLECTION_HASH = "args_collection_hash"
        const val ARGS_TRACE_ID = "args_trace_id"
        const val ARGS_RECEIVER_ID = "args_receiver_id"
        const val ARGS_MEMO = "args_memo"

        fun newInstance(collectionHash: String, traceId: String, receiverId: String, memo: String?) = CollectionBottomSheetDialogFragment().withArgs {
            putString(ARGS_COLLECTION_HASH, collectionHash)
            putString(ARGS_TRACE_ID, traceId)
            putString(ARGS_RECEIVER_ID, receiverId)
            putString(ARGS_MEMO, memo)
        }
    }

    private val collectionHash by lazy {
        requireNotNull(requireArguments().getString(ARGS_COLLECTION_HASH))
    }

    private val traceId by lazy {
        requireNotNull(requireArguments().getString(ARGS_TRACE_ID))
    }

    private val receiverId by lazy {
        requireNotNull(requireArguments().getString(ARGS_RECEIVER_ID))
    }

    private val memo by lazy {
        requireArguments().getString(ARGS_MEMO)
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

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private val bottomSheetViewModel by viewModels<BottomSheetViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CollectionPage(collectionHash, { inscription ->
                    lifecycleScope.launch {
                        val u = bottomSheetViewModel.refreshUser(receiverId)
                        val token = checkTokenByCollectionHash(collectionHash, inscription.inscriptionHash)
                        val inscriptionCollection = checkInscriptionCollection(collectionHash)
                        if (inscriptionCollection == null) {
                            toast(R.string.collectible_not_found)
                            dismiss()
                            return@launch
                        }
                        if (u == null) {
                            toast(R.string.User_not_found)
                            dismiss()
                            return@launch
                        }
                        val output = bottomSheetViewModel.findUnspentOutputByHash(inscription.inscriptionHash)
                        if (output == null) {
                            toast(R.string.collectible_not_found)
                            dismiss()
                            return@launch
                        }
                        val nftBiometricItem =
                            NftBiometricItem(
                                asset = token,
                                traceId = traceId,
                                amount = output.amount,
                                memo = memo,
                                state = PaymentStatus.pending.name,
                                receivers = listOf(u),
                                reference = null,
                                inscriptionItem = inscription,
                                inscriptionCollection = inscriptionCollection,
                                releaseAmount = null,
                            )

                        TransferBottomSheetDialogFragment.newInstance(nftBiometricItem).showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
                        dismiss()
                    }
                }, {
                    dismiss()
                })
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                behavior?.peekHeight = requireContext().screenHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

    private suspend fun checkTokenByCollectionHash(collectionHash: String, instantiationHash: String): TokenItem? {
        var asset = bottomSheetViewModel.findAssetItemByCollectionHash(collectionHash)
        if (asset == null) {
            asset = bottomSheetViewModel.refreshAssetByInscription(collectionHash, instantiationHash)
        }
        if (asset != null && asset.assetId != asset.chainId && bottomSheetViewModel.findAssetItemById(asset.chainId) == null) {
            bottomSheetViewModel.refreshAsset(asset.chainId)
        }
        return bottomSheetViewModel.findAssetItemByCollectionHash(collectionHash)
    }

    private suspend fun checkInscriptionCollection(hash: String): InscriptionCollection? {
        var inscriptionCollection = bottomSheetViewModel.findInscriptionCollectionByHash(hash)
        if (inscriptionCollection == null) {
            inscriptionCollection = bottomSheetViewModel.refreshInscriptionCollection(hash)
            return inscriptionCollection
        } else {
            return inscriptionCollection
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
}
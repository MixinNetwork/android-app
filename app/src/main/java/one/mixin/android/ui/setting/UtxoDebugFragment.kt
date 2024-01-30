package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.databinding.FragmentDatabaseDebugBinding
import one.mixin.android.databinding.FragmentUtxoDebugBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.extension.alert
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.navigate
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.WarningBottomSheetDialogFragment
import one.mixin.android.ui.wallet.fiatmoney.OrderStatusFragment
import one.mixin.android.util.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class UtxoDebugFragment : BaseFragment(R.layout.fragment_utxo_debug) {
    companion object {
        const val TAG = "UtxoDebugFragment"

        fun newInstance() = UtxoDebugFragment()
    }

    private val binding by viewBinding(FragmentUtxoDebugBinding::bind)
    @Inject
    lateinit var tip: Tip
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener { }
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.publicKey.setOnClickListener {
            reveal()
        }
        binding.delete.setOnClickListener {
            // todo
        }
    }

    private fun reveal(){
        PinInputBottomSheetDialogFragment.newInstance("Input PIN to Reveal").setOnPinComplete { pin ->
            lifecycleScope.launch {
                val tipPriv = tip.getOrRecoverTipPriv(MixinApplication.appContext, pin).getOrThrow()
                val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(MixinApplication.appContext), pin, tipPriv)
                val keyPair = newKeyPairFromSeed(spendKey)
                val pkHex = keyPair.publicKey.toHex()
                requireContext().alert(pkHex, "Reveal Public Key")
                    .setPositiveButton(android.R.string.copy) { dialog, _ ->
                        requireContext().getClipboardManager().setPrimaryClip(
                            ClipData.newPlainText(
                                null,
                                pkHex
                            ),
                        )
                        dialog.dismiss()
                    }
                    .show()
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }
}

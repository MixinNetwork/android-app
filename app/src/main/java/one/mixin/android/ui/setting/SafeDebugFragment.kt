package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Tip.ALIAS_SPEND_SALT
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.databinding.FragmentSafeDebugBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.tip.Tip
import one.mixin.android.tip.deleteKeyByAlias
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class SafeDebugFragment : BaseFragment(R.layout.fragment_safe_debug) {
    companion object {
        const val TAG = "UtxoDebugFragment"

        fun newInstance() = SafeDebugFragment()
    }

    private val binding by viewBinding(FragmentSafeDebugBinding::bind)

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
            deleteKeyByAlias(ALIAS_SPEND_SALT)
            toast("Delete Salt")
        }
    }

    private fun reveal() {
        PinInputBottomSheetDialogFragment.newInstance("Input PIN to Reveal").setOnPinComplete { pin ->
            lifecycleScope.launch {
                val dialog =
                    indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                        setCancelable(false)
                    }
                dialog.show()
                val tipPriv = tip.getOrRecoverTipPriv(MixinApplication.appContext, pin).getOrThrow()
                val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(MixinApplication.appContext), pin, tipPriv)
                val keyPair = newKeyPairFromSeed(spendKey)
                val pkHex = keyPair.publicKey.toHex()
                dialog.dismiss()
                requireContext().alert(pkHex, "Reveal Public Key")
                    .setPositiveButton(android.R.string.copy) { dialog, _ ->
                        requireContext().getClipboardManager().setPrimaryClip(
                            ClipData.newPlainText(
                                null,
                                pkHex,
                            ),
                        )
                        dialog.dismiss()
                    }
                    .show()
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }
}

package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.navTo
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.NewMnemonicPhraseBackupPinPage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel
import javax.inject.Inject

@AndroidEntryPoint
class CheckPinFragment : BaseFragment(R.layout.fragment_compose) {

    @Inject
    lateinit var tip: Tip

    private val viewModel by activityViewModels<FetchWalletViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NewMnemonicPhraseBackupPinPage(
                    tip = tip,
                    pop = {
                        activity?.finish()
                    },
                    next = { pin ->
                        lifecycleScope.launch {
                            val result = tip.getOrRecoverTipPriv(requireContext(), pin)
                            if (result.isSuccess) {
                                val tipPriv = result.getOrThrow()
                                val spendKey = tip.getSpendPrivFromEncryptedSalt(
                                    tip.getMnemonicFromEncryptedPreferences(requireContext()),
                                    tip.getEncryptedSalt(requireContext()),
                                    pin,
                                    tipPriv
                                )
                                viewModel.setSpendKey(spendKey)
                                navTo(AddWalletFragment.newInstance(), AddWalletFragment.TAG)
                                requireActivity().supportFragmentManager
                                    .beginTransaction()
                                    .remove(this@CheckPinFragment)
                                    .commit()
                            } else {
                                requireActivity().finish()
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val TAG = "CheckPinFragment"
        fun newInstance() = CheckPinFragment()
    }
}

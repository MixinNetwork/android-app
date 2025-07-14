package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.navTo
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.ui.page.MnemonicPhraseBackupPinPage
import javax.inject.Inject

@AndroidEntryPoint
class CheckPinFragment : BaseFragment(R.layout.fragment_compose) {

    @Inject
    lateinit var tip: Tip

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MnemonicPhraseBackupPinPage(
                    tip = tip,
                    pop = {
                        activity?.finish()
                    },
                    next = { pin ->
                        navTo(AddWalletFragment.newInstance(), AddWalletFragment.TAG)
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .remove(this@CheckPinFragment)
                            .commit()
                    }
                )
            }
        }
    }

    companion object {
        fun newInstance(): CheckPinFragment {
            return CheckPinFragment()
        }
    }
}

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
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.components.CreateWalletNoticePage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel

@AndroidEntryPoint
class CreateWalletNoticeFragment : BaseFragment(R.layout.fragment_compose) {

    private val viewModel by activityViewModels<FetchWalletViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CreateWalletNoticePage(
                    pop = {
                        parentFragmentManager.popBackStack()
                    },
                    createWallet = {
                        lifecycleScope.launch {
                            navTo(
                                ImportingWalletFragment.newInstance(),
                                ImportingWalletFragment.TAG
                            )
                            viewModel.createClassicWallet()
                            requireActivity().supportFragmentManager
                                .beginTransaction()
                                .remove(this@CreateWalletNoticeFragment)
                                .commit()
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val TAG = "CreateWalletNoticeFragment"

        fun newInstance(): CreateWalletNoticeFragment {
            return CreateWalletNoticeFragment()
        }
    }
}

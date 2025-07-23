package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.components.FetchWalletState
import one.mixin.android.ui.wallet.components.ImportWalletDetailPage
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel

@AndroidEntryPoint
class ImportWalletDetailFragment : BaseFragment(R.layout.fragment_compose) {

    private val viewModel by activityViewModels<FetchWalletViewModel>()
    private var mode: WalletSecurityActivity.Mode = WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY
    private var scannedText by mutableStateOf("")

    private val scanLauncher =
        registerForActivityResult(CaptureActivity.CaptureContract()) { intent ->
            intent?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)?.let {
                scannedText = it
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeOrdinal =
                it.getInt(ARG_MODE, WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY.ordinal)
            mode = WalletSecurityActivity.Mode.values()[modeOrdinal]
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state == FetchWalletState.IMPORTING) {
                    navTo(
                        ImportingWalletFragment.newInstance(),
                        ImportingWalletFragment.TAG
                    )
                    if (isAdded) {
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .remove(this@ImportWalletDetailFragment)
                            .commit()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ImportWalletDetailPage(
                    mode = mode,
                    pop = {
                        activity?.finish()
                    },
                    onConfirmClick = { chainId, key ->
                        viewModel.importWallet(key, chainId, mode)
                    },
                    onScan = {
                        scanLauncher.launch(
                            Pair(
                                CaptureActivity.ARGS_FOR_SCAN_RESULT,
                                true
                            )
                        )
                    },
                    contentText = scannedText
                )
            }
        }
    }

    companion object {
        const val TAG = "ImportWalletDetailFragment"
        private const val ARG_MODE = "arg_mode"

        fun newInstance(mode: WalletSecurityActivity.Mode): ImportWalletDetailFragment {
            val fragment = ImportWalletDetailFragment()
            val args = Bundle()
            args.putInt(ARG_MODE, mode.ordinal)
            fragment.arguments = args
            return fragment
        }
    }
}

package one.mixin.android.ui.wallet

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.components.AddWalletPage
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AddWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "AddWalletFragment"
        fun newInstance() = AddWalletFragment()
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    // state for scanned mnemonic list
    private var scannedMnemonicList by mutableStateOf<List<String>>(emptyList())
    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getScanResult = registerForActivityResult(
            CaptureActivity.CaptureContract()
        ) { intent ->
            intent?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)
                ?.split(" ")
                ?.takeIf { it.isNotEmpty() }
                ?.let { scannedMnemonicList = it }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().finish()
        }
        binding.compose.setContent {
            // pass current scanned list into page, handle scan trigger
            AddWalletPage(
                mnemonicList = scannedMnemonicList,
                onComplete = { mnemonicList -> navigateToFetchWallet(mnemonicList) },
                onScan = { getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true)) }
            )
        }
    }

    private fun navigateToFetchWallet(mnemonicList: List<String>) {
        val mnemonic = mnemonicList.joinToString(separator = " ")
        navTo(FetchingWalletFragment.newInstance(mnemonic), FetchingWalletFragment.TAG)
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }
}

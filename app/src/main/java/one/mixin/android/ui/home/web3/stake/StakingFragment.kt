package one.mixin.android.ui.home.web3.stake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class StakingFragment : BaseFragment() {
    companion object {
        const val TAG = "StakingFragment"
        private const val ARGS_STAKE_ACCOUNTS = "args_stake_accounts"

        fun newInstance(stakeAccounts: ArrayList<StakeAccount>) = StakingFragment().withArgs {
            putParcelableArrayList(ARGS_STAKE_ACCOUNTS, stakeAccounts)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val stakeAccounts = requireNotNull(requireArguments().getParcelableArrayListCompat(ARGS_STAKE_ACCOUNTS, StakeAccount::class.java)) { "required stakeAccounts cannot be null" }
        return ComposeView(inflater.context).apply {
            setContent {
                StakingPage(
                    stakeAccounts = stakeAccounts,
                    onClick = { sa ->

                    }
                ) {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }
}
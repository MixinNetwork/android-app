package one.mixin.android.ui.home.web3.stake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.api.response.web3.StakeAccountActivation
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.stake.StakeFragment.Companion.ARGS_BALANCE

@AndroidEntryPoint
class StakingFragment : BaseFragment() {
    companion object {
        const val TAG = "StakingFragment"
        private const val ARGS_STAKE_ACCOUNTS = "args_stake_accounts"

        fun newInstance(
            stakeAccounts: ArrayList<StakeAccount>,
            balance: String,
        ) = StakingFragment().withArgs {
            putParcelableArrayList(ARGS_STAKE_ACCOUNTS, stakeAccounts)
            putString(ARGS_BALANCE, balance)
        }
    }
    private val stakeViewModel by viewModels<StakeViewModel>()

    private val activationMap: MutableMap<String, StakeAccountActivation?> = SnapshotStateMap()
    private val validatorMap: MutableMap<String, Validator?> = SnapshotStateMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val stakeAccounts = requireNotNull(requireArguments().getParcelableArrayListCompat(ARGS_STAKE_ACCOUNTS, StakeAccount::class.java)) { "required stakeAccounts cannot be null" }
        val balance = requireNotNull(requireArguments().getString(ARGS_BALANCE))
        if (stakeAccounts.isNotEmpty()) {
            lifecycleScope.launch {
                loadStakeActivations(stakeAccounts)
                loadValidators(stakeAccounts)
            }
        }
        return ComposeView(inflater.context).apply {
            setContent {
                StakingPage(
                    stakeAccounts = stakeAccounts,
                    activations = activationMap,
                    validators = validatorMap,
                    onClick = { sa, v, a ->
                        if (v == null || a == null) {
                            toast(R.string.error_bad_data)
                            return@StakingPage
                        }
                        navTo(UnstakeFragment.newInstance(v, sa, a), UnstakeFragment.TAG)
                    },
                    onAdd = {
                        navTo(ValidatorsFragment.newInstance().apply {
                            setOnSelect { v ->
                                navTo(StakeFragment.newInstance(v, balance), StakeFragment.TAG)
                            }
                        }, ValidatorsFragment.TAG)
                    }
                ) {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }

    private suspend fun loadStakeActivations(stakeAccounts: List<StakeAccount>) {
        val activations = stakeViewModel.getStakeAccountActivations(stakeAccounts.map { it.pubkey })
        if (activations.isNullOrEmpty()) return

        stakeAccounts.forEach { sa ->
            activationMap[sa.pubkey] = activations.find { a -> a.pubkey == sa.pubkey }
        }
    }

    private suspend fun loadValidators(stakeAccounts: List<StakeAccount>) {
        val validators = stakeViewModel.getStakeValidators(stakeAccounts.map { it.account.data.parsed.info.stake.delegation.voter })
        if (validators.isNullOrEmpty()) return

        stakeAccounts.forEach { sa ->
            val votePubkey = sa.account.data.parsed.info.stake.delegation.voter
            validatorMap[votePubkey] = validators.find { v -> v.votePubkey == votePubkey }
        }
    }
}
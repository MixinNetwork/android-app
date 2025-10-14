package one.mixin.android.ui.address

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.UserAddressView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.address.components.FetchAddressContent
import one.mixin.android.ui.address.components.FetchAddressState
import one.mixin.android.ui.address.viewmodel.FetchAddressViewModel
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.InputFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import timber.log.Timber

@AndroidEntryPoint
class FetchUserAddressFragment : BaseFragment(R.layout.fragment_compose) {

    companion object {
        const val TAG = "FetchUserAddressFragment"
        const val ARGS_WEB3_TOKEN = "args_web3_token"
        const val ARGS_TO_USER = "args_to_user"
        const val ARGS_FROM_ADDRESS = "args_from_address"
        const val ARGS_CHAIN_TOKEN = "args_chain_token"

        fun newInstance(
            web3Token: Web3TokenItem,
            toUser: User,
            fromAddress: String,
            chainToken: Web3TokenItem
        ): FetchUserAddressFragment {
            val fragment = FetchUserAddressFragment()
            val args = Bundle()
            args.putParcelable(ARGS_WEB3_TOKEN, web3Token)
            args.putParcelable(ARGS_TO_USER, toUser)
            args.putString(ARGS_FROM_ADDRESS, fromAddress)
            args.putParcelable(ARGS_CHAIN_TOKEN, chainToken)
            fragment.arguments = args
            return fragment
        }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val viewModel by viewModels<FetchAddressViewModel>()

    private val web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
    }

    private val toUser by lazy {
        requireArguments().getParcelableCompat(ARGS_TO_USER, User::class.java)
    }

    private val fromAddress by lazy {
        requireArguments().getString(ARGS_FROM_ADDRESS)
    }

    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_CHAIN_TOKEN, Web3TokenItem::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleView.leftIb.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightAnimator.setOnClickListener {
            context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
        }

        binding.compose.setContent {
            MixinAppTheme {
                val state by viewModel.state.collectAsState()
                val userAddress by viewModel.userAddress.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                FetchAddressContent(
                    state = state,
                    errorMessage = errorMessage,
                    onRetry = {
                        retryFetchAddress()
                    },
                    onCancel = {
                        parentFragmentManager.popBackStack()
                    }
                )

                // Handle success state with navigation
                if (state == FetchAddressState.SUCCESS) {
                    userAddress?.let { address ->
                        parentFragmentManager.popBackStack()
                        findNavController().navigate(R.id.action_fetch_user_address_to_input, Bundle().apply {
                            putParcelable(
                                InputFragment.ARGS_WEB3_TOKEN,
                                web3Token
                            )
                            putString(InputFragment.ARGS_FROM_ADDRESS, fromAddress)
                            putString(InputFragment.ARGS_TO_ADDRESS, address.destination)
                            putParcelable(InputFragment.ARGS_WEB3_CHAIN_TOKEN, chainToken)
                            putParcelable(InputFragment.ARGS_TO_USER, toUser)
                        })

                    }
                }
            }
        }

        // Start fetching address when fragment is created
        startFetchingAddress()
    }

    private fun startFetchingAddress() {
        if (web3Token == null || toUser == null) {
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.fetchUserAddress(toUser!!.userId, web3Token!!.chainId)
    }

    private fun retryFetchAddress() {
        if (web3Token == null || toUser == null) {
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.retry(toUser!!.userId, web3Token!!.chainId)
    }

}

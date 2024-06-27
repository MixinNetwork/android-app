package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.Account.PREF_WEB3_BOT_PK
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.RouteConfig.WEB3_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.findChainToken
import one.mixin.android.api.response.isSolToken
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.databinding.FragmentChainBinding
import one.mixin.android.databinding.ViewWalletWeb3BottomBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.event.SolanaRefreshEvent
import one.mixin.android.event.TokenEvent
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.WCUnlockEvent
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.stake.StakeFragment
import one.mixin.android.ui.home.web3.swap.SwapFragment
import one.mixin.android.ui.tip.wc.WalletConnectFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment.Companion.TYPE_SOLANA
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId
import one.mixin.android.web3.ChainType
import one.mixin.android.web3.dapp.SearchDappFragment
import one.mixin.android.web3.details.Web3TransactionDetailsFragment
import one.mixin.android.web3.details.Web3TransactionFragment
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.web3.receive.Web3ReceiveSelectionFragment
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.send.InputAddressFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SpacesItemDecoration

@AndroidEntryPoint
class SolanaFragment : BaseFragment() {
    companion object {
        const val TAG = "SolanaFragment"
    }

    private var _binding: FragmentChainBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var _bottomBinding: ViewWalletWeb3BottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }

    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val adapter by lazy {
        Web3WalletAdapter(SOLANA_CHAIN_ID).apply {
            setOnWeb3Click { token ->
                address?.let { address ->
                    navTo(Web3TransactionDetailsFragment.newInstance(address, ChainType.solana.name, token, token.findChainToken(tokens), tokens), Web3TransactionDetailsFragment.TAG)
                }
            }
            setOnClickAction { id ->
                when (id) {
                    R.id.send -> {
                        sendCallback(tokens)
                    }

                    R.id.receive -> {
                        navTo(Web3ReceiveSelectionFragment(), Web3ReceiveSelectionFragment.TAG)
                    }

                    R.id.swap -> {
                        navTo(SwapFragment.newInstance(tokens), SwapFragment.TAG)
                    }

                    R.id.browser -> {
                        navTo(SearchDappFragment(), SearchDappFragment.TAG)
                    }

                    R.id.more -> {
                        val builder = BottomSheet.Builder(requireActivity())
                        _bottomBinding = ViewWalletWeb3BottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_web3_bottom, null))
                        builder.setCustomView(bottomBinding.root)
                        val bottomSheet = builder.create()
                        bottomBinding.apply {
                            title.setText(R.string.Solana_Account)
                            addressTv.text = this@SolanaFragment.address?.formatPublicKey()
                            stakeSolTv.setOnClickListener {
                                this@SolanaFragment.navTo(StakeFragment.newInstance(
                                    Validator("Mixin Validator", "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png", "6.9%", "9%", "412,456.1234", "J2nUHEAgZFRyuJbFjdqPrAa9gyWDuc7hErtDQHPhsYRp"),
                                    tokens.find { t -> t.isSolToken() }?.balance ?: "0",
                                ), StakeFragment.TAG)
                                bottomSheet.dismiss()
                            }
                            copy.setOnClickListener {
                                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                                toast(R.string.copied_to_clipboard)
                                bottomSheet.dismiss()
                            }
                            connectedTv.setOnClickListener {
                                this@SolanaFragment.navTo(WalletConnectFragment.newInstance(), WalletConnectFragment.TAG)
                                bottomSheet.dismiss()
                            }
                            cancel.setOnClickListener { bottomSheet.dismiss() }
                        }

                        bottomSheet.show()
                    }
                }
            }
        }
    }

    private val sendCallback = fun(list: List<Web3Token>) {
        Web3TokenListBottomSheetDialogFragment.newInstance(ArrayList(list)).apply {
            setOnClickListener { token ->
                address?.let { add ->
                    navTo(InputAddressFragment.newInstance(add, token, token.findChainToken(tokens)), InputAddressFragment.TAG)
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChainBinding.inflate(inflater, container, false)
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            this@SolanaFragment.address = address
            binding.apply {
                adapter.address = address
                walletRv.adapter = adapter
                walletRv.addItemDecoration(SpacesItemDecoration(4.dp, true))
            }
        }
        RxBus.listen(WCUnlockEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe { _ ->
                updateUI()
            }
        updateUI()
        RxBus.listen(SolanaRefreshEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe {
                address?.let { address ->
                    lifecycleScope.launch {
                        refreshAccount(address)
                    }
                }
            }
        RxBus.listen(TokenEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { e ->
                val token = web3ViewModel.web3Token("solana", e.tokenId)
                if (token != null) {
                    val fragments = mutableListOf<Fragment>()
                    parentFragmentManager.findFragmentByTag(Web3TransactionDetailsFragment.TAG)?.let {
                        fragments.add(it)
                    }
                    parentFragmentManager.findFragmentByTag(Web3TransactionFragment.TAG)?.let {
                        fragments.add(it)
                    }
                    address?.let { address ->
                        navTo(Web3TransactionDetailsFragment.newInstance(address, ChainType.solana.name, token, token.findChainToken(tokens)), Web3TransactionDetailsFragment.TAG)
                    }
                    fragments.forEach {
                        parentFragmentManager.beginTransaction().remove(it).commit()
                    }
                }
            }
        return binding.root
    }

    private var address: String? = null

    fun updateUI() {
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            if (isAdded) {
                this@SolanaFragment.address = address
                if (address.isBlank()) {
                    adapter.setContent(getString(R.string.web3_account_network, getString(R.string.Solana)), getString(R.string.access_dapps_defi_projects), R.drawable.ic_solana) {
                        WalletUnlockBottomSheetDialogFragment.getInstance(TYPE_SOLANA).showIfNotShowing(parentFragmentManager, WalletUnlockBottomSheetDialogFragment.TAG)
                    }
                } else {
                    adapter.address = address
                    refreshAccount(address)
                }
            }
        }
    }

    private suspend fun checkPublicKey() {
        if (MixinApplication.appContext.defaultSharedPreferences.getString(PREF_WEB3_BOT_PK, null) != null) return
        val key =
            web3ViewModel.findBotPublicKey(
                generateConversationId(
                    WEB3_BOT_USER_ID,
                    Session.getAccountId()!!,
                ),
                WEB3_BOT_USER_ID,
            )
        if (!key.isNullOrEmpty()) {
            MixinApplication.appContext.defaultSharedPreferences.putString(PREF_WEB3_BOT_PK, key)
        } else {
            val sessionResponse =
                web3ViewModel.fetchSessionsSuspend(listOf(WEB3_BOT_USER_ID))
            if (sessionResponse.isSuccess) {
                val sessionData = requireNotNull(sessionResponse.data)[0]
                web3ViewModel.saveSession(
                    ParticipantSession(
                        generateConversationId(
                            sessionData.userId,
                            Session.getAccountId()!!,
                        ),
                        sessionData.userId,
                        sessionData.sessionId,
                        publicKey = sessionData.publicKey,
                    ),
                )
                MixinApplication.appContext.defaultSharedPreferences.putString(PREF_WEB3_BOT_PK, sessionData.publicKey)
            } else {
                throw MixinResponseException(
                    sessionResponse.errorCode,
                    sessionResponse.errorDescription,
                )
            }
        }
    }

    private var dialog: AlertDialog? = null

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun refreshAccount(address: String) {
        if (!isAdded) return
        if (adapter.isEmpty()) {
            binding.progress.isVisible = true
            binding.empty.isVisible = false
        }
        try {
            checkPublicKey()
        } catch (e: Exception) {
            handleError(address, e.message ?: getString(R.string.Unknown))
            binding.progress.isVisible = false
            return
        }
        val account =
            try {
                val response = web3ViewModel.web3Account("solana", address)
                if (!isAdded) return
                if (response.errorCode == ErrorHandler.OLD_VERSION) {
                    dialog?.dismiss()
                    dialog =
                        alertDialogBuilder()
                            .setTitle(R.string.Update_Mixin)
                            .setMessage(getString(R.string.update_mixin_description, requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName))
                            .setNegativeButton(R.string.Later) { dialog, _ ->
                                dialog.dismiss()
                            }.setPositiveButton(R.string.Update) { dialog, _ ->
                                requireContext().openMarket(parentFragmentManager, lifecycleScope)
                                dialog.dismiss()
                            }.create()
                    dialog?.show()
                    throw MixinResponseException(
                        response.errorCode,
                        getString(R.string.update_mixin_description, requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName),
                    )
                } else if (response.error != null) {
                    handleError(address, response.errorDescription)
                    binding.progress.isVisible = false
                    return
                }
                response
            } catch (e: Exception) {
                if (!isAdded) return
                handleError(address, e.message ?: getString(R.string.Unknown))
                binding.progress.isVisible = false
                return
            }
        account.data?.let { data ->
            adapter.account = data
            tokens = data.tokens
        }
        handleEmpty()
        binding.progress.isVisible = false
    }

    private var tokens = emptyList<Web3Token>()

    private fun handleError(
        address: String,
        err: String,
    ) {
        binding.apply {
            if (!adapter.isEmpty()) return
            empty.isVisible = true
            titleTv.text = err
            receiveTv.setText(R.string.Retry)
            receiveTv.setOnClickListener {
                lifecycleScope.launch {
                    refreshAccount(address = address)
                }
            }
        }
    }

    private fun handleEmpty() {
        binding.apply {
            if (!adapter.isEmpty()) return
            empty.isVisible = true
            titleTv.setText(R.string.No_asset)
            receiveTv.setText(R.string.Receive)
            receiveTv.setOnClickListener {
                navTo(Web3AddressFragment(), Web3AddressFragment.TAG)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateUI()
        }
    }
}

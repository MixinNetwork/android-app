package one.mixin.android.ui.wallet

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.PrivacyPreference.getPrefPinInterval
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.databinding.FragmentWalletBinding
import one.mixin.android.databinding.ViewClassicWalletBottomBinding
import one.mixin.android.databinding.ViewImportWalletBottomBinding
import one.mixin.android.databinding.ViewPrivacyWalletBottomBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putString
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.internal.ethTransactionSerializer
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.components.AssetDashboardScreen
import one.mixin.android.ui.wallet.components.AssetDistributionViewModel
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.WalletDestinationTypeAdapter
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.ui.web.reloadWebViewInClips
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.generateConversationId
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.hypot

@AndroidEntryPoint
class WalletFragment : BaseFragment(R.layout.fragment_wallet) {
    companion object {
        const val TAG = "WalletFragment"

        fun newInstance(): WalletFragment = WalletFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private var selectedWalletDestination: WalletDestination? = null

    private fun loadSelectedWalletDestination(): WalletDestination {
        val walletPref = defaultSharedPreferences.getString(
            Constants.Account.PREF_HAS_USED_WALLET, null
        )

        walletViewModel.setHasUsedWallet(walletPref != null)

        val initialWalletDestination = walletPref?.let { pref ->
            try {
                gson.fromJson(pref, WalletDestination::class.java)
            } catch (e: Exception) {
                WalletDestination.Privacy
            }
        } ?: WalletDestination.Privacy

        lifecycleScope.launch {
            val finalWalletDestination = when (val dest = initialWalletDestination) {
                is WalletDestination.Classic -> {
                    val wallet = walletViewModel.findWalletById(dest.walletId)
                    if (wallet != null) dest else WalletDestination.Privacy
                }
                is WalletDestination.Import -> {
                    val wallet = walletViewModel.findWalletById(dest.walletId)
                    if (wallet != null) {
                        if (dest.category != wallet.category) {
                            WalletDestination.Import(dest.walletId, wallet.category)
                        } else {
                            dest
                        }
                    } else {
                        WalletDestination.Privacy
                    }
                }
                is WalletDestination.Privacy -> dest
            }

            if (initialWalletDestination != finalWalletDestination || walletPref?.trim()?.startsWith("{") == false) {
                saveSelectedWalletDestination(finalWalletDestination)
            }

            selectedWalletDestination = finalWalletDestination
            updateUi(finalWalletDestination)
        }

        return initialWalletDestination
    }

    private fun saveSelectedWalletDestination(destination: WalletDestination) {
        defaultSharedPreferences.putString(Constants.Account.PREF_HAS_USED_WALLET, gson.toJson(destination))
        walletViewModel.setHasUsedWallet(true)
    }

    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(WalletDestination::class.java, WalletDestinationTypeAdapter())
        .create()

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val walletViewModel by viewModels<WalletViewModel>()
    private val assetDistributionViewModel by viewModels<AssetDistributionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val classicWalletFragment by lazy { ClassicWalletFragment.newInstance() }
    private val privacyWalletFragment by lazy { PrivacyWalletFragment.newInstance() }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            walletViewModel.hasUsedWallet.observe(viewLifecycleOwner) { hasUsed ->
                badge.isVisible = !hasUsed
            }

            selectedWalletDestination = loadSelectedWalletDestination()
            selectedWalletDestination?.let { updateUi(it) }

            moreIb.setOnClickListener {
                when (selectedWalletDestination) {
                    is WalletDestination.Privacy -> showPrivacyBottom()
                    is WalletDestination.Import -> showImportBottom()
                    is WalletDestination.Classic -> showClassicBottom()
                    null -> showPrivacyBottom() // Default
                }
            }
            scanIb.setOnClickListener {
                RxPermissions(requireActivity()).request(Manifest.permission.CAMERA)
                    .autoDispose(stopScope).subscribe { granted ->
                        if (granted) {
                            (requireActivity() as? MainActivity)?.showCapture(true)
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
            }
            searchIb.setOnClickListener {
                when (selectedWalletDestination) {
                    is WalletDestination.Privacy -> {
                        WalletActivity.show(requireActivity(), WalletActivity.Destination.Search)
                    }
                    is WalletDestination.Import -> {
                        val dest = selectedWalletDestination
                        if (dest is WalletDestination.Import) {
                            WalletActivity.show(
                                requireActivity(),
                                WalletActivity.Destination.SearchWeb3(dest.walletId)
                            )
                        }
                    }
                    is WalletDestination.Classic -> {
                        val dest = selectedWalletDestination
                        if (dest is WalletDestination.Classic) {
                            WalletActivity.show(
                                requireActivity(),
                                WalletActivity.Destination.SearchWeb3(dest.walletId)
                            )
                        }
                    }
                    null -> WalletActivity.show(
                        requireActivity(),
                        WalletActivity.Destination.Search
                    ) // Default
                }
            }
            compose.setContent {
                AssetDashboardScreen(
                    onWalletCardClick = ::handleWalletCardClick,
                    onAddWalletClick = ::handleAddWalletClick
                )
            }

            titleRl.setOnClickListener {
                if (compose.isVisible.not()) {
                    compose.visibility = VISIBLE
                    assetDistributionViewModel.loadWallets()
                    val centerX = titleTv.x.toInt() + titleTv.width / 2
                    val centerY = titleTv.y.toInt() + titleTv.height / 2
                    val startRadius = 0
                    val endRadius = hypot(
                        binding.root.width.toDouble(),
                        binding.root.height.toDouble()
                    ).toInt()

                    val anim = ViewAnimationUtils.createCircularReveal(
                        compose,
                        centerX,
                        centerY,
                        startRadius.toFloat(),
                        endRadius.toFloat()
                    )
                    anim.duration = 300

                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                        }
                    })

                    anim.start()
                }
            }
        }

        walletViewModel.hasAssetsWithValue().observe(viewLifecycleOwner) {
            migrateEnable = it
        }
        checkPin()
    }

    private fun updateUi(destination: WalletDestination) {
        when (destination) {
            is WalletDestination.Privacy -> {
                requireActivity().replaceFragment(
                    privacyWalletFragment,
                    R.id.wallet_container,
                    PrivacyWalletFragment.TAG
                )
                binding.titleTv.setText(R.string.Privacy_Wallet)
                binding.tailIcon.setImageResource(R.drawable.ic_wallet_privacy)
                binding.tailIcon.isVisible = true
            }
            is WalletDestination.Classic -> {
                classicWalletFragment.walletId = destination.walletId
                requireActivity().replaceFragment(
                    classicWalletFragment,
                    R.id.wallet_container,
                    ClassicWalletFragment.TAG
                )
                binding.titleTv.setText(R.string.Common_Wallet)
                binding.tailIcon.isVisible = false
            }
            is WalletDestination.Import -> {
                classicWalletFragment.walletId = destination.walletId
                requireActivity().replaceFragment(
                    classicWalletFragment,
                    R.id.wallet_container,
                    ClassicWalletFragment.TAG
                )
                lifecycleScope.launch {
                    walletViewModel.findWalletById(destination.walletId)?.let { wallet ->
                        binding.tailIcon.isVisible = wallet.hasLocalPrivateKey.not()
                        binding.tailIcon.setImageResource(R.drawable.ic_wallet_watch)
                        binding.titleTv.text = if (!wallet.hasLocalPrivateKey) {
                            getString(R.string.watch, walletViewModel.getAddresses(wallet.id).joinToString { it.destination }.formatPublicKey(limit = 15, suffixLen = 4, prefixLen = 6))
                        } else {
                            wallet.name.ifBlank { getString(R.string.Common_Wallet) }
                        }
                    } ?: run {
                        binding.titleTv.setCompoundDrawables(null, null ,null , null)
                        binding.titleTv.setText(R.string.Common_Wallet)
                        binding.tailIcon.isVisible = false
                    }
                }

            }
        }
    }

    private var migrateEnable = false

    private fun handleAddWalletClick() {
        if (!Session.saltExported() && Session.isAnonymous()) {
            BackupMnemonicPhraseWarningBottomSheetDialogFragment.newInstance()
                .apply {
                    val dialog = AddWalletBottomSheetDialogFragment.newInstance()
                    dialog.callback = {
                        startActivity(Intent(requireContext(), WalletSecurityActivity::class.java).apply {
                            putExtra(WalletSecurityActivity.EXTRA_MODE, WalletSecurityActivity.Mode.IMPORT.ordinal)
                        })
                    }
                    dialog.show(parentFragmentManager, AddWalletBottomSheetDialogFragment.TAG)
                }
                .show(parentFragmentManager, BackupMnemonicPhraseWarningBottomSheetDialogFragment.TAG)
        } else {
            val dialog = AddWalletBottomSheetDialogFragment.newInstance()
            dialog.callback = {
                startActivity(Intent(requireContext(), WalletSecurityActivity::class.java).apply {
                    putExtra(WalletSecurityActivity.EXTRA_MODE, WalletSecurityActivity.Mode.IMPORT.ordinal)
                })
            }
            dialog.show(parentFragmentManager, AddWalletBottomSheetDialogFragment.TAG)
        }
    }

    private fun handleWalletCardClick(destination: WalletDestination) {
        selectedWalletDestination = destination
        saveSelectedWalletDestination(destination)
        if (destination is WalletDestination.Classic || destination is WalletDestination.Import) {
            val walletId = if (destination is WalletDestination.Classic) {
                destination.walletId
            } else {
                (destination as WalletDestination.Import).walletId
            }
            lifecycleScope.launch {
                val wallet = walletViewModel.findWalletById(walletId)
                if (wallet == null || (CryptoWalletHelper.hasPrivateKey(requireActivity(), walletId).not() && wallet.category == WalletCategory.IMPORTED_MNEMONIC.value)) {
                    return@launch
                }
                JsSigner.setWallet(walletId, wallet.category) { queryWalletId ->
                    runBlocking { walletViewModel.getAddresses(queryWalletId) }
                }
                reloadWebViewInClips()
                PropertyHelper.updateKeyValue(Constants.Account.SELECTED_WEB3_WALLET_ID, walletId)
            }
        }
        updateUi(destination)
        closeMenu()
    }

    override fun onResume() {
        super.onResume()
        if (classicWalletFragment.isVisible) classicWalletFragment.update()
    }

    private fun closeMenu() {
        val centerX = binding.titleTv.x.toInt() + binding.titleTv.width / 2
        val centerY = binding.titleTv.y.toInt() + binding.titleTv.height / 2
        val endRadius = hypot(
            binding.root.width.toDouble(),
            binding.root.height.toDouble()
        ).toInt()

        val closeAnim = ViewAnimationUtils.createCircularReveal(
            binding.compose,
            centerX,
            centerY,
            endRadius.toFloat(),
            0f
        )
        closeAnim.duration = 300
        closeAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.compose.isVisible = false
            }
        })
        closeAnim.start()
    }

    override fun onBackPressed(): Boolean {
        return if (binding.compose.isVisible) {
            closeMenu()
            true
        } else {
            false
        }
    }

    private var _importBottomBinding: ViewImportWalletBottomBinding? = null
    private val importBottomBinding get() = requireNotNull(_importBottomBinding)
    @SuppressLint("InflateParams")
    private fun showImportBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        if (_importBottomBinding == null) {
            _importBottomBinding = ViewImportWalletBottomBinding.bind(
                View.inflate(
                    ContextThemeWrapper(
                        requireActivity(),
                        R.style.Custom
                    ), R.layout.view_import_wallet_bottom, null
                )
            )
        }
        builder.setCustomView(importBottomBinding.root)
        val bottomSheet = builder.create()
        importBottomBinding.title.text = binding.titleTv.text.toString()

        val dest = selectedWalletDestination
        if (dest is WalletDestination.Import) {
            lifecycleScope.launch {
                val wallet = walletViewModel.findWalletById(dest.walletId)
                val hasPrivateKey = wallet?.hasLocalPrivateKey == true
                importBottomBinding.privateKey.isVisible = hasPrivateKey
                importBottomBinding.mnemonicPhrase.isVisible = hasPrivateKey
                importBottomBinding.rename.isVisible = hasPrivateKey
            }
            importBottomBinding.privateKey.setOnClickListener {
                ChainSelectionBottomSheetDialogFragment.newInstance(dest.walletId).apply {
                    callback = { chainItem ->
                        WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY, chainItem.chainId)
                    }
                }.show(parentFragmentManager, ChainSelectionBottomSheetDialogFragment.TAG)
                bottomSheet.dismiss()
            }
        }
        importBottomBinding.hide.setOnClickListener {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Import) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.Web3Hidden(dest.walletId))
            }
            bottomSheet.dismiss()
        }
        importBottomBinding.transactionsTv.setOnClickListener {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Import) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(dest.walletId))
            }
            bottomSheet.dismiss()
        }

        importBottomBinding.mnemonicPhrase.setOnClickListener {
            WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.VIEW_MNEMONIC)
            bottomSheet.dismiss()
        }
        importBottomBinding.delete.setOnClickListener {
            VerifyBottomSheetDialogFragment.newInstance(
                getString(R.string.remove_wallet_pin_hint),
                true,
                true,
                subtitle = getString(R.string.remove_wallet_warning),
            ).apply {
                disableToast = true
            }.setOnPinSuccess { _ ->
                this.lifecycleScope.launch {
                    val dest = selectedWalletDestination
                    if (dest is WalletDestination.Import) {
                        walletViewModel.deleteWallet(dest.walletId)
                        selectedWalletDestination = WalletDestination.Privacy
                        saveSelectedWalletDestination(WalletDestination.Privacy)
                        updateUi(WalletDestination.Privacy)
                    }
                }
            }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
            bottomSheet.dismiss()
        }
        importBottomBinding.rename.setOnClickListener {
            editDialog {
                titleText = this@WalletFragment.getString(R.string.Rename_Wallet)
                editText = binding.titleTv.text.toString()
                maxTextCount = 32
                allowEmpty = false
                rightAction = { newName ->
                    this@WalletFragment.lifecycleScope.launch {
                        val dest = selectedWalletDestination
                        if (dest is WalletDestination.Import) {
                            walletViewModel.renameWallet(dest.walletId, newName)
                            updateUi(dest)
                        }
                    }
                }
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private var _privacyBottomBinding: ViewPrivacyWalletBottomBinding? = null
    private val privacyBottomBinding get() = requireNotNull(_privacyBottomBinding)

    @SuppressLint("InflateParams")
    private fun showPrivacyBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        if (_privacyBottomBinding == null) {
            _privacyBottomBinding = ViewPrivacyWalletBottomBinding.bind(
                View.inflate(
                    ContextThemeWrapper(
                        requireActivity(),
                        R.style.Custom
                    ), R.layout.view_privacy_wallet_bottom, null
                )
            )
        }
        builder.setCustomView(privacyBottomBinding.root)
        val bottomSheet = builder.create()
        privacyBottomBinding.migrate.isVisible = migrateEnable
        privacyBottomBinding.hide.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.Hidden)
            bottomSheet.dismiss()
        }
        privacyBottomBinding.transactionsTv.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllTransactions)
            bottomSheet.dismiss()
        }
        privacyBottomBinding.migrate.setOnClickListener {
            lifecycleScope.launch click@{
                val bot = walletViewModel.findBondBotUrl() ?: return@click
                WebActivity.show(requireContext(), url = bot.homeUri, generateConversationId(bot.appId, Session.getAccountId()!!), app = bot)
            }
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private var _classicBottomBinding: ViewClassicWalletBottomBinding? = null
    private val classicBottomBinding get() = requireNotNull(_classicBottomBinding)

    @SuppressLint("InflateParams")
    private fun showClassicBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        if (_classicBottomBinding == null) {
            _classicBottomBinding = ViewClassicWalletBottomBinding.bind(
                View.inflate(
                    ContextThemeWrapper(
                        requireActivity(),
                        R.style.Custom
                    ), R.layout.view_classic_wallet_bottom, null
                )
            )
        }
        builder.setCustomView(classicBottomBinding.root)
        val bottomSheet = builder.create()
        classicBottomBinding.hide.setOnClickListener {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Classic) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.Web3Hidden(dest.walletId))
            }
            bottomSheet.dismiss()
        }
        classicBottomBinding.transactionsTv.setOnClickListener {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Classic) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(dest.walletId))
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _privacyBottomBinding = null
        _classicBottomBinding = null
    }

    private fun checkPin() {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(Constants.Account.PREF_PIN_CHECK, 0)
        var interval = getPrefPinInterval(requireContext(), 0)
        val account = Session.getAccount()
        if (account != null && account.hasPin && last == 0L) {
            interval = Constants.INTERVAL_24_HOURS
            putPrefPinInterval(requireContext(), Constants.INTERVAL_24_HOURS)
        }
        if (cur - last > interval) {
            val pinCheckDialog =
                PinCheckDialogFragment.newInstance().apply {
                    supportsS({
                        setDialogCallback { showed ->
                            if (this@WalletFragment.viewDestroyed()) return@setDialogCallback

                            binding.root.setRenderEffect(
                                if (showed) {
                                    RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.MIRROR)
                                } else {
                                    null
                                },
                            )
                        }
                    })
                }
            pinCheckDialog.show(parentFragmentManager, PinCheckDialogFragment.TAG)
        }
    }

}

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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putString
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshSingleWalletJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.components.AssetDashboardScreen
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
        private const val ARG_WALLET_DESTINATION = "wallet_destination"

        @Volatile
        private var instance: WalletFragment? = null

        fun newInstance(walletDestination: WalletDestination? = null): WalletFragment =
            instance ?: synchronized(this) {
                instance ?: WalletFragment().apply {
                    arguments = Bundle().apply {
                        walletDestination?.let { destination ->
                            putString(ARG_WALLET_DESTINATION, GsonHelper.customGson.toJson(destination))
                        }
                    }
                }.also { instance = it }
            }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private var selectedWalletDestination: WalletDestination? = null
        set(value) {
            if (value != field && value != null) {
                field = value
                when(value) {
                    is WalletDestination.Classic -> {
                        classicWalletFragment.walletId = value.walletId
                    }
                    is WalletDestination.Import -> {
                        classicWalletFragment.walletId = value.walletId
                    }
                    is WalletDestination.Watch -> {
                        classicWalletFragment.walletId = value.walletId
                    }
                    is WalletDestination.Privacy -> {
                        // No action needed for Privacy wallet
                    }
                }
                updateUi(value)
                saveSelectedWalletDestination(value)
            }
            Timber.e("Selected wallet destination: $value")
        }

    private fun saveSelectedWalletDestination(destination: WalletDestination) {
        defaultSharedPreferences.putString(Constants.Account.PREF_USED_WALLET, GsonHelper.customGson.toJson(destination))
    }

    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(WalletDestination::class.java, WalletDestinationTypeAdapter())
        .create()

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val walletViewModel by viewModels<WalletViewModel>()

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
        Timber.e("onViewCreated called in WalletFragment")

        val initialWalletDestination = arguments?.getString(ARG_WALLET_DESTINATION)?.let { json ->
            try {
                GsonHelper.customGson.fromJson(json, WalletDestination::class.java)
            } catch (_: Exception) {
                null
            }
        }

        if (!classicWalletFragment.isAdded) {
            childFragmentManager.beginTransaction()
                .add(R.id.wallet_container, classicWalletFragment, ClassicWalletFragment.TAG)
                .hide(classicWalletFragment)
                .commit()
        }

        if (!privacyWalletFragment.isAdded) {
            childFragmentManager.beginTransaction()
                .add(R.id.wallet_container, privacyWalletFragment, PrivacyWalletFragment.TAG)
                .hide(privacyWalletFragment)
                .commit()
        }

        selectedWalletDestination = initialWalletDestination ?: WalletDestination.Privacy

        binding.apply {
            badge.isVisible = defaultSharedPreferences.getBoolean(Constants.Account.PREF_HAS_USED_WALLET_LIST, true)

            moreIb.setOnClickListener {
                when (selectedWalletDestination) {
                    is WalletDestination.Privacy -> showPrivacyBottom()
                    is WalletDestination.Import -> showImportBottom()
                    is WalletDestination.Watch -> showImportBottom()
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
                    is WalletDestination.Watch -> {
                        val dest = selectedWalletDestination
                        if (dest is WalletDestination.Watch) {
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
                badge.isVisible = false
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_HAS_USED_WALLET_LIST, false)
                if (compose.isVisible.not()) {
                    compose.visibility = VISIBLE
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
        Timber.e("updateUi called with destination: $destination")
        when (destination) {
            is WalletDestination.Privacy -> {
                childFragmentManager.beginTransaction()
                    .hide(classicWalletFragment)
                    .show(privacyWalletFragment)
                    .commit()
                binding.titleTv.setText(R.string.Privacy_Wallet)
                binding.tailIcon.setImageResource(R.drawable.ic_wallet_privacy)
                binding.tailIcon.isVisible = true
            }
            is WalletDestination.Classic -> {
                childFragmentManager.beginTransaction()
                    .hide(privacyWalletFragment)
                    .show(classicWalletFragment)
                    .commit()
                binding.titleTv.setText(R.string.Common_Wallet)
                binding.tailIcon.isVisible = false
            }
            is WalletDestination.Watch -> {
                childFragmentManager.beginTransaction()
                    .hide(privacyWalletFragment)
                    .show(classicWalletFragment)
                    .commit()
                binding.titleTv.setText(R.string.Watch_Wallet)
                binding.tailIcon.isVisible = false
                lifecycleScope.launch {
                    walletViewModel.findWalletById(destination.walletId)?.let { wallet ->
                        binding.tailIcon.setImageResource(R.drawable.ic_wallet_watch)
                        binding.tailIcon.isVisible = wallet.hasLocalPrivateKey.not()
                        binding.titleTv.text = wallet.name.ifBlank { getString(R.string.Watch_Wallet) }
                    } ?: run {
                        binding.titleTv.setText(R.string.Watch_Wallet)
                        binding.tailIcon.isVisible = false
                    }
                }
            }
            is WalletDestination.Import -> {
                childFragmentManager.beginTransaction()
                    .hide(privacyWalletFragment)
                    .show(classicWalletFragment)
                    .commit()
                binding.titleTv.setText(R.string.Common_Wallet)
                binding.tailIcon.isVisible = false
                lifecycleScope.launch {
                    walletViewModel.findWalletById(destination.walletId)?.let { wallet ->
                        binding.tailIcon.isVisible = wallet.hasLocalPrivateKey.not()
                        if (wallet.hasLocalPrivateKey.not()) {
                            binding.tailIcon.setImageResource(R.drawable.ic_wallet_watch)
                        }
                        binding.titleTv.text = wallet.name.ifBlank { getString(R.string.Common_Wallet) }
                    } ?: run {
                        binding.titleTv.setText(R.string.Common_Wallet)
                        binding.tailIcon.isVisible = false
                    }
                }
            }
        }
    }

    private var migrateEnable = false

    private fun handleAddWalletClick() {
        val callback: (AddWalletBottomSheetDialogFragment.Action) -> Unit = { action ->
            val intent = Intent(requireContext(), WalletSecurityActivity::class.java)
            val mode = when (action) {
                AddWalletBottomSheetDialogFragment.Action.IMPORT_MNEMONIC -> WalletSecurityActivity.Mode.IMPORT_MNEMONIC
                AddWalletBottomSheetDialogFragment.Action.IMPORT_PRIVATE_KEY -> WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY
                AddWalletBottomSheetDialogFragment.Action.ADD_WATCH_ADDRESS -> WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS
            }
            intent.putExtra(WalletSecurityActivity.EXTRA_MODE, mode.ordinal)
            startActivity(intent)
        }

        if (!Session.saltExported() && Session.isAnonymous()) {
            BackupMnemonicPhraseWarningBottomSheetDialogFragment.newInstance()
                .apply {
                    val dialog = AddWalletBottomSheetDialogFragment.newInstance()
                    dialog.callback = callback
                    dialog.show(parentFragmentManager, AddWalletBottomSheetDialogFragment.TAG)
                }
                .show(parentFragmentManager, BackupMnemonicPhraseWarningBottomSheetDialogFragment.TAG)
        } else {
            val dialog = AddWalletBottomSheetDialogFragment.newInstance()
            dialog.callback = callback
            dialog.show(parentFragmentManager, AddWalletBottomSheetDialogFragment.TAG)
        }
    }

    private fun handleWalletCardClick(destination: WalletDestination) {
        selectedWalletDestination = destination
        when (destination) {
            is WalletDestination.Classic -> {
                destination.walletId
            }

            is WalletDestination.Import -> {
                destination.walletId
            }

            is WalletDestination.Watch -> {
                destination.walletId
            }

            else -> {
                null
            }
        }?.let { wallet ->
            jobManager.addJobInBackground(RefreshSingleWalletJob(wallet))
        }
        if (destination is WalletDestination.Classic || destination is WalletDestination.Import) {
            val walletId = if (destination is WalletDestination.Classic) {
                destination.walletId
            } else {
                (destination as WalletDestination.Import).walletId
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val wallet = walletViewModel.findWalletById(walletId)
                if (wallet != null && (wallet.category == WalletCategory.CLASSIC.value || CryptoWalletHelper.hasPrivateKey(requireActivity(), walletId))) {
                    JsSigner.setWallet(walletId, wallet.category) { queryWalletId ->
                        runBlocking { walletViewModel.getAddresses(queryWalletId) }
                    }
                    withContext(Dispatchers.Main) {
                        reloadWebViewInClips()
                    }
                    PropertyHelper.updateKeyValue(Constants.Account.SELECTED_WEB3_WALLET_ID, walletId)
                }
            }
        }
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
        importBottomBinding.privateKey.isVisible = dest is WalletDestination.Import && dest.category != WalletCategory.WATCH_ADDRESS.value
        importBottomBinding.mnemonicPhrase.isVisible = dest is WalletDestination.Import && dest.category == WalletCategory.IMPORTED_MNEMONIC.value
        importBottomBinding.rename.isVisible = dest is WalletDestination.Import || dest is WalletDestination.Watch
        if (dest is WalletDestination.Import) {
            lifecycleScope.launch {
                val wallet = walletViewModel.findWalletById(dest.walletId)
                val hasPrivateKey = wallet?.hasLocalPrivateKey == true
                importBottomBinding.privateKey.isVisible = hasPrivateKey
                importBottomBinding.mnemonicPhrase.isVisible = hasPrivateKey && wallet.category == WalletCategory.IMPORTED_MNEMONIC.value
            }
            importBottomBinding.privateKey.setOnClickListener {
                ChainSelectionBottomSheetDialogFragment.newInstance(dest.walletId).apply {
                    callback = { chainItem ->
                        WalletSecurityActivity.show(requireActivity(), WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY, chainItem.chainId, dest.walletId)
                    }
                }.show(parentFragmentManager, ChainSelectionBottomSheetDialogFragment.TAG)
                bottomSheet.dismiss()
            }
        }
        importBottomBinding.hide.setOnClickListener {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Import) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.Web3Hidden(dest.walletId))
            } else if (dest is WalletDestination.Watch) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.Web3Hidden(dest.walletId))
            }
            bottomSheet.dismiss()
        }
        importBottomBinding.transactionsTv.setOnClickListener {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Import) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(dest.walletId))
            } else if (dest is WalletDestination.Watch) {
                WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions(dest.walletId))
            }
            bottomSheet.dismiss()
        }

        importBottomBinding.mnemonicPhrase.setOnClickListener {
            WalletSecurityActivity.show(
                requireActivity(), WalletSecurityActivity.Mode.VIEW_MNEMONIC, walletId = if (dest is WalletDestination.Import) {
                    dest.walletId
                } else {
                    null
                }
            )
            bottomSheet.dismiss()
        }
        importBottomBinding.delete.setOnClickListener {
            VerifyBottomSheetDialogFragment.newInstance(
                getString(R.string.delete_wallet_title),
                disableBiometric = true,
                isHintRed = true,
                subtitle = getString(
                    if (dest is WalletDestination.Import) R.string.delete_common_wallet_description
                    else R.string.delete_watch_wallet_description,
                ),
            ).apply {
                disableToast = true
            }.setOnPinSuccess { _ ->
                deleteWallet()
            }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
            bottomSheet.dismiss()
        }
        importBottomBinding.rename.setOnClickListener {
            editDialog {
                titleText = this@WalletFragment.getString(R.string.rename_wallet)
                editText = binding.titleTv.text.toString()
                maxTextCount = 32
                allowEmpty = false
                rightAction = { newName ->
                    this@WalletFragment.lifecycleScope.launch {
                        val dest = selectedWalletDestination
                        if (dest is WalletDestination.Import) {
                            walletViewModel.renameWallet(dest.walletId, newName)
                            updateUi(dest)
                        } else if (dest is WalletDestination.Watch) {
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

    private fun deleteWallet() {
        val dialog = indeterminateProgressDialog(R.string.Please_wait_a_bit).apply {
            setCancelable(false)
        }
        dialog.show()
        lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
            dialog.dismiss()
            Timber.e(error)
        }) {
            val dest = selectedWalletDestination
            if (dest is WalletDestination.Import) {
                walletViewModel.deleteWallet(dest.walletId)
                selectedWalletDestination = WalletDestination.Classic(JsSigner.classicWalletId)
            } else if (dest is WalletDestination.Watch) {
                walletViewModel.deleteWallet(dest.walletId)
                selectedWalletDestination = WalletDestination.Classic(JsSigner.classicWalletId)
            }
            dialog.dismiss()
            withContext(Dispatchers.IO) {
                JsSigner.setWallet(JsSigner.classicWalletId, WalletCategory.CLASSIC.value) { queryWalletId ->
                    runBlocking { walletViewModel.getAddresses(queryWalletId) }
                }
                withContext(Dispatchers.Main) {
                    reloadWebViewInClips()
                }
                PropertyHelper.updateKeyValue(Constants.Account.SELECTED_WEB3_WALLET_ID, JsSigner.classicWalletId)
            }
        }
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

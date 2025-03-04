package one.mixin.android.ui.wallet

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
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
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletBinding
import one.mixin.android.databinding.ViewClassicWalletBottomBinding
import one.mixin.android.databinding.ViewPrivacyWalletBottomBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putString
import one.mixin.android.extension.replaceFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.components.AssetDashboardScreen
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.generateConversationId
import one.mixin.android.widget.BottomSheet
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

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var currentType: String = WalletDestination.Classic.name
    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val classicWalletFragment by lazy {  ClassicWalletFragment.newInstance() }
    private val privacyWalletFragment by lazy {  PrivacyWalletFragment.newInstance() }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        walletViewModel.init()
        val wallet = defaultSharedPreferences.getString(Constants.Account.PREF_HAS_USED_WALLET, null)

        binding.apply {
            badge.isVisible = wallet == null
            if (wallet == WalletDestination.Privacy.name || wallet == null) { // defualt wallet
                currentType = WalletDestination.Privacy.name
                requireActivity().replaceFragment(privacyWalletFragment, R.id.wallet_container, PrivacyWalletFragment.TAG)
                titleTv.setText(R.string.Privacy_Wallet)
                tailIcon.isVisible = true
            } else {
                // Todo wallet id
                currentType = WalletDestination.Classic.name
                requireActivity().replaceFragment(classicWalletFragment, R.id.wallet_container, ClassicWalletFragment.TAG)
                titleTv.setText(R.string.Classic_Wallet)
                tailIcon.isVisible = false
            }
            moreIb.setOnClickListener {
                if (currentType == WalletDestination.Privacy.name) {
                    showPrivacyBottom()
                } else {
                    showClassicBottom()
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
                if (currentType == WalletDestination.Privacy.name) {
                    WalletActivity.show(requireActivity(), WalletActivity.Destination.Search)
                } else {
                    WalletActivity.show(requireActivity(), WalletActivity.Destination.SearchWeb3)
                }
            }
            compose.setContent {
                AssetDashboardScreen(
                    onWalletCardClick = ::handleWalletCardClick
                )
            }

            titleRl.setOnClickListener {
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
    }

    private var migrateEnable = false

    private fun handleWalletCardClick(destination: WalletDestination, walletId: String?) {
        when (destination) {
            WalletDestination.Privacy -> {
                defaultSharedPreferences.putString(Constants.Account.PREF_HAS_USED_WALLET, WalletDestination.Privacy.name)
                currentType = WalletDestination.Privacy.name
                requireActivity().replaceFragment(privacyWalletFragment, R.id.wallet_container, PrivacyWalletFragment.TAG)
                binding.titleTv.setText(R.string.Privacy_Wallet)
                binding.tailIcon.isVisible = true
                binding.badge.isVisible = false
            }
            WalletDestination.Classic -> {
                defaultSharedPreferences.putString(Constants.Account.PREF_HAS_USED_WALLET, WalletDestination.Classic.name)
                currentType = WalletDestination.Classic.name
                requireActivity().replaceFragment(classicWalletFragment, R.id.wallet_container, ClassicWalletFragment.TAG)
                binding.titleTv.setText(R.string.Classic_Wallet)
                binding.tailIcon.isVisible = false
                binding.badge.isVisible = false
            }
        }
        closeMenu()
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
    private var _privacyBottomBinding: ViewPrivacyWalletBottomBinding? = null
    private val privacyBottomBinding get() = requireNotNull(_privacyBottomBinding)

    @SuppressLint("InflateParams")
    private fun showPrivacyBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        if (_privacyBottomBinding == null) {
            _privacyBottomBinding = ViewPrivacyWalletBottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_privacy_wallet_bottom, null))
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
            _classicBottomBinding = ViewClassicWalletBottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_classic_wallet_bottom, null))
        }
        builder.setCustomView(classicBottomBinding.root)
        val bottomSheet = builder.create()
        classicBottomBinding.hide.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.Web3Hidden)
            bottomSheet.dismiss()
        }
        classicBottomBinding.transactionsTv.setOnClickListener {
            WalletActivity.show(requireActivity(), WalletActivity.Destination.AllWeb3Transactions)
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _privacyBottomBinding = null
        _classicBottomBinding = null
    }
}
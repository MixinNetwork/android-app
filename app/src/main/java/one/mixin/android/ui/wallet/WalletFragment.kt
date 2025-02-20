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
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.replaceFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.WalletScreen
import one.mixin.android.util.rxpermission.RxPermissions
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

    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

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
                requireActivity().replaceFragment(PrivacyWalletFragment.newInstance(), R.id.wallet_container, PrivacyWalletFragment.TAG)
                titleTv.setText(R.string.Privacy_Wallet)
                tailIcon.isVisible = true
            } else {
                // Todo wallet id
                requireActivity().replaceFragment(ClassicWalletFragment.newInstance(), R.id.wallet_container, ClassicWalletFragment.TAG)
                titleTv.setText(R.string.Classic_Wallet)
                tailIcon.isVisible = false
            }
            moreIb.setOnClickListener {
                // Todo
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
                WalletActivity.show(requireActivity(), WalletActivity.Destination.Search)
            }
            compose.setContent {
                WalletScreen(
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
    }

    private fun handleWalletCardClick(destination: WalletDestination, walletId: String?) {
        when (destination) {
            WalletDestination.Privacy -> {
                // Todo save destination
                requireActivity().replaceFragment(PrivacyWalletFragment.newInstance(), R.id.wallet_container, PrivacyWalletFragment.TAG)
                binding.titleTv.setText(R.string.Privacy_Wallet)
                binding.tailIcon.isVisible = true
                binding.badge.isVisible = false
            }
            WalletDestination.Classic -> {
                // Todo save destination
                requireActivity().replaceFragment(ClassicWalletFragment.newInstance(), R.id.wallet_container, ClassicWalletFragment.TAG)
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
}
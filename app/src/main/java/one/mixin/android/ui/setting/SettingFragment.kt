package one.mixin.android.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.TEAM_MIXIN_USER_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentSettingBinding
import one.mixin.android.event.MembershipEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.setting.member.MixinMemberInvoicesFragment
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.membershipIcon
import one.mixin.android.widget.lottie.RLottieDrawable
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : BaseFragment(R.layout.fragment_setting) {
    companion object {
        const val TAG = "SettingFragment"

        fun newInstance() = SettingFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentSettingBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobManager.addJobInBackground(RefreshAccountJob())
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.apply {
            aboutRl.setOnClickListener {
                navTo(AboutFragment.newInstance(), AboutFragment.TAG)
            }
            desktopRl.setOnClickListener {
                DeviceFragment.newInstance().showNow(parentFragmentManager, DeviceFragment.TAG)
            }
            storageRl.setOnClickListener {
                navTo(SettingDataStorageFragment.newInstance(), SettingDataStorageFragment.TAG)
            }
            backupRl.setOnClickListener {
                navTo(MigrateRestoreFragment.newInstance(), MigrateRestoreFragment.TAG)
            }
            accountRl.setOnClickListener {
                navTo(AccountFragment.newInstance(), AccountFragment.TAG)
            }
            appearanceRl.setOnClickListener {
                navTo(AppearanceFragment.newInstance(), AppearanceFragment.TAG)
            }

            mixinMemberInvoicesRl.setOnClickListener {
                if (Session.getAccount()?.membership?.isMembership() == true) {
                    navTo(MixinMemberInvoicesFragment.newInstance(), MixinMemberInvoicesFragment.TAG)
                } else {
                    MixinMemberUpgradeBottomSheetDialogFragment.newInstance().showNow(
                        parentFragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG
                    )
                }
            }

            updateMembershipIcon()

            notificationRl.setOnClickListener {
                navTo(NotificationsFragment.newInstance(), NotificationsFragment.TAG)
            }
            shareRl.setOnClickListener {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.chat_on_mixin_content, Session.getAccount()?.identityNumber),
                )
                sendIntent.type = "text/plain"
                startActivity(
                    Intent.createChooser(
                        sendIntent,
                        resources.getText(R.string.Invite_a_Friend),
                    ),
                )
            }
            feedbackRl.setOnClickListener {
                lifecycleScope.launch {
                    val userTeamMixin = viewModel.refreshUser(TEAM_MIXIN_USER_ID)
                    if (userTeamMixin == null) {
                        toast(R.string.Data_error)
                    } else {
                        ConversationActivity.show(requireContext(), recipientId = TEAM_MIXIN_USER_ID)
                    }
                }
            }
        }
        RxBus.listen(MembershipEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(pauseScope)
            .subscribe { _ ->
                if (isAdded) {
                    updateMembershipIcon()
                }
            }
    }

    private fun updateMembershipIcon() {
        val icon = Session.getAccount()?.membership?.membershipIcon(true)
        if (icon != null) {
            binding.mixinMemberPlanIv.isVisible = true
            binding.mixinMemberPlanTv.isVisible = false

            if (Session.getAccount()?.membership?.isProsperity() == true) {
                binding.mixinMemberPlanIv.setImageDrawable(
                    RLottieDrawable(
                        R.raw.prosperity,
                        "prosperity",
                        18.dp,
                        18.dp
                    ).apply {
                        setAllowDecodeSingleFrame(true)
                        setAutoRepeat(1)
                        setAutoRepeatCount(Int.MAX_VALUE)
                        start()
                    }
                )
            } else {
                binding.mixinMemberPlanIv.setImageResource(icon)
            }
        } else {
            binding.mixinMemberPlanIv.isVisible = false
            binding.mixinMemberPlanTv.isVisible = true
            binding.mixinMemberPlanIv.clearAnimation()
        }
    }

}

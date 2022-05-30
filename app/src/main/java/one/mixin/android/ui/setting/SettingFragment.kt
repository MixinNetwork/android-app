package one.mixin.android.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.TEAM_MIXIN_USER_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSettingBinding
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SettingFragment : BaseFragment(R.layout.fragment_setting) {
    companion object {
        const val TAG = "SettingFragment"

        fun newInstance() = SettingFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentSettingBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            activity?.onBackPressed()
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
                navTo(BackUpFragment.newInstance(), BackUpFragment.TAG)
            }
            accountRl.setOnClickListener {
                navTo(AccountFragment.newInstance(), AccountFragment.TAG)
            }
            appearanceRl.setOnClickListener {
                navTo(AppearanceFragment.newInstance(), AppearanceFragment.TAG)
            }
            notificationRl.setOnClickListener {
                navTo(NotificationsFragment.newInstance(), NotificationsFragment.TAG)
            }
            shareRl.setOnClickListener {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.chat_on_mixin_content, Session.getAccount()?.identityNumber)
                )
                sendIntent.type = "text/plain"
                startActivity(
                    Intent.createChooser(
                        sendIntent,
                        resources.getText(R.string.Share_This_App)
                    )
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
    }
}

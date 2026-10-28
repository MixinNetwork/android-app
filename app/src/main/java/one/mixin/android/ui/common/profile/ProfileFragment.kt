package one.mixin.android.ui.common.profile

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.ReceiveQrActivity
import one.mixin.android.ui.contacts.AddPeopleFragment
import one.mixin.android.ui.contacts.ContactListFragment
import one.mixin.android.ui.contacts.ContactPageHeader
import one.mixin.android.ui.contacts.NewChatBottomSheetDialogFragment
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.vo.toUser
import one.mixin.android.widget.AvatarView
import one.mixin.android.widget.NameTextView

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    companion object {
        const val TAG = "ProfileFragment"
    }

    private val viewModel by viewModels<BottomSheetViewModel>()
    private val self by lazy { viewModel.observeSelf() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val user by self.observeAsState(Session.getAccount()?.toUser())
                MixinAppTheme {
                    Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
                        ContactPageHeader(stringResource(R.string.Profile), { requireActivity().onBackPressedDispatcher.onBackPressed() })
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    user?.let { user ->
                                        AndroidView(
                                            factory = { AvatarView(it) },
                                            modifier = Modifier.size(90.dp),
                                            update = { avatar ->
                                                avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
                                                avatar.contentDescription = getString(R.string.Profile)
                                                avatar.setOnClickListener {
                                                    user.avatarUrl?.takeIf { it.isNotBlank() }?.let { AvatarActivity.show(requireActivity(), it, avatar) }
                                                }
                                            },
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        AndroidView(
                                            factory = {
                                                NameTextView(it).apply {
                                                    textView.textSize = 18f
                                                    textView.letterSpacing = -0.4f / 18f
                                                    textView.typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                        Typeface.create(textView.typeface, 600, false)
                                                    } else {
                                                        Typeface.create(textView.typeface, Typeface.BOLD)
                                                    }
                                                }
                                            },
                                            update = { name ->
                                                name.setName(user)
                                                name.setOnIconClickListener {
                                                    Session.getAccount()?.membership?.plan?.let {
                                                        MixinMemberUpgradeBottomSheetDialogFragment.newInstance(it).showNow(parentFragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG)
                                                    }
                                                }
                                            },
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(stringResource(R.string.contact_mixin_id, user.identityNumber), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.sp)
                                        if (!user.biography.isNullOrBlank()) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(user.biography.orEmpty(), color = MixinAppTheme.colors.textMinor, fontSize = 14.sp, lineHeight = 24.sp, letterSpacing = 0.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        }
                                    }
                                }
                            }
                            item {
                                ProfileMenuGroup {
                                    ProfileMenuItem(R.drawable.ic_profile_details, R.string.Profile) {
                                        ProfileBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, ProfileBottomSheetDialogFragment.TAG)
                                    }
                                    ProfileMenuItem(R.drawable.ic_profile_qr, R.string.My_QR_Code) {
                                        Session.getAccountId()?.let {
                                            QrBottomSheetDialogFragment.newInstance(it, QrBottomSheetDialogFragment.TYPE_MY_QR).showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
                                        }
                                    }
                                    ProfileMenuItem(R.drawable.ic_profile_receive, R.string.Receive_Money) {
                                        Session.getAccountId()?.let { ReceiveQrActivity.show(requireContext(), it) }
                                    }
                                }
                            }
                            item {
                                ProfileMenuGroup {
                                    ProfileMenuItem(R.drawable.ic_profile_chat, R.string.new_chat) {
                                        NewChatBottomSheetDialogFragment().showNow(parentFragmentManager, NewChatBottomSheetDialogFragment.TAG)
                                    }
                                    ProfileMenuItem(R.drawable.ic_profile_group, R.string.New_Group) { GroupActivity.show(requireContext()) }
                                }
                            }
                            item {
                                ProfileMenuGroup {
                                    ProfileMenuItem(R.drawable.ic_profile_add_contact, R.string.Add_Contact) { navTo(AddPeopleFragment.newInstance(), AddPeopleFragment.TAG) }
                                    ProfileMenuItem(R.drawable.ic_profile_contacts, R.string.My_Contacts) { navTo(ContactListFragment(), ContactListFragment.TAG) }
                                }
                            }
                            item {
                                ProfileMenuGroup {
                                    ProfileMenuItem(R.drawable.ic_profile_settings, R.string.Settings) { SettingActivity.show(requireContext(), compose = false) }
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refreshAccount()
    }
}

@Composable
private fun ProfileMenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MixinAppTheme.colors.backgroundWindow), content = content)
}

@Composable
private fun ProfileMenuItem(icon: Int, title: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(start = 20.dp, end = 10.dp, top = 17.dp, bottom = 17.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(stringResource(title), color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)), modifier = Modifier.weight(1f).padding(vertical = 2.dp))
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(R.drawable.ic_profile_arrow), null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
    }
}

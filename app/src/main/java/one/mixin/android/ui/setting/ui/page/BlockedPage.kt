package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.UserAvatarImage
import one.mixin.android.compose.rememberComposeScope
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.setting.SettingBlockedViewModel
import one.mixin.android.vo.User

@Composable
fun BlockedPage() {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(stringResource(id = R.string.Blocked))
                },
                navigationIcon = {
                    MixinBackButton()
                },
            )
        },
    ) {
        Box(
            Modifier
                .padding(it)
                .fillMaxSize(),
        ) {
            val viewModel = hiltViewModel<SettingBlockedViewModel>()
            val users by viewModel.blockingUsers(rememberComposeScope()).observeAsState()
            if (users.isNullOrEmpty()) {
                EmptyBlockedView()
            } else {
                BlockedList(users = users!!)
            }
        }
    }
}

@Composable
private fun BlockedList(users: List<User>) {
    LazyColumn {
        items(users) {
            BlockedUserItem(user = it)
        }
        item {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(id = R.string.block_tip),
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}

@Composable
private fun EmptyBlockedView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_blocked_users),
                contentDescription = null,
                modifier =
                    Modifier
                        .height(42.dp)
                        .width(42.dp),
            )
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.No_blocked_users),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}

@Composable
private fun BlockedUserItem(user: User) {
    val context = LocalContext.current
    Row(
        modifier =
            Modifier
                .height(60.dp)
                .fillMaxWidth()
                .clickable {
                    val fragmentManager = context.findFragmentActivityOrNull()?.supportFragmentManager
                    if (fragmentManager != null) {
                        showUserBottom(user = user, fragmentManager = fragmentManager)
                    }
                }
                .background(MixinAppTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(16.dp))
        UserAvatarImage(user = user, size = 40.dp)
        Box(modifier = Modifier.width(16.dp))
        Text(
            text = user.fullName ?: "",
            color = MixinAppTheme.colors.textPrimary,
        )
    }
}

@Composable
@Preview
fun EmptyBlockedPagePreview() {
    MixinAppTheme {
        EmptyBlockedView()
    }
}

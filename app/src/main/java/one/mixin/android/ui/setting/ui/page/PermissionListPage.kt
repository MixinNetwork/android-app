package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uber.autodispose.autoDispose
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.getScopes
import one.mixin.android.compose.IndeterminateProgressDialog
import one.mixin.android.compose.MixinAlertDialog
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.rememberComposeScope
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.fullDate
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.PermissionListFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.vo.Scope

@Composable
fun PermissionListPage(
    auth: AuthorizationResponse,
    authViewModel: AuthenticationsViewModel?,
) {
    SettingPageScaffold(title = stringResource(id = R.string.Permissions)) {
        val viewModel = hiltViewModel<SettingViewModel>()

        val context = LocalContext.current

        val scopes =
            remember {
                mutableStateOf(emptyList<Scope>())
            }

        LaunchedEffect(auth) {
            scopes.value = auth.getScopes(context)
        }

        LazyColumn {
            items(scopes.value) { item ->
                PermissionScopeItem(scope = item)
            }
            item {
                Footer(
                    auth = auth,
                    authViewModel = authViewModel,
                )
            }
        }
    }
}

@Composable
private fun PermissionScopeItem(scope: Scope) {
    Column(
        modifier =
            Modifier
                .height(72.dp)
                .fillMaxWidth()
                .background(MixinAppTheme.colors.background)
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current
        Text(
            text = scope.name,
            overflow = TextOverflow.Ellipsis,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(4.dp))
        Text(
            text = scope.desc,
            overflow = TextOverflow.Ellipsis,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Footer(
    auth: AuthorizationResponse,
    authViewModel: AuthenticationsViewModel?,
) {
    Column {
        Box(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.setting_auth_access, auth.createAt.fullDate(), auth.accessedAt.fullDate()),
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textSubtitle,
        )
        Box(modifier = Modifier.height(16.dp))

        var showRevokeAlert by remember { mutableStateOf(false) }

        SettingTile(
            title = stringResource(R.string.Revoke_access),
            titleColor = MixinAppTheme.colors.red,
        ) {
            showRevokeAlert = true
        }

        if (showRevokeAlert) {
            RevokeAlertDialog(
                onRequestDismiss = {
                    showRevokeAlert = false
                },
                auth = auth,
                authViewModel = authViewModel,
            )
        }
    }
}

@Composable
private fun RevokeAlertDialog(
    onRequestDismiss: () -> Unit,
    auth: AuthorizationResponse,
    authViewModel: AuthenticationsViewModel?,
) {
    val scope = rememberComposeScope()
    val settingViewModel = hiltViewModel<SettingViewModel>()

    var showLoading by remember { mutableStateOf(false) }

    val navController = LocalSettingNav.current

    MixinAlertDialog(
        onDismissRequest = onRequestDismiss,
        text = {
            Text(text = stringResource(R.string.setting_revoke_confirmation, auth.app.name))
        },
        confirmText = stringResource(id = android.R.string.ok),
        onConfirmClick = {
            showLoading = true
            settingViewModel.deauthApp(auth.app.appId).autoDispose(scope).subscribe({
                PermissionListFragment.clearRelatedCookies(auth.app)

                authViewModel?.onDeAuthorize(auth.app.appId)

                showLoading = false
                onRequestDismiss()

                navController.pop()
            }, {
                showLoading = false
                onRequestDismiss()
            })
        },
        dismissText = stringResource(id = R.string.Cancel),
    )

    if (showLoading) {
        IndeterminateProgressDialog(
            message = stringResource(R.string.Please_wait_a_bit),
            cancelable = false,
        )
    }
}

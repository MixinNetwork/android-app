package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.ui.compose.AppAvatarImage
import one.mixin.android.ui.setting.ui.compose.HighlightText
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.vo.App

@Composable
fun AuthenticationsPage() {
    SettingPageScaffold(
        title = stringResource(id = R.string.Authorizations),
        verticalScrollable = false,
    ) {
        val viewModel = hiltViewModel<AuthenticationsViewModel>()

        val text = rememberSaveable {
            mutableStateOf("")
        }
        SearchTextFiled(text)

        val response by viewModel.authentications.collectAsState()

        if (response == null) {
            Loading()
        } else if (response?.isSuccess == true) {
            val data = response?.getOrNull() ?: emptyList()
            if (data.isEmpty()) {
                EmptyLayout()
            } else {
                AuthorizationsList(data, keyword = text.value.trim())
            }
        } else {
            EmptyLayout()
        }
    }
}

@Composable
private fun AuthorizationsList(
    data: List<AuthorizationResponse>,
    keyword: String,
) {
    val filteredData = remember(data, keyword) {
        if (keyword.isEmpty()) {
            data
        } else {
            data.filter {
                it.app.name.containsIgnoreCase(keyword) || it.app.appNumber.containsIgnoreCase(keyword)
            }.sortedByDescending {
                it.app.name.equalsIgnoreCase(keyword) || it.app.appNumber.equalsIgnoreCase(keyword)
            }
        }
    }
    LazyColumn {
        items(filteredData, key = {
            listOf(it.app.appId, keyword)
        }) { item ->
            val navigationController = LocalSettingNav.current
            AuthenticationItem(item.app, keyword) {
                navigationController.authorizationPermissions(item)
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MixinAppTheme.colors.accent,
        )
    }
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchTextFiled(text: MutableState<String>) {
    val focusRequester = remember {
        FocusRequester()
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = text.value,
        onValueChange = { text.value = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .focusRequester(focusRequester)
            .background(MixinAppTheme.colors.background)
            .onFocusChanged {
                if (it.isFocused) {
                    keyboardController?.show()
                }
            },
        interactionSource = interactionSource,
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        ),
        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
    ) { innerTextField ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                tint = MixinAppTheme.colors.textSubtitle,
                contentDescription = null,
            )
            Box(modifier = Modifier.width(8.dp))
            Box(contentAlignment = Alignment.CenterStart) {
                innerTextField()
                if (text.value.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.setting_auth_search_hint),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textSubtitle,
                    )
                }
            }
            Box(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun EmptyLayout() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-42).dp),
        ) {
            Image(
                modifier = Modifier.size(42.dp),
                painter = painterResource(id = R.drawable.ic_authentication),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MixinAppTheme.colors.textSubtitle),
            )
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.NO_AUTHORIZATIONS),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}

@Composable
private fun AuthenticationItem(
    app: App,
    highlight: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MixinAppTheme.colors.background)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(30.dp))
        AppAvatarImage(app = app, size = 50.dp)
        Box(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            HighlightText(
                text = app.name,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                ),
                overflow = TextOverflow.Ellipsis,
                target = highlight,
            )
            Box(modifier = Modifier.height(4.dp))
            HighlightText(
                text = app.appNumber,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textSubtitle,
                ),
                overflow = TextOverflow.Ellipsis,
                target = highlight,
            )
        }
        Box(modifier = Modifier.width(16.dp))
    }
}

@Composable
@Preview
fun EmptyLayoutPreview() {
    MixinAppTheme {
        EmptyLayout()
    }
}

@Composable
@Preview
fun PreviewSearchTextFiled() {
    MixinAppTheme {
        SearchTextFiled(text = remember { mutableStateOf("") })
    }
}

@Composable
@Preview
fun PreviewLoading() {
    MixinAppTheme {
        Loading()
    }
}

@Composable
@Preview
fun PreviewAuthorizationItem() {
    MixinAppTheme {
        AuthenticationItem(
            App(
                appNumber = "123456789",
                name = "Mixin",
                homeUri = "",
                redirectUri = "",
                iconUrl = "",
                category = null,
                description = "",
                appSecret = "",
                capabilities = null,
                creatorId = "",
                resourcePatterns = null,
                updatedAt = null,
                appId = "124124124",
            ),
            "123",
        ) {
        }
    }
}

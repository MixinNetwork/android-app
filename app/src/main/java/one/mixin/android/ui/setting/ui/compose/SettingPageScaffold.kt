package one.mixin.android.ui.setting.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun SettingPageScaffold(
    title: String,
    body: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(title)
                },
                navigationIcon = {
                    MixinBackButton()
                }
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            body()
        }
    }

}


@Composable
fun SettingTile(
    @DrawableRes icon: Int? = null,
    trailing: @Composable () -> Unit = {},
    title: String,
    description: String? = null,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .clickable {
                    onClick()
                }
                .height(60.dp)
                .background(color = MixinAppTheme.colors.background)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MixinAppTheme.colors.icon
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                    )
                    Box(modifier = Modifier.width(16.dp))
                }
            }
            Text(
                text = title,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            ProvideTextStyle(
                value = TextStyle(
                    color = MixinAppTheme.colors.textSubtitle,
                    fontSize = 13.sp,
                )
            ) {
                trailing()
            }
        }
        if (description != null) {
            Text(
                modifier = Modifier
                    .background(color = MixinAppTheme.colors.backgroundWindow)
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 16.dp),
                text = description,
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textSubtitle
            )
        }
    }
}


@Composable
@Preview
fun SettingScaffoldPreview() {
    MixinAppTheme {
        SettingPageScaffold(title = "Preview") {
            SettingTile(
                trailing = {
                    Text("Preview")
                },
                onClick = {},
                title = "Title",
                description = "with description"
            )

            SettingTile(
                onClick = {},
                title = "Title",
                description = "with long description, with long description, with long description,with long description,with long description"
            )

            SettingTile(
                onClick = {},
                title = "Title",
            )

            SettingTile(
                onClick = {},
                title = "Title",
                trailing = {
                    Text("Next")
                }
            )

            Box(modifier = Modifier.height(16.dp))

            SettingTile(
                onClick = {},
                title = "Title With Icon",
                icon = R.drawable.ic_setting_about,
            )

        }

    }
}

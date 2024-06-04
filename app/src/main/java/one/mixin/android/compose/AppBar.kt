package one.mixin.android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.LocalSettingNav

@Composable
fun MixinBackButton() {
    val navController = LocalSettingNav.current
    IconButton(onClick = { navController.pop() }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_back),
            contentDescription = null,
            tint = MixinAppTheme.colors.icon,
        )
    }
}

@Composable
fun MixinTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = MixinAppTheme.colors.background,
    contentColor: Color = MixinAppTheme.colors.textPrimary,
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        elevation = 0.dp,
        shape = RectangleShape,
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(AppBarDefaults.ContentPadding)
                    .height(56.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (navigationIcon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompositionLocalProvider(
                            LocalContentAlpha provides ContentAlpha.high,
                            content = navigationIcon,
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProvideTextStyle(
                        value = TextStyle(fontSize = 18.sp),
                    ) {
                        CompositionLocalProvider(
                            LocalContentAlpha provides ContentAlpha.high,
                            content = title,
                        )
                    }
                }
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Row(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMixinAppBar() {
    MixinTopAppBar(
        navigationIcon = {
            MixinBackButton()
        },
        title = {
            Text(text = "Title")
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
    )
}

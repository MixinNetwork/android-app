import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun PageScaffold(
    title: String,
    subtitleText: String?,
    verticalScrollable: Boolean = true,
    pop: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    PageScaffold(
        title = title,
        subtitle = subtitleText?.let { text ->
            @Composable {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MixinAppTheme.colors.textAssist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        verticalScrollable = verticalScrollable,
        pop = pop,
        actions = actions,
        body = body
    )
}

@Composable
fun PageScaffold(
    title: String,
    subtitle: @Composable (() -> Unit)? = null,
    verticalScrollable: Boolean = true,
    pop: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    backIcon: Int = R.drawable.ic_back,
    body: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            MixinTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        subtitle?.invoke()
                    }
                },
                actions = actions,
                navigationIcon = {
                    pop?.let { pop ->
                        IconButton(onClick = { pop() }) {
                            Icon(
                                painter = painterResource(id = backIcon),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.icon,
                            )
                        }
                    }
                },
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .apply {
                    if (verticalScrollable) {
                        verticalScroll(rememberScrollState())
                    }
                },
        ) {
            body()
        }
    }
}

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun PageScaffold(
    title: String,
    verticalScrollable: Boolean = true,
    pop: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(title)
                },
                actions = actions,
                navigationIcon = {
                    pop?.let { pop ->
                        IconButton(onClick = { pop() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
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
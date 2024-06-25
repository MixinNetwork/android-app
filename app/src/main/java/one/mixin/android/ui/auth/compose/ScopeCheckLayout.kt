package one.mixin.android.ui.auth.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.Scope

@Composable
fun ScopeCheckLayout(
    scope: Scope,
    state: Boolean = true,
    onCheckedChange: ((checked: Boolean) -> Unit)? = null,
) {
    val checkedState = remember { mutableStateOf(state) }
    val isProfileScope = scope.source == Scope.SCOPES[0]
    Row(
        modifier =
            Modifier
                .run {
                    if (!isProfileScope && onCheckedChange != null) {
                        clickable {
                            checkedState.value = !checkedState.value
                            onCheckedChange.invoke(checkedState.value)
                        }
                    } else {
                        this
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
    ) {
        Image(
            modifier =
                Modifier
                    .padding(vertical = 4.dp)
                    .padding(end = 8.dp),
            painter =
                painterResource(
                    id =
                        when {
                            isProfileScope ->
                                R.drawable.ic_selected_disable
                            checkedState.value ->
                                R.drawable.ic_selected
                            else ->
                                R.drawable.ic_not_selected
                        },
                ),
            contentDescription = null,
        )
        Column(
            modifier = Modifier.align(alignment = Alignment.Top),
        ) {
            Text(
                scope.name,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Text(
                scope.desc,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}

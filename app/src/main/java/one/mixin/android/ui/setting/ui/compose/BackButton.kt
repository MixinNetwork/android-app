package one.mixin.android.ui.setting.ui.compose

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import one.mixin.android.ui.setting.LocalSettingNav

@Composable
fun MixinBackButton() {
    val navController = LocalSettingNav.current
    IconButton(onClick = { navController.pop() }) {
        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
    }
}
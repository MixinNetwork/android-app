package one.mixin.android.ui.setting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.mixin.android.ui.setting.ui.page.SettingPage
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import timber.log.Timber

enum class SettingDestination {
    Setting,
    Account,
    NotificationAndConfirm,
    Backup,
    Storage,
    Appearance,
    Desktop,
    Feedback,
    Share,
    About,
}

open class SettingNavigationController {
    open fun navigation(destination: SettingDestination) {
        Timber.e("setting navigation: ${destination.name}")
    }
}

private class SettingNavControllerImpl(
    private val navController: NavController
) : SettingNavigationController() {
    override fun navigation(destination: SettingDestination) {
        navController.navigate(destination.name)
    }
}

val LocalSettingNav =
    compositionLocalOf { SettingNavigationController() }

class SettingActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MixinAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    val navigationController = remember {
                        SettingNavControllerImpl(navController)
                    }
                    CompositionLocalProvider(
                        LocalSettingNav provides navigationController
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = SettingDestination.Setting.name
                        ) {
                            composable(SettingDestination.Setting.name) {
                                SettingPage()
                            }
                        }
                    }
                }
            }
        }
    }
}

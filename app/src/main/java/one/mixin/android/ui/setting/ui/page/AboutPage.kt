package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.diagnosis.DiagnosisActivity
import one.mixin.android.ui.setting.ui.compose.MixinBackButton
import one.mixin.android.ui.setting.ui.compose.MixinTopAppBar
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.widget.DebugClickHandler

private fun Modifier.debugClickable(
    onClick: (() -> Unit)? = null,
    onDebugClick: (() -> Unit)? = null,
) = composed(
    factory = {
        val clickListener = remember {
            object : DebugClickHandler() {
                override fun onDebugClick() {
                    onDebugClick?.invoke()
                }

                override fun onSingleClick() {
                    onClick?.invoke()
                }
            }
        }
        clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = clickListener::onClick,
        )
    },
)

@Composable
fun AboutPage() {
    val preferences = LocalContext.current.defaultSharedPreferences

    val showDatabase =
        remember { mutableStateOf(preferences.getBoolean(Constants.Debug.DB_DEBUG, false)) }

    LaunchedEffect(showDatabase.value) {
        preferences.putBoolean(Constants.Debug.DB_DEBUG, showDatabase.value)
        if (!showDatabase.value) {
            preferences.putBoolean(Constants.Debug.DB_DEBUG_WARNING, true)
        }
    }

    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            val context = LocalContext.current
            MixinTopAppBar(
                modifier = Modifier.debugClickable(
                    onDebugClick = {
                        showDatabase.value = !showDatabase.value
                        if (showDatabase.value) {
                            toast(R.string.Enable_db_debug)
                        } else {
                            toast(R.string.Disable_db_debug)
                        }
                    },
                    onClick = {
                        DiagnosisActivity.show(context)
                    },
                ),
                navigationIcon = {
                    MixinBackButton()
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = stringResource(id = R.string.About))
                        VersionName()
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState()),
        ) {
            val navController = LocalSettingNav.current
            val context = LocalContext.current
            Image(
                modifier = Modifier
                    .debugClickable {
                        if (preferences.getBoolean(Constants.Debug.WEB_DEBUG, false)) {
                            preferences.putBoolean(Constants.Debug.WEB_DEBUG, false)
                            toast(R.string.Disable_web_debug)
                        } else {
                            preferences.putBoolean(Constants.Debug.WEB_DEBUG, true)
                            toast(R.string.Enable_web_debug)
                        }
                    }
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
            )
            AboutTile(
                text = stringResource(id = R.string.Follow_us_on_twitter),
                onClick = {
                    context.openUrl("https://twitter.com/MixinMessenger")
                },
            )
            AboutTile(
                text = stringResource(id = R.string.Follow_us_on_facebook),
                onClick = {
                    context.openUrl("https://fb.com/MixinMessenger")
                },
            )
            AboutTile(
                text = stringResource(id = R.string.Help_center),
                onClick = {
                    context.openUrl(Constants.HelpLink.CENTER)
                },
            )
            AboutTile(
                text = stringResource(id = R.string.Terms_of_Service),
                onClick = {
                    context.openUrl(context.getString(R.string.landing_terms_url))
                },
            )
            AboutTile(
                text = stringResource(id = R.string.Privacy_Policy),
                onClick = {
                    context.openUrl(context.getString(R.string.landing_privacy_policy_url))
                },
            )
            AboutTile(
                text = stringResource(id = R.string.Version_Update),
                onClick = {
                    context.openMarket()
                },
            )
            if (showDatabase.value) {
                AboutTile(
                    text = stringResource(id = R.string.Debug_database),
                    onClick = {
                        navController.navigation(SettingDestination.DatabaseDebug)
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutTile(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, color = MixinAppTheme.colors.accent)
    }
}

@Composable
private fun VersionName() {
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager?.getPackageInfo(
            context.packageName,
            0,
        )?.versionName ?: "Unknown"
    }
    Text(
        text = stringResource(
            R.string.about_version,
            versionName,
        ),
        color = MixinAppTheme.colors.textSubtitle,
        fontSize = 10.sp,
    )
}

@Preview
@Composable
fun AboutPagePreview() {
    MixinAppTheme {
        AboutPage()
    }
}

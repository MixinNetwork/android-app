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
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.widget.DebugClickHandler

private fun Modifier.debugClickable(
    onClick: (() -> Unit)? = null,
    onDebugClick: (() -> Unit)? = null,
) = composed(
    factory = {
        val clickListener =
            remember {
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
    val settingNavController = LocalSettingNav.current
    val preferences = LocalContext.current.defaultSharedPreferences
    val showLogDebug =
        remember { mutableStateOf(preferences.getBoolean(Constants.Debug.LOG_AND_DEBUG, false)) }

    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
            val context = LocalContext.current
            MixinTopAppBar(
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
            modifier =
                Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState()),
        ) {
            val context = LocalContext.current
            val attrs = context.obtainStyledAttributes(intArrayOf(R.attr.ic_logo))
            val logoResId = attrs.getResourceId(0, R.drawable.ic_logo_mixin) // 默认值为 ic_logo_mixin
            attrs.recycle() // 记得回收
            Image(
                modifier =
                    Modifier
                        .debugClickable {
                            if (preferences.getBoolean(Constants.Debug.LOG_AND_DEBUG, false)) {
                                preferences.putBoolean(Constants.Debug.LOG_AND_DEBUG, false)
                                showLogDebug.value = false
                            } else {
                                preferences.putBoolean(Constants.Debug.LOG_AND_DEBUG, true)
                                showLogDebug.value = true
                            }
                        }
                        .align(Alignment.CenterHorizontally),
                painter = painterResource(id = logoResId),
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
                    context.openUrl(context.getString(R.string.help_link))
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
            if (showLogDebug.value) {
                AboutTile(
                    text = stringResource(id = R.string.LogAndDebug),
                    onClick = {
                        settingNavController.navigation(SettingDestination.LogAndDebug)
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
        modifier =
            Modifier
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
    val versionName =
        remember {
            context.packageManager?.getPackageInfo(
                context.packageName,
                0,
            )?.versionName ?: "Unknown"
        }
    Text(
        text = "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}",
        color = MixinAppTheme.colors.textAssist,
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

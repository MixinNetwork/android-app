package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalInspectionMode
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
    val context = LocalContext.current
    val preferences = context.defaultSharedPreferences
    val showLogDebugInitial = remember { preferences.getBoolean(Constants.Debug.LOG_AND_DEBUG, false) }
    
    AboutPageContent(
        showLogDebugInitial = showLogDebugInitial,
        onUpdateLogDebug = { newValue -> preferences.putBoolean(Constants.Debug.LOG_AND_DEBUG, newValue) }
    )
}

@Composable
fun AboutPageContent(
    showLogDebugInitial: Boolean,
    onUpdateLogDebug: (Boolean) -> Unit,
) {
    val settingNavController = LocalSettingNav.current
    val showLogDebug = remember { mutableStateOf(showLogDebugInitial) }

    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {
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
            val isInPreview = LocalInspectionMode.current
            val logoResId = if (isInPreview) R.drawable.ic_launcher_logo else {
                val attrs = context.obtainStyledAttributes(intArrayOf(R.attr.ic_logo))
                val resId = attrs.getResourceId(0, R.drawable.ic_launcher_logo)
                attrs.recycle()
                resId
            }
            Image(
                modifier =
                    Modifier
                        .debugClickable {
                            val isLogDebug = showLogDebug.value
                            onUpdateLogDebug(!isLogDebug)
                            showLogDebug.value = !isLogDebug
                        }
                        .padding(top = 40.dp, bottom = 20.dp)
                        .size(64.dp)
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
            val helpLink = stringResource(R.string.help_link)
            AboutTile(
                text = stringResource(id = R.string.Help_center),
                onClick = {
                    context.openUrl(helpLink)
                },
            )
            val termsUrl = stringResource(R.string.landing_terms_url)
            AboutTile(
                text = stringResource(id = R.string.Terms_of_service),
                onClick = {
                    context.openUrl(termsUrl)
                },
            )
            val privacyUrl = stringResource(R.string.landing_privacy_url)
            AboutTile(
                text = stringResource(id = R.string.Privacy_policy),
                onClick = {
                    context.openUrl(privacyUrl)
                },
            )
            AboutTile(
                text = stringResource(id = R.string.Check_for_updates),
                onClick = {
                    context.openMarket(context.packageName)
                },
            )
            if (showLogDebug.value) {
                AboutTile(stringResource(id = R.string.Logs)) {
                    settingNavController.navigation(SettingDestination.Logs)
                }
            }
        }
    }
}

@Composable
private fun VersionName() {
    Text(
        text = "V ${BuildConfig.VERSION_NAME}",
        color = MixinAppTheme.colors.textAssist,
        fontSize = 10.sp,
    )
}

@Composable
private fun AboutTile(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onClick() }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
    }
}

@Preview
@Composable
fun AboutPagePreview() {
    MixinAppTheme {
        AboutPageContent(showLogDebugInitial = false, onUpdateLogDebug = {})
    }
}

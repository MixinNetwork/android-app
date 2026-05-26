package one.mixin.android.ui.setting.ui.page

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinAlertDialog
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.putInt
import one.mixin.android.extension.singleChoice
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.AppearanceFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.setting.CurrencyBottomSheetDialogFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.SettingDestination
import one.mixin.android.ui.setting.getLanguagePos
import one.mixin.android.util.TimeCache
import one.mixin.android.util.isFollowSystem
import one.mixin.android.vo.Fiats
import java.util.Locale

@Composable
fun AppearancePage() {
    val context = LocalContext.current
    val preference = context.defaultSharedPreferences
    val initialThemeId = preference.getInt(
        Constants.Theme.THEME_CURRENT_ID,
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Constants.Theme.THEME_DEFAULT_ID
        } else {
            Constants.Theme.THEME_AUTO_ID
        },
    )
    val initialCurrency = Session.getFiatCurrency()
    
    AppearancePageContent(
        initialThemeId = initialThemeId,
        initialCurrency = initialCurrency,
        getCurrencySymbol = { Fiats.getSymbol(it) }
    )
}

@Composable
fun AppearancePageContent(
    initialThemeId: Int,
    initialCurrency: String,
    getCurrencySymbol: (String) -> String,
) {
    val navController = LocalSettingNav.current
    Scaffold(
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(stringResource(R.string.Appearance))
                },
                navigationIcon = {
                    MixinBackButton()
                },
            )
        },
    ) {
        Column(Modifier.padding(it)) {
            ThemeItem(initialThemeId)

            Box(modifier = Modifier.height(20.dp))

            LanguageItem()

            Box(modifier = Modifier.height(20.dp))

            CurrencyItem(initialCurrency, getCurrencySymbol)

            Box(modifier = Modifier.height(20.dp))

            Row(
                modifier =
                    Modifier
                        .height(60.dp)
                        .background(MixinAppTheme.colors.background)
                        .clickable {
                            navController.navigation(SettingDestination.Wallpaper)
                        }
                        .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.chat_background),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun ThemeItem(initialThemeId: Int) {
    val context = LocalContext.current
    val preference = context.defaultSharedPreferences
    val themeTitle = stringResource(id = R.string.Theme)
    val themeOptions = stringArrayResource(R.array.setting_night_array_oreo)

    val currentThemeId = remember(initialThemeId) { mutableStateOf(initialThemeId) }

    AppearanceItem(
        label = themeTitle,
        value = themeOptions[currentThemeId.value],
    ) {
        context.singleChoice(
            themeTitle,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                R.array.setting_night_array
            } else {
                R.array.setting_night_array_oreo
            },
            currentThemeId.value,
        ) { _, index ->
            val changed = index != currentThemeId.value
            currentThemeId.value = index
            preference.putInt(Constants.Theme.THEME_CURRENT_ID, index)
            AppCompatDelegate.setDefaultNightMode(
                when (index) {
                    Constants.Theme.THEME_DEFAULT_ID -> AppCompatDelegate.MODE_NIGHT_NO
                    Constants.Theme.THEME_NIGHT_ID -> AppCompatDelegate.MODE_NIGHT_YES
                    Constants.Theme.THEME_AUTO_ID -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else -> AppCompatDelegate.MODE_NIGHT_NO
                },
            )
            if (changed && isFollowSystem()) {
                val activity = context.findFragmentActivityOrNull()
                activity?.recreate()
            }
        }
    }
}

@Composable
private fun LanguageItem() {
    val showLanguageDialog =
        remember {
            mutableStateOf(false)
        }

    val context = LocalContext.current

    AppearanceItem(
        label = stringResource(id = R.string.Language),
        value = stringArrayResource(id = R.array.setting_language_array)[getLanguagePos()],
    ) {
        showLanguageDialog.value = true
    }

    if (showLanguageDialog.value) {
        MixinAlertDialog(
            onDismissRequest = {
                showLanguageDialog.value = false
            },
            confirmButton = {},
            title = {
                Text(
                    text = stringResource(id = R.string.Language),
                    fontSize = 18.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
            },
            text = {
                Column {
                    val radioOptions = stringArrayResource(id = R.array.setting_language_array)
                    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[getLanguagePos()]) }
                    radioOptions.forEach { text ->
                        LanguageRadioItem(
                            name = text,
                            selected = (text == selectedOption),
                            onOptionSelected = {
                                onOptionSelected(text)
                                showLanguageDialog.value = false
                                val pos = radioOptions.indexOf(text)
                                if (pos == getLanguagePos()) return@LanguageRadioItem
                                val locale =
                                    when (pos) {
                                        0 -> Locale.getDefault()
                                        1 -> Locale.ENGLISH
                                        2 -> Locale.SIMPLIFIED_CHINESE
                                        3 -> Locale.TRADITIONAL_CHINESE
                                        4 -> Locale("zh", "HK")
                                        5 -> Locale("ja")
                                        else -> Locale.getDefault()
                                    }
                                val appLocale: LocaleListCompat = LocaleListCompat.create(locale)
                                AppCompatDelegate.setApplicationLocales(appLocale)

                                TimeCache.clear()
                            },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun LanguageRadioItem(
    name: String,
    selected: Boolean,
    onOptionSelected: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = { onOptionSelected(name) },
                role = Role.RadioButton,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MixinAppTheme.colors.accent,
                    unselectedColor = MixinAppTheme.colors.unchecked,
                ),
        )
        Text(
            text = name,
            style =
                TextStyle(
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                ),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun CurrencyItem(
    initialCurrency: String,
    getCurrencySymbol: (String) -> String,
) {
    val currentCurrency = remember(initialCurrency) { mutableStateOf(initialCurrency) }

    val context = LocalContext.current

    AppearanceItem(
        label = stringResource(id = R.string.Currency),
        value =
            stringResource(
                R.string.wallet_setting_currency_desc,
                currentCurrency.value,
                getCurrencySymbol(currentCurrency.value),
            ),
    ) {
        val activity = context.findFragmentActivityOrNull() ?: return@AppearanceItem
        val currencyBottom = CurrencyBottomSheetDialogFragment.newInstance()
        currencyBottom.callback =
            object : CurrencyBottomSheetDialogFragment.Callback {
                override fun onCurrencyClick(currency: Currency) {
                    currentCurrency.value = currency.name
                }
            }
        currencyBottom.show(activity.supportFragmentManager, AppearanceFragment.TAG)
    }
}

@Composable
private fun AppearanceItem(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(60.dp)
                .background(MixinAppTheme.colors.background)
                .clickable { onClick() }
                .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist,
        )
    }
}

@Composable
@Preview
fun AppearancePagePreview() {
    MixinAppTheme {
        AppearancePageContent(
            initialThemeId = 0,
            initialCurrency = "USD",
            getCurrencySymbol = { "$" }
        )
    }
}

@Composable
@Preview
fun AppearanceItemPreview() {
    MixinAppTheme {
        AppearanceItem("Label", "Value") {}
    }
}

@Composable
@Preview
fun LanguageRadioItemPreview() {
    MixinAppTheme {
        Column(
            modifier = Modifier.background(MixinAppTheme.colors.background),
        ) {
            listOf(
                LanguageRadioItem(
                    name = "name",
                    selected = true,
                    onOptionSelected = {
                    },
                ),
                LanguageRadioItem(
                    name = "name",
                    selected = false,
                    onOptionSelected = {
                    },
                ),
            )
        }
    }
}

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
import one.mixin.android.compose.MixinAlertDialog
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.util.TimeCache
import one.mixin.android.util.isFollowSystem
import one.mixin.android.vo.Fiats
import java.util.Locale

@Composable
fun AppearancePage() {
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
            ThemeItem()

            Box(modifier = Modifier.height(20.dp))

            LanguageItem()

            Box(modifier = Modifier.height(20.dp))

            CurrencyItem()

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
private fun ThemeItem() {
    val context = LocalContext.current
    val preference = context.defaultSharedPreferences

    val currentThemeId =
        remember {
            val id =
                preference.getInt(
                    Constants.Theme.THEME_CURRENT_ID,
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        Constants.Theme.THEME_DEFAULT_ID
                    } else {
                        Constants.Theme.THEME_AUTO_ID
                    },
                )
            mutableStateOf(id)
        }

    AppearanceItem(
        label = stringResource(id = R.string.Theme),
        value = context.resources.getStringArray(R.array.setting_night_array_oreo)[currentThemeId.value],
    ) {
        context.singleChoice(
            context.getString(R.string.Theme),
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
            if (changed) {
                context.findFragmentActivityOrNull()?.apply {
                    onBackPressed()
                    recreate()
                }
            }
        }
    }
}

@Composable
private fun LanguageItem() {
    val languageNames =
        stringArrayResource(R.array.language_names).apply {
            this[0] = stringResource(R.string.Follow_system)
        }

    val showLanguageDialog = remember { mutableStateOf(false) }

    val currentLanguage =
        remember {
            val index =
                if (isFollowSystem()) {
                    AppearanceFragment.POS_FOLLOW_SYSTEM
                } else {
                    getLanguagePos()
                }
            mutableStateOf(index)
        }

    AppearanceItem(
        label = stringResource(R.string.Language),
        value = languageNames[currentLanguage.value],
    ) {
        showLanguageDialog.value = true
    }

    if (showLanguageDialog.value) {
        val dialogSelected = remember { mutableStateOf(currentLanguage.value) }
        val context = LocalContext.current
        MixinAlertDialog(
            title = {
                Text(stringResource(R.string.Language))
            },
            onDismissRequest = {
                showLanguageDialog.value = false
            },
            text = {
                Column {
                    Box(modifier = Modifier.height(8.dp))

                    listOf(
                        AppearanceFragment.POS_FOLLOW_SYSTEM,
                        AppearanceFragment.POS_ENGLISH,
                        AppearanceFragment.POS_SIMPLIFY_CHINESE,
                        AppearanceFragment.POS_TRADITIONAL_CHINESE,
                        AppearanceFragment.POS_SIMPLIFY_JAPANESE,
                        AppearanceFragment.POS_RUSSIAN,
                        AppearanceFragment.POS_INDONESIA,
                        AppearanceFragment.POS_Malay,
                    ).forEach { index ->
                        LanguageRadioItem(
                            name = languageNames[index],
                            selected = dialogSelected.value == index,
                            onOptionSelected = {
                                dialogSelected.value = index
                            },
                        )
                    }
                }
            },
            dismissText = stringResource(R.string.Cancel),
            confirmText = stringResource(R.string.OK),
            onConfirmClick = {
                showLanguageDialog.value = false

                val newSelected = dialogSelected.value
                if (currentLanguage.value != newSelected) {
                    currentLanguage.value = newSelected
                    if (newSelected == AppearanceFragment.POS_FOLLOW_SYSTEM) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    } else {
                        val selectedLang =
                            when (newSelected) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.language
                                AppearanceFragment.POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.language
                                AppearanceFragment.POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.language
                                AppearanceFragment.POS_RUSSIAN -> Constants.Locale.Russian.Language
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Language
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Language
                                else -> Locale.US.language
                            }
                        val selectedCountry =
                            when (newSelected) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                                AppearanceFragment.POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.country
                                AppearanceFragment.POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.country
                                AppearanceFragment.POS_RUSSIAN -> Constants.Locale.Russian.Country
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Country
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Country
                                else -> Locale.US.country
                            }
                        val newLocale = Locale(selectedLang, selectedCountry)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(newLocale))
                    }

                    TimeCache.singleton.evictAll()
                    context.findFragmentActivityOrNull()?.apply {
                        onBackPressed()
                        recreate()
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
    onOptionSelected: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = (selected),
                onClick = { onOptionSelected() },
                role = Role.RadioButton,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MixinAppTheme.colors.accent,
                ),
            selected = selected,
            onClick = null, // null recommended for accessibility with screenreaders
        )
        Text(
            text = name,
            modifier = Modifier.padding(start = 16.dp),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
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
                .fillMaxWidth()
                .background(MixinAppTheme.colors.background)
                .height(60.dp)
                .clickable {
                    onClick()
                }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style =
                TextStyle(
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                ),
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = value,
            style =
                TextStyle(
                    color = MixinAppTheme.colors.textSubtitle,
                ),
        )
    }
}

@Composable
private fun CurrencyItem() {
    val currentCurrency =
        remember {
            mutableStateOf(Session.getFiatCurrency())
        }

    val context = LocalContext.current

    AppearanceItem(
        label = stringResource(id = R.string.Currency),
        value =
            stringResource(
                R.string.wallet_setting_currency_desc,
                currentCurrency.value,
                Fiats.getSymbol(currentCurrency.value),
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
        currencyBottom.showNow(
            activity.supportFragmentManager,
            CurrencyBottomSheetDialogFragment.TAG,
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

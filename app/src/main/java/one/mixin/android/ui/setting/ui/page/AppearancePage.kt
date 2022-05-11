package one.mixin.android.ui.setting.ui.page

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.ui.setting.ui.compose.SettingTile
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.util.TimeCache
import one.mixin.android.util.language.Lingver
import one.mixin.android.vo.Fiats
import java.util.*


@Composable
fun AppearancePage() {
    SettingPageScaffold(title = stringResource(R.string.setting_appearance)) {
        ThemeItem()

        Box(modifier = Modifier.height(20.dp))

        LanguageItem()

        Box(modifier = Modifier.height(20.dp))

        CurrencyItem()

    }

}

@Composable
private fun ThemeItem() {
    val context = LocalContext.current
    val preference = context.defaultSharedPreferences

    val currentThemeId = remember {
        val id = preference.getInt(
            Constants.Theme.THEME_CURRENT_ID,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Constants.Theme.THEME_DEFAULT_ID
            } else {
                Constants.Theme.THEME_AUTO_ID
            }
        )
        mutableStateOf(id)
    }

    SettingTile(
        trailing = {
            Text(text = stringResource(id = R.string.setting_theme))
        },
        title = context.resources.getStringArray(R.array.setting_night_array_oreo)[currentThemeId.value],
    ) {
        context.singleChoice(
            context.getString(R.string.setting_theme),
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
                }
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

    val languageNames = stringArrayResource(R.array.language_names).apply {
        this[0] = stringResource(R.string.follow_system)
    }

    val showLanguageDialog = remember { mutableStateOf(false) }

    val currentLanguage = remember {
        val index = if (Lingver.getInstance().isFollowingSystemLocale()) {
            AppearanceFragment.POS_FOLLOW_SYSTEM
        } else {
            when (Lingver.getInstance().getLanguage()) {
                Locale.SIMPLIFIED_CHINESE.language -> {
                    AppearanceFragment.POS_SIMPLIFY_CHINESE
                }
                Constants.Locale.Indonesian.Language -> {
                    AppearanceFragment.POS_INDONESIA
                }
                Constants.Locale.Malay.Language -> {
                    AppearanceFragment.POS_Malay
                }
                else -> {
                    AppearanceFragment.POS_ENGLISH
                }
            }
        }
        mutableStateOf(index)
    }

    SettingTile(
        trailing = {
            Text(text = stringResource(R.string.language))
        },
        title = languageNames[currentLanguage.value],
    ) {
        showLanguageDialog.value = true
    }

    if (showLanguageDialog.value) {
        val dialogSelected = remember { mutableStateOf(currentLanguage.value) }
        AlertDialog(
            title = {
                Text(stringResource(R.string.language))
            },
            onDismissRequest = {
                showLanguageDialog.value = false
            },
            text = {
                Column {
                    listOf(
                        AppearanceFragment.POS_FOLLOW_SYSTEM,
                        AppearanceFragment.POS_ENGLISH,
                        AppearanceFragment.POS_SIMPLIFY_CHINESE,
                        AppearanceFragment.POS_INDONESIA,
                        AppearanceFragment.POS_Malay,
                    ).forEach { index ->
                        LanguageRadioItem(
                            name = languageNames[index],
                            selected = dialogSelected.value == index,
                            onOptionSelected = {
                                dialogSelected.value = index
                            })
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLanguageDialog.value = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MixinAppTheme.colors.textSubtitle,
                    ),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                val context = LocalContext.current

                TextButton(onClick = {
                    showLanguageDialog.value = false

                    val newSelected = dialogSelected.value
                    if (currentLanguage.value != newSelected) {
                        currentLanguage.value = newSelected
                        if (newSelected == AppearanceFragment.POS_FOLLOW_SYSTEM) {
                            Lingver.getInstance().setFollowSystemLocale(context)
                        } else {
                            val selectedLang = when (newSelected) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.language
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Language
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Language
                                else -> Locale.US.language
                            }
                            val selectedCountry = when (newSelected) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Country
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Country
                                else -> Locale.US.country
                            }
                            val newLocale = Locale(selectedLang, selectedCountry)
                            Lingver.getInstance().setLocale(context, newLocale)
                        }

                        TimeCache.singleton.evictAll()
                        context.findFragmentActivityOrNull()?.apply {
                            onBackPressed()
                            recreate()
                        }

                    }

                }) {
                    Text(stringResource(R.string.capital_ok))
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
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = name,
            style = MaterialTheme.typography.body1.merge(),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}


@Composable
private fun CurrencyItem() {

    val currentCurrency = remember {
        mutableStateOf(Session.getFiatCurrency())
    }

    val context = LocalContext.current

    SettingTile(
        trailing = {
            Text(text = stringResource(id = R.string.currency))
        },
        title = stringResource(
            R.string.wallet_setting_currency_desc,
            currentCurrency.value, Fiats.getSymbol(currentCurrency.value)
        ),
    ) {
        val activity = context.findFragmentActivityOrNull() ?: return@SettingTile
        val currencyBottom = CurrencyBottomSheetDialogFragment.newInstance()
        currencyBottom.callback = object : CurrencyBottomSheetDialogFragment.Callback {
            override fun onCurrencyClick(currency: Currency) {
                currentCurrency.value = currency.name
            }
        }
        currencyBottom.showNow(activity.supportFragmentManager, CurrencyBottomSheetDialogFragment.TAG)
    }

}

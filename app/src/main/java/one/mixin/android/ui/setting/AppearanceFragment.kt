package one.mixin.android.ui.setting

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_SHOW_TRANSLATE_BUTTON
import one.mixin.android.Constants.Account.PREF_TRANSLATE_TARGET_LANG
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAppearanceBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.singleChoice
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.TimeCache
import one.mixin.android.util.TranslateManager
import one.mixin.android.util.getLanguage
import one.mixin.android.util.getLanguageOrDefault
import one.mixin.android.util.getLocaleString
import one.mixin.android.util.isFollowSystem
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import java.util.Locale

@AndroidEntryPoint
class AppearanceFragment : BaseFragment(R.layout.fragment_appearance) {
    companion object {
        const val TAG = "AppearanceFragment"

        const val POS_FOLLOW_SYSTEM = 0
        const val POS_ENGLISH = 1
        const val POS_SIMPLIFY_CHINESE = 2
        const val POS_TRADITIONAL_CHINESE = 3
        const val POS_SIMPLIFY_JAPANESE = 4
        const val POS_RUSSIAN = 5
        const val POS_INDONESIA = 6
        const val POS_Malay = 7
        const val POS_Spanish = 8

        fun newInstance() = AppearanceFragment()
    }

    private val binding by viewBinding(FragmentAppearanceBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            nightModeTv.setText(R.string.Theme)
            val currentId = defaultSharedPreferences.getInt(
                Constants.Theme.THEME_CURRENT_ID,
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Constants.Theme.THEME_DEFAULT_ID
                } else {
                    Constants.Theme.THEME_AUTO_ID
                },
            )
            nightModeDescTv.text = resources.getStringArray(R.array.setting_night_array_oreo)[currentId]
            nightModeRl.setOnClickListener {
                singleChoice(
                    resources.getString(R.string.Theme),
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        R.array.setting_night_array
                    } else {
                        R.array.setting_night_array_oreo
                    },
                    currentId,
                ) { dialog, index ->
                    val changed = index != currentId
                    defaultSharedPreferences.putInt(Constants.Theme.THEME_CURRENT_ID, index)
                    AppCompatDelegate.setDefaultNightMode(
                        when (index) {
                            Constants.Theme.THEME_DEFAULT_ID -> AppCompatDelegate.MODE_NIGHT_NO
                            Constants.Theme.THEME_NIGHT_ID -> AppCompatDelegate.MODE_NIGHT_YES
                            Constants.Theme.THEME_AUTO_ID -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            else -> AppCompatDelegate.MODE_NIGHT_NO
                        },
                    )
                    dialog.dismiss()
                    if (changed) {
                        requireActivity().onBackPressed()
                        requireActivity().recreate()
                    }
                }
            }
            val languageNames = resources.getStringArray(R.array.language_names)
            languageDescTv.text = if (isFollowSystem()) {
                getString(R.string.Follow_system)
            } else {
                languageNames[getLanguagePos()]
            }
            languageRl.setOnClickListener { showLanguageAlert() }
            currentTv.text = getString(R.string.wallet_setting_currency_desc, Session.getFiatCurrency(), Fiats.getSymbol())
            currencyRl.setOnClickListener {
                val currencyBottom = CurrencyBottomSheetDialogFragment.newInstance()
                currencyBottom.callback = object : CurrencyBottomSheetDialogFragment.Callback {
                    override fun onCurrencyClick(currency: Currency) {
                        currentTv.text = getString(R.string.wallet_setting_currency_desc, currency.name, currency.symbol)
                    }
                }
                currencyBottom.showNow(parentFragmentManager, CurrencyBottomSheetDialogFragment.TAG)
            }

            backgroundRl.setOnClickListener {
                navTo(SettingWallpaperFragment.newInstance(), SettingWallpaperFragment.TAG)
            }

            textSizeRl.setOnClickListener {
                navTo(SettingSizeFragment.newInstance(), SettingSizeFragment.TAG)
            }

            translateSv.setContent(R.string.Show_Translate_Button)
            translateSv.isChecked = defaultSharedPreferences.getBoolean(PREF_SHOW_TRANSLATE_BUTTON, false)
            translateSv.setOnCheckedChangeListener { _, isChecked ->
                defaultSharedPreferences.putBoolean(PREF_SHOW_TRANSLATE_BUTTON, isChecked)
                targetRl.isVisible = isChecked
                translateSv.updateLayoutParams<MarginLayoutParams> {
                    topMargin = if (isChecked) 0 else 20.dp
                }
            }
            targetLangTv.text = defaultSharedPreferences.getString(PREF_TRANSLATE_TARGET_LANG, getLanguageOrDefault())?.let {
                TranslateManager.Language(it).nameInCurrentLanguage
            }
            targetRl.setOnClickListener {
                val languageBottom = TranslateLanguageBottomSheetDialogFragment.newInstance()
                languageBottom.callback = object : TranslateLanguageBottomSheetDialogFragment.Callback {
                    override fun onLanguageClick(language: TranslateManager.Language) {
                        targetLangTv.text = language.nameInCurrentLanguage
                    }
                }
                languageBottom.showNow(parentFragmentManager, TranslateLanguageBottomSheetDialogFragment.TAG)
            }
        }
    }

    private fun showLanguageAlert() {
        val choice = resources.getStringArray(R.array.language_names)
        choice[0] = getString(R.string.Follow_system)
        val selectItem = getLanguagePos()
        var newSelectItem = selectItem
        alertDialogBuilder()
            .setTitle(R.string.Language)
            .setSingleChoiceItems(choice, selectItem) { _, which ->
                newSelectItem = which
            }
            .setPositiveButton(R.string.OK) { dialog, _ ->
                if (newSelectItem != selectItem) {
                    if (newSelectItem == POS_FOLLOW_SYSTEM) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    } else {
                        val selectedLang = when (newSelectItem) {
                            POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.language
                            POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.language
                            POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.language
                            POS_RUSSIAN -> Constants.Locale.Russian.Language
                            POS_INDONESIA -> Constants.Locale.Indonesian.Language
                            POS_Malay -> Constants.Locale.Malay.Language
                            POS_Spanish -> Constants.Locale.Spanish.Language
                            else -> Locale.US.language
                        }
                        val selectedCountry = when (newSelectItem) {
                            POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                            POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.country
                            POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.country
                            POS_RUSSIAN -> Constants.Locale.Russian.Country
                            POS_INDONESIA -> Constants.Locale.Indonesian.Country
                            POS_Malay -> Constants.Locale.Malay.Country
                            POS_Spanish -> Constants.Locale.Spanish.Country
                            else -> Locale.US.country
                        }
                        val newLocale = Locale(selectedLang, selectedCountry)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(newLocale))
                    }
                    TimeCache.singleton.evictAll()
                    requireActivity().onBackPressed()
                    requireActivity().recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

fun getLanguagePos() = when (getLanguage()) {
    "zh" -> {
        if (getLocaleString() in Constants.Locale.TraditionalChinese.localeStrings) {
            AppearanceFragment.POS_TRADITIONAL_CHINESE
        } else {
            AppearanceFragment.POS_SIMPLIFY_CHINESE
        }
    }
    Locale.ENGLISH.language -> {
        AppearanceFragment.POS_ENGLISH
    }
    Locale.JAPANESE.language -> {
        AppearanceFragment.POS_SIMPLIFY_JAPANESE
    }
    Constants.Locale.Russian.Language -> {
        AppearanceFragment.POS_RUSSIAN
    }
    Constants.Locale.Indonesian.Language -> {
        AppearanceFragment.POS_INDONESIA
    }
    Constants.Locale.Malay.Language -> {
        AppearanceFragment.POS_Malay
    }
    Constants.Locale.Spanish.Language -> {
        AppearanceFragment.POS_Spanish
    }
    else -> {
        AppearanceFragment.POS_FOLLOW_SYSTEM
    }
}

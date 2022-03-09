package one.mixin.android.ui.setting

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAppearanceBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.textColorResource
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.TimeCache
import one.mixin.android.util.getLanguage
import one.mixin.android.util.getLocaleString
import one.mixin.android.util.isFollowSystem
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.theme.Coordinate
import one.mixin.android.widget.theme.NightModeSwitch.Companion.ANIM_DURATION
import one.mixin.android.widget.theme.ThemeActivity
import timber.log.Timber
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

        fun newInstance() = AppearanceFragment()
    }

    private val binding by viewBinding(FragmentAppearanceBinding::bind)
    private val androidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val androidS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val systemNightMode by lazy {
        MixinApplication.appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    private val localNightModeState by lazy {
        defaultSharedPreferences.getInt(
            Constants.Theme.THEME_CURRENT_ID,
            if (!androidQ) {
                Constants.Theme.THEME_LIGHT_ID
            } else {
                Constants.Theme.THEME_AUTO_ID
            }
        )
    }
    private var lastNightModeState: Int? = null
    private var switchTask: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            nightModeRl.setOnClickListener {
                nightModeSwitch.switch()
            }
            nightModeTv.setText(R.string.Theme)
            nightModeSwitch.initState(localNightModeState)
            nightModeSwitch.setOnSwitchListener { state ->
                if (lastNightModeState == null) lastNightModeState = localNightModeState
                if (!isAdded || state == lastNightModeState) return@setOnSwitchListener
                Timber.e("$state $lastNightModeState")
                val currentNightMode = if (lastNightModeState == Constants.Theme.THEME_AUTO_ID) {
                    systemNightMode
                } else {
                    lastNightModeState == Constants.Theme.THEME_NIGHT_ID
                }
                lastNightModeState = state
                val targetNightMode = if (state == Constants.Theme.THEME_AUTO_ID) {
                    systemNightMode
                } else {
                    state == Constants.Theme.THEME_NIGHT_ID
                }
                Timber.e("targetNightMode:$targetNightMode currentNightMode:$currentNightMode")
                switchTask = if (lastNightModeState != localNightModeState) {
                    {
                        Timber.e("switchTask")
                        AppCompatDelegate.setDefaultNightMode(
                            when (state) {
                                Constants.Theme.THEME_LIGHT_ID -> AppCompatDelegate.MODE_NIGHT_NO
                                Constants.Theme.THEME_NIGHT_ID -> AppCompatDelegate.MODE_NIGHT_YES
                                Constants.Theme.THEME_AUTO_ID -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                else -> AppCompatDelegate.MODE_NIGHT_NO
                            }
                        )
                        defaultSharedPreferences.putInt(Constants.Theme.THEME_CURRENT_ID, state)
                        requireActivity().recreate()
                    }
                } else {
                    null
                }
                (requireActivity() as ThemeActivity).run {
                    changeTheme(
                        getViewCoordinates(nightModeSwitch),
                        ANIM_DURATION,
                        !targetNightMode
                    ) {
                        if (androidS) {
                            switchTask?.invoke()
                        }
                    }
                }
                syncTheme(targetNightMode)
            }
            val languageNames = resources.getStringArray(R.array.language_names)
            languageDescTv.text = if (isFollowSystem()) {
                getString(R.string.Follow_system)
            } else {
                languageNames[getLanguagePos()]
            }
            languageRl.setOnClickListener { showLanguageAlert() }
            currentTv.text = getString(
                R.string.wallet_setting_currency_desc,
                Session.getFiatCurrency(),
                Fiats.getSymbol()
            )
            currencyRl.setOnClickListener {
                val currencyBottom = CurrencyBottomSheetDialogFragment.newInstance()
                currencyBottom.callback = object : CurrencyBottomSheetDialogFragment.Callback {
                    override fun onCurrencyClick(currency: Currency) {
                        currentTv.text = getString(
                            R.string.wallet_setting_currency_desc,
                            currency.name,
                            currency.symbol
                        )
                    }
                }
                currencyBottom.showNow(parentFragmentManager, CurrencyBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun onDestroy() {
        if (!androidS) {
            switchTask?.invoke()
        }
        super.onDestroy()
    }

    private fun syncTheme(isNight: Boolean) {
        binding.apply {
            val bgWindow = if (isNight) {
                R.color.bgWindowNight
            } else {
                R.color.bgWindow
            }
            val bgColor = if (isNight) {
                R.color.bgWhiteNight
            } else {
                R.color.bgWhite
            }
            val iconColor = if (isNight) {
                R.color.colorIconNight
            } else {
                R.color.colorIcon
            }
            val textPrimary = if (isNight) {
                R.color.textPrimaryNight
            } else {
                R.color.textPrimary
            }
            val textMinor = if (isNight) {
                R.color.textMinorNight
            } else {
                R.color.textMinor
            }
            root.setBackgroundResource(bgWindow)
            titleView.root.setBackgroundResource(bgColor)
            titleView.leftIb.setColorFilter(requireContext().getColor(iconColor))
            titleView.titleTv.textColorResource = textPrimary
            nightModeRl.setBackgroundResource(bgColor)
            languageRl.setBackgroundResource(bgColor)
            currencyRl.setBackgroundResource(bgColor)
            nightModeTv.textColorResource = textPrimary
            languageTv.textColorResource = textPrimary
            languageDescTv.textColorResource = textMinor
            currencyTv.textColorResource = textPrimary
            currentTv.textColorResource = textMinor

            Timber.e("isNight $isNight")
            val window = requireActivity().window
            SystemUIManager.lightUI(window, !isNight)
            SystemUIManager.setSystemUiColor(window, requireContext().getColor(bgColor))
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
                            else -> Locale.US.language
                        }
                        val selectedCountry = when (newSelectItem) {
                            POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                            POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.country
                            POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.country
                            POS_RUSSIAN -> Constants.Locale.Russian.Country
                            POS_INDONESIA -> Constants.Locale.Indonesian.Country
                            POS_Malay -> Constants.Locale.Malay.Country
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

    private fun getViewCoordinates(view: View): Coordinate {
        return Coordinate(
            getRelativeLeft(view) + view.width / 2,
            getRelativeTop(view) + view.height / 2
        )
    }

    private fun getRelativeLeft(myView: View): Int {
        return if ((myView.parent as View).id == ThemeActivity.ROOT_ID) {
            myView.left
        } else {
            myView.left + getRelativeLeft(
                myView.parent as View
            )
        }
    }

    private fun getRelativeTop(myView: View): Int {
        return if ((myView.parent as View).id == ThemeActivity.ROOT_ID) {
            myView.top
        } else {
            myView.top + getRelativeTop(
                myView.parent as View
            )
        }
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
    else -> {
        AppearanceFragment.POS_FOLLOW_SYSTEM
    }
}

package one.mixin.android.ui.setting

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAppearanceBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.singleChoice
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.TimeCache
import one.mixin.android.util.language.Lingver
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
        const val POS_INDONESIA = 3
        const val POS_Malay = 4

        fun newInstance() = AppearanceFragment()
    }

    private val binding by viewBinding(FragmentAppearanceBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            nightModeTv.setText(R.string.Theme)
            val currentId = defaultSharedPreferences.getInt(
                Constants.Theme.THEME_CURRENT_ID,
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Constants.Theme.THEME_DEFAULT_ID
                } else {
                    Constants.Theme.THEME_AUTO_ID
                }
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
                    currentId
                ) { _, index ->
                    val changed = index != currentId
                    defaultSharedPreferences.putInt(Constants.Theme.THEME_CURRENT_ID, index)
                    AppCompatDelegate.setDefaultNightMode(
                        when (index) {
                            Constants.Theme.THEME_DEFAULT_ID -> AppCompatDelegate.MODE_NIGHT_NO
                            Constants.Theme.THEME_NIGHT_ID -> AppCompatDelegate.MODE_NIGHT_YES
                            Constants.Theme.THEME_AUTO_ID -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            else -> AppCompatDelegate.MODE_NIGHT_NO
                        }
                    )
                    if (changed) {
                        requireActivity().onBackPressed()
                        requireActivity().recreate()
                    }
                }
            }
            val language = Lingver.getInstance().getLanguage()
            val languageNames = resources.getStringArray(R.array.language_names)
            languageDescTv.text = if (Lingver.getInstance().isFollowingSystemLocale()) {
                getString(R.string.follow_system)
            } else {
                when (language) {
                    Locale.SIMPLIFIED_CHINESE.language -> {
                        languageNames[POS_SIMPLIFY_CHINESE]
                    }
                    Constants.Locale.Indonesian.Language -> {
                        languageNames[POS_INDONESIA]
                    }
                    Constants.Locale.Malay.Language -> {
                        languageNames[POS_Malay]
                    }
                    else -> {
                        languageNames[POS_ENGLISH]
                    }
                }
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
        }
    }

    private fun showLanguageAlert() {
        val choice = resources.getStringArray(R.array.language_names)
        choice[0] = getString(R.string.follow_system)
        val selectItem = if (Lingver.getInstance().isFollowingSystemLocale()) {
            POS_FOLLOW_SYSTEM
        } else {
            when (Lingver.getInstance().getLanguage()) {
                Locale.SIMPLIFIED_CHINESE.language -> {
                    POS_SIMPLIFY_CHINESE
                }
                Constants.Locale.Indonesian.Language -> {
                    POS_INDONESIA
                }
                Constants.Locale.Malay.Language -> {
                    POS_Malay
                }
                else -> {
                    POS_ENGLISH
                }
            }
        }
        var newSelectItem = selectItem
        alertDialogBuilder()
            .setTitle(R.string.Language)
            .setSingleChoiceItems(choice, selectItem) { _, which ->
                newSelectItem = which
            }
            .setPositiveButton(R.string.OK) { dialog, _ ->
                if (newSelectItem != selectItem) {
                    if (newSelectItem == POS_FOLLOW_SYSTEM) {
                        Lingver.getInstance().setFollowSystemLocale(requireContext())
                    } else {
                        val selectedLang = when (newSelectItem) {
                            POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.language
                            POS_INDONESIA -> Constants.Locale.Indonesian.Language
                            POS_Malay -> Constants.Locale.Malay.Language
                            else -> Locale.US.language
                        }
                        val selectedCountry = when (newSelectItem) {
                            POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                            POS_INDONESIA -> Constants.Locale.Indonesian.Country
                            POS_Malay -> Constants.Locale.Malay.Country
                            else -> Locale.US.country
                        }
                        val newLocale = Locale(selectedLang, selectedCountry)
                        Lingver.getInstance().setLocale(requireContext(), newLocale)
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

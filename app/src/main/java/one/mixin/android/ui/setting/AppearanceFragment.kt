package one.mixin.android.ui.setting

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_appearance.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.singleChoice
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.session.Session
import one.mixin.android.util.TimeCache
import one.mixin.android.util.language.Lingver
import one.mixin.android.vo.Fiats
import java.util.Locale

@AndroidEntryPoint
class AppearanceFragment : BaseFragment() {
    companion object {
        const val TAG = "AppearanceFragment"

        fun newInstance() = AppearanceFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_appearance, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        night_mode_tv.setText(R.string.setting_theme)
        val currentId = defaultSharedPreferences.getInt(
            Constants.Theme.THEME_CURRENT_ID,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Constants.Theme.THEME_DEFAULT_ID
            } else {
                Constants.Theme.THEME_AUTO_ID
            }
        )
        night_mode_desc_tv.text = resources.getStringArray(R.array.setting_night_array_oreo)[currentId]
        night_mode_rl.setOnClickListener {
            singleChoice(
                resources.getString(R.string.setting_theme),
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
        language_desc_tv.text = getString(
            if (Lingver.getInstance().isFollowingSystemLocale()) {
                R.string.follow_system
            } else {
                when (language) {
                    Locale.SIMPLIFIED_CHINESE.language -> {
                        R.string.simplified_chinese
                    }
                    Locale.JAPANESE.language -> {
                        R.string.japanese
                    }
                    else -> {
                        R.string.english
                    }
                }
            }
        )
        language_rl.setOnClickListener { showLanguageAlert() }
        current_tv.text = getString(R.string.wallet_setting_currency_desc, Session.getFiatCurrency(), Fiats.getSymbol())
        currency_rl.setOnClickListener {
            val currencyBottom = CurrencyBottomSheetDialogFragment.newInstance()
            currencyBottom.callback = object : CurrencyBottomSheetDialogFragment.Callback {
                override fun onCurrencyClick(currency: Currency) {
                    current_tv?.text = getString(R.string.wallet_setting_currency_desc, currency.name, currency.symbol)
                }
            }
            currencyBottom.showNow(parentFragmentManager, CurrencyBottomSheetDialogFragment.TAG)
        }
    }

    private fun showLanguageAlert() {
        val choice = resources.getStringArray(R.array.language_names)
        val selectItem = if (Lingver.getInstance().isFollowingSystemLocale()) {
            0
        } else {
            when (Lingver.getInstance().getLanguage()) {
                Locale.SIMPLIFIED_CHINESE.language -> {
                    2
                }
                Locale.JAPANESE.language -> {
                    3
                }
                else -> {
                    1
                }
            }
        }
        var newSelectItem = selectItem
        alertDialogBuilder()
            .setTitle(R.string.language)
            .setSingleChoiceItems(choice, selectItem) { _, which ->
                newSelectItem = which
            }
            .setPositiveButton(R.string.group_ok) { dialog, _ ->
                if (newSelectItem != selectItem) {
                    if (newSelectItem == 0) {
                        Lingver.getInstance().setFollowSystemLocale(requireContext())
                    } else {
                        val selectedLang = when (newSelectItem) {
                            2 -> Locale.SIMPLIFIED_CHINESE.language
                            3 -> Locale.JAPANESE.language
                            else -> Locale.US.language
                        }
                        val selectedCountry = when (newSelectItem) {
                            2 -> Locale.SIMPLIFIED_CHINESE.country
                            3 -> Locale.JAPANESE.country
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
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

package one.mixin.android.ui.wallet

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_deposit_key.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean

abstract class DepositFragment : Fragment() {

    protected abstract fun getTips(): String

    override fun onStop() {
        super.onStop()
        title.removeCallbacks(showTipRunnable)
    }

    protected fun showTip() {
        val showTip = requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_SHOW_DEPOSIT_TIP, true)
        if (!showTip) return
        title.postDelayed(showTipRunnable, 3000)
    }

    private val showTipRunnable = Runnable {
        val builder = AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
            .setMessage(getTips())
        val firstShow = requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_FIRST_SHOW_DEPOSIT, true)
        if (firstShow) {
            requireContext().defaultSharedPreferences.putBoolean(Constants.Account.PREF_FIRST_SHOW_DEPOSIT, false)
        } else {
            builder.setNegativeButton(R.string.dont_remind) { dialog, _ ->
                requireContext().defaultSharedPreferences.putBoolean(Constants.Account.PREF_SHOW_DEPOSIT_TIP, false)
                dialog.dismiss()
            }
        }
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }.show()
    }
}
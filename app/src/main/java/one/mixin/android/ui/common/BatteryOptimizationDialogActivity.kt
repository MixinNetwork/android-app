package one.mixin.android.ui.common

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.handleIgnoreBatteryOptimization
import one.mixin.android.session.Session
import one.mixin.android.util.RomUtil
import timber.log.Timber

class BatteryOptimizationDialogActivity : BaseActivity() {
    companion object {
        const val ARGS_NEW_TASK = "args_new_task"

        fun show(
            context: Context,
            newTask: Boolean = false,
        ) {
            if (!Session.hasSafe()) return
            Intent(context, BatteryOptimizationDialogActivity::class.java).apply {
                putExtra(ARGS_NEW_TASK, newTask)
                if (newTask) {
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val newTask = intent.getBooleanExtra(ARGS_NEW_TASK, false)
        alertDialogBuilder()
            .setMessage(
                replaceTags(
                    getString(
                        if (RomUtil.isOneUi) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                R.string.setting_battery_optimize_title_one_ui_above_s
                            } else {
                                R.string.setting_battery_optimize_title_one_ui_below_s
                            }
                        } else {
                            R.string.setting_battery_optimize_title
                        },
                    ),
                ),
            )
            .setCancelable(false)
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setPositiveButton(R.string.Go_settings) { dialog, _ ->
                handleIgnoreBatteryOptimization(newTask)
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun replaceTags(str: String): SpannableStringBuilder {
        try {
            var start: Int
            var end: Int
            val stringBuilder = StringBuilder(str)
            val bolds: ArrayList<Int> = ArrayList()
            while (stringBuilder.indexOf("<b>").also { start = it } != -1) {
                stringBuilder.replace(start, start + 3, "")
                end = stringBuilder.indexOf("</b>")
                if (end == -1) {
                    end = stringBuilder.indexOf("<b>")
                }
                stringBuilder.replace(end, end + 4, "")
                bolds.add(start)
                bolds.add(end)
            }
            while (stringBuilder.indexOf("**").also { start = it } != -1) {
                stringBuilder.replace(start, start + 2, "")
                end = stringBuilder.indexOf("**")
                if (end >= 0) {
                    stringBuilder.replace(end, end + 2, "")
                    bolds.add(start)
                    bolds.add(end)
                }
            }

            val spannableStringBuilder = SpannableStringBuilder(stringBuilder)
            for (a in 0 until bolds.count() / 2) {
                spannableStringBuilder.setSpan(StyleSpan(Typeface.BOLD), bolds[a * 2], bolds[a * 2 + 1], Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
            return spannableStringBuilder
        } catch (e: Exception) {
            Timber.e(e)
        }
        return SpannableStringBuilder(str)
    }
}

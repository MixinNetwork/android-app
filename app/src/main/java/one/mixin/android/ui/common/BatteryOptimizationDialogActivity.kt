package one.mixin.android.ui.common

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.handleIgnoreBatteryOptimization

class BatteryOptimizationDialogActivity : BaseActivity() {
    companion object {
        const val ARGS_NEW_TASK = "args_new_task"

        fun show(context: Context, newTask: Boolean = false) {
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
            .setMessage(getString(R.string.setting_battery_optimize_title))
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
}

package one.mixin.android.ui.common

import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences

open class AppAuthActivity : BaseActivity() {

    private var appAuthDialog: DialogFragment? = null

    override fun onStart() {
        super.onStart()
        if (MixinApplication.get().activityReferences == 1) {
            MixinApplication.get().appAuthShown = checkAndShowAppAuth()
        }
    }

    override fun onResume() {
        super.onResume()
        disableScreenshotsIfAppAuthShown()
    }

    override fun onStop() {
        super.onStop()
        appAuthDialog?.dismissAllowingStateLoss()
        appAuthDialog = null
    }

    private fun checkAndShowAppAuth(): Boolean {
        if (ignoreCheck()) return false

        val appAuth = defaultSharedPreferences.getInt(Constants.Account.PREF_APP_AUTH, -1)
        if (appAuth != -1) {
            if (appAuth == 0) {
                if (appAuthDialog == null) {
                    appAuthDialog = AppAuthDialogFragment()
                }
                appAuthDialog?.show(supportFragmentManager, AppAuthDialogFragment.TAG)
                return true
            } else {
                val enterBackground =
                    defaultSharedPreferences.getLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, 0)
                val now = System.currentTimeMillis()
                val offset = if (appAuth == 1) {
                    Constants.INTERVAL_1_MIN
                } else {
                    Constants.INTERVAL_30_MINS
                }
                if (now - enterBackground > offset) {
                    if (appAuthDialog == null) {
                        appAuthDialog = AppAuthDialogFragment()
                    }
                    appAuthDialog?.show(supportFragmentManager, AppAuthDialogFragment.TAG)
                    return true
                }
            }
        }
        return false
    }

    private fun disableScreenshotsIfAppAuthShown() {
        if (MixinApplication.get().appAuthShown) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    protected open fun ignoreCheck() = false
}

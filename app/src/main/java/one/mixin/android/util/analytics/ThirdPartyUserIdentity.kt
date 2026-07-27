package one.mixin.android.util.analytics

import com.appsflyer.AppsFlyerLib
import com.bugsnag.android.Bugsnag
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.extension.sha256
import one.mixin.android.extension.toHex
import one.mixin.android.vo.Account

object ThirdPartyUserIdentity {
    fun hashedUserId(userId: String): String = userId.sha256().toHex()

    fun appsFlyerCustomerUserId(userId: String): String = hashedUserId(userId)

    fun setUser(account: Account) {
        val userId = hashedUserId(account.userId)
        Bugsnag.setUser(userId, account.identityNumber, account.fullName)
        FirebaseCrashlytics.getInstance().setUserId(userId)
        FirebaseAnalytics.getInstance(MixinApplication.get()).setUserId(userId)
        if (BuildConfig.APPSFLYER_DEV_KEY.isNotBlank()) {
            AppsFlyerLib.getInstance().setCustomerUserId(userId)
        }
    }

    fun clearUser() {
        Bugsnag.setUser(null, null, null)
        FirebaseCrashlytics.getInstance().setUserId("")
        FirebaseAnalytics.getInstance(MixinApplication.get()).setUserId(null)
        if (BuildConfig.APPSFLYER_DEV_KEY.isNotBlank()) {
            AppsFlyerLib.getInstance().setCustomerUserId("")
        }
    }
}

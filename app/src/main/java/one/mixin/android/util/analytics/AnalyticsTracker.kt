package one.mixin.android.util.analytics

import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import one.mixin.android.MixinApplication
import one.mixin.android.vo.Account
import one.mixin.android.vo.Plan

object AnalyticsTracker {
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(MixinApplication.get()) }

    fun trackSignUpStart(method: String) {
        val params = Bundle().apply {
            putString("method", method)
        }
        firebaseAnalytics.logEvent("sign_up_start", params)
    }

    fun trackSignUpFullName() {
        firebaseAnalytics.logEvent("sign_up_fullname", null)
    }

    fun trackSignUpSetPin() {
        firebaseAnalytics.logEvent("sign_up_set_pin", null)
    }

    fun trackLoginStart() {
        firebaseAnalytics.logEvent("login_start", null)
    }

    fun trackLoginRestore(method: String) {
        val params = Bundle().apply {
            putString("method", method)
        }
        firebaseAnalytics.logEvent("login_restore", params)
    }

    fun trackLoginVerifyPin(method: String) {
        val params = Bundle().apply {
            putString("method", method)
        }
        firebaseAnalytics.logEvent("login_verify_pin", params)
    }

    fun trackSwapStart(source: String, entrance: String) {
        val params = Bundle().apply {
            putString("source", source)
            putString("entrance", entrance)
        }
        firebaseAnalytics.logEvent("swap_start", params)
    }


    fun trackSwapCoinSwitch(method: String) {
        val params = Bundle().apply {
            putString("method", method)
        }
        firebaseAnalytics.logEvent("swap_coin_switch", params)
    }

    object SwapCoinSwitchMethod {
        const val RECENT_CLICK = "recent_click"
        const val SEARCH_ITEM_CLICK = "search_item_click"
        const val ALL_ITEM_CLICK = "all_item_click"
        const val CHAIN_ITEM_CLICK = "chain_item_click"
    }

    fun trackSwapQuote(result: String) {
        val params = Bundle().apply {
            putString("result", result)
        }
        firebaseAnalytics.logEvent("swap_quote", params)
    }

    object SwapQuoteResult {
        const val SUCCESS = "success"
        const val FAILURE = "failure"
    }

    fun trackSwapPreview() {
        firebaseAnalytics.logEvent("swap_preview", null)
    }

    fun trackSwapSend() {
        firebaseAnalytics.logEvent("swap_send", null)
    }

    fun setHasEmergencyContact(account: Account) {
        firebaseAnalytics.setUserProperty("has_emergency_contact", account.hasEmergencyContact.toString())
    }

    fun setMembership(account: Account) {
        firebaseAnalytics.setUserProperty("membership", account.membership?.toString() ?: Plan.None.value)
    }

    fun setNotificationAuthStatus(context: Context) {
        firebaseAnalytics.setUserProperty(
            "notification_auth_status", if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                "authorized"
            } else {
                "denied"
            }
        )
    }

    fun setAssetLevel(totalUsd: Int) {
        val level = when {
            totalUsd >= 10000 -> "v10000"
            totalUsd >= 1000 -> "v1000"
            totalUsd >= 100 -> "v100"
            totalUsd >= 1 -> "v1"
            else -> "v0"
        }
        firebaseAnalytics.setUserProperty("asset_level", level)
    }
}

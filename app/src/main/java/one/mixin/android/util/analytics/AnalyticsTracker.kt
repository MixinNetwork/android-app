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

    fun trackSignUpStart(type: String) {
        val params = Bundle().apply {
            putString("type", type)
        }
        firebaseAnalytics.logEvent("sign_up_start", params)
    }

    fun trackSignUpCaptcha(type: String) {
        val params = Bundle().apply {
            putString("type", type)
        }
        firebaseAnalytics.logEvent("sign_up_captcha", params)
    }

    fun trackSignUpSmsVerify() {
        firebaseAnalytics.logEvent("sign_up_sms_verify", null)
    }

    fun trackSignUpFullName() {
        firebaseAnalytics.logEvent("sign_up_fullname", null)
    }

    fun trackSignalInit() {
        firebaseAnalytics.logEvent("signal_init", null)
    }

    fun trackSignUpPinSet() {
        firebaseAnalytics.logEvent("sign_up_pin_set", null)
    }

    fun trackSignUpEnd() {
        firebaseAnalytics.logEvent("sign_up_end", null)
    }

    fun trackLoginStart() {
        firebaseAnalytics.logEvent("login_start", null)
    }

    fun trackLoginMnemonicPhrase() {
        firebaseAnalytics.logEvent("login_mnemonic_phrase", null)
    }

    fun trackLoginCaptcha(type: String) {
        val params = Bundle().apply {
            putString("type", type)
        }
        firebaseAnalytics.logEvent("login_captcha", params)
    }

    fun trackLoginSmsVerify() {
        firebaseAnalytics.logEvent("login_sms_verify", null)
    }

    fun trackLoginRestore(type: String) {
        val params = Bundle().apply {
            putString("type", type)
        }
        firebaseAnalytics.logEvent("login_restore", params)
    }

    fun trackLoginPinVerify(type: String) {
        val params = Bundle().apply {
            putString("type", type)
        }
        firebaseAnalytics.logEvent("login_pin_verify", params)
    }

    fun trackLoginEnd() {
        firebaseAnalytics.logEvent("login_end", null)
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

    fun trackTradeTokenSelect(method: String) {
        val params = Bundle().apply {
            putString("method", method)
        }
        firebaseAnalytics.logEvent("trade_token_select", params)
    }

    object TradeTokenSelectMethod {
        const val RECENT_CLICK = "recent_click"
        const val SEARCH_ITEM_CLICK = "search_item_click"
        const val ALL_ITEM_CLICK = "all_item_click"
        const val CHAIN_ITEM_CLICK = "chain_item_click"
    }

    fun trackTradeQuote(result: String, type: String, reason: String? = null) {
        val params = Bundle().apply {
            putString("result", result)
            putString("type", type)
            reason?.let { putString("reason", it) }
        }
        firebaseAnalytics.logEvent("trade_quote", params)
    }

    object TradeQuoteResult {
        const val SUCCESS = "success"
        const val FAILURE = "failure"
    }

    object TradeQuoteType {
        const val SWAP = "swap"
        const val LIMIT = "limit"
        const val RECURRING = "recurring"
    }

    object TradeQuoteReason {
        const val INVALID_AMOUNT = "invalid_amount"
        const val NO_AVAILABLE_QUOTE = "no_available_quote"
        const val OTHER = "other"
    }

    fun trackTradePreview() {
        firebaseAnalytics.logEvent("trade_preview", null)
    }

    fun trackTradeEnd(wallet: String, tradeAssetLevel: String) {
        val params = Bundle().apply {
            putString("wallet", wallet)
            putString("trade_asset_level", tradeAssetLevel)
        }
        firebaseAnalytics.logEvent("trade_end", params)
    }

    fun getTradeAssetLevel(amountUsd: Double): String {
        return when {
            amountUsd >= 1000000 -> "v1,000,000"
            amountUsd >= 100000 -> "v100,000"
            amountUsd >= 10000 -> "v10,000"
            amountUsd >= 1000 -> "v1,000"
            amountUsd >= 100 -> "v100"
            else -> "v1"
        }
    }

    fun trackTradeTransactions() {
        firebaseAnalytics.logEvent("trade_transactions", null)
    }

    fun trackTradeDetail() {
        firebaseAnalytics.logEvent("trade_detail", null)
    }
}

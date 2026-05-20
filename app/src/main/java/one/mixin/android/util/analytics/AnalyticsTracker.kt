package one.mixin.android.util.analytics

import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics
import one.mixin.android.MixinApplication
import one.mixin.android.vo.Account
import one.mixin.android.vo.Plan
import java.math.BigDecimal

object AnalyticsTracker {
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(MixinApplication.get()) }

    private fun logEvent(name: String) {
        firebaseAnalytics.logEvent(name, null)
    }

    private inline fun logEvent(name: String, block: Bundle.() -> Unit) {
        firebaseAnalytics.logEvent(name, Bundle().apply(block))
    }

    fun trackSignUpStart(source: String) {
        logEvent("sign_up_start") {
            putString("source", source)
        }
    }


    fun trackSignUpCaptcha() {
        logEvent("sign_up_captcha")
    }

    fun trackSignUpSmsVerify() {
        logEvent("sign_up_sms_verify")
    }

    fun trackSignUpFullName() {
        logEvent("sign_up_fullname")
    }

    fun trackSignUpSignalInit() {
        logEvent("sign_up_signal_init")
    }

    fun trackSignUpPinSet() {
        logEvent("sign_up_pin_set")
    }

    fun trackSignUpPinQuiz() {
        logEvent("sign_up_pin_quiz")
    }

    fun trackSignUpEnd() {
        logEvent("sign_up_end")
    }

    fun trackLoginStart() {
        logEvent("login_start")
    }

    fun trackLoginSmsSendConfirmed() {
        logEvent("login_sms_send_confirmed")
    }

    fun trackLoginMnemonicPhrase() {
        logEvent("login_mnemonic_phrase")
    }

    fun trackLoginCaptcha(type: String) {
        logEvent("login_captcha") {
            putString("type", type)
        }
    }

    fun trackLoginSmsVerify() {
        logEvent("login_sms_verify")
    }

    fun trackLoginSignalInit() {
        logEvent("login_signal_init")
    }

    fun trackLoginRestore(type: String) {
        logEvent("login_restore") {
            putString("type", type)
        }
    }

    fun trackLoginPinVerify(type: String) {
        logEvent("login_pin_verify") {
            putString("type", type)
        }
    }

    fun trackLoginEnd() {
        logEvent("login_end")
    }

    fun setHasRecoveryContact(account: Account) {
        firebaseAnalytics.setUserProperty("has_recovery_contact", account.hasEmergencyContact.toString())
    }

    fun setMembership(account: Account) {
        firebaseAnalytics.setUserProperty("membership", account.membership?.toString() ?: Plan.None.value)
    }

    fun setNotificationAuthStatus(context: Context) {
        val status = if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            "authorized"
        } else {
            "denied"
        }
        firebaseAnalytics.setUserProperty("notification_auth_status", status)
    }

    fun setAssetLevel(totalUsd: Int) {
        val level = when {
            totalUsd >= 10000000 -> "v10,000,000"
            totalUsd >= 1000000 -> "v1,000,000"
            totalUsd >= 100000 -> "v100,000"
            totalUsd >= 10000 -> "v10,000"
            totalUsd >= 1000 -> "v1,000"
            totalUsd >= 100 -> "v100"
            totalUsd >= 1 -> "v1"
            else -> "v0"
        }
        firebaseAnalytics.setUserProperty("asset_level", level)
    }

    fun trackTradeTokenSelect(method: String) {
        logEvent("trade_token_select") {
            putString("method", method)
        }
    }

    object TradeTokenSelectMethod {
        const val RECENT_CLICK = "recent_click"
        const val SEARCH_ITEM_CLICK = "search_item_click"
        const val ALL_ITEM_CLICK = "all_item_click"
        const val CHAIN_ITEM_CLICK = "chain_item_click"
    }

    fun trackTradeQuote(result: String, type: String, reason: String? = null) {
        logEvent("trade_quote") {
            putString("result", result)
            putString("type", type)
            reason?.let { putString("reason", it) }
        }
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

    fun trackTradeStart(wallet: String, source: String) {
        logEvent("trade_start") {
            putString("wallet", wallet)
            putString("source", source)
        }
    }

    object TradeWallet {
        const val MAIN = "main"
        const val WEB3 = "web3"
    }

    object TradeSource {
        const val WALLET_HOME = "wallet_home"
        const val MARKET_DETAIL = "market_detail"
        const val APP_CARD = "app_card"
        const val TRADE_DETAIL = "trade_detail"
        const val SCHEMA = "scheme"
        const val ASSET_DETAIL = "asset_detail"
        const val EXPLORE = "explore"
        const val FEE = "fee"
        const val BALANCE = "balance"
    }

    object SpotTradeType {
        const val SIMPLE = "simple"
        const val ADVANCED = "advanced"
        const val PERPETUAL = "perpetual"
    }

    object SpotTokenType {
        const val SEND = "send"
        const val RECEIVE = "receive"
    }

    object SpotGuideSource {
        const val FIRST_GUIDE = "first_guide"
        const val MENU = "menu"
    }

    object SpotExpiryMethod {
        const val NEVER = "never"
        const val MIN_10 = "10m"
        const val HOUR_1 = "1h"
        const val DAY_1 = "1d"
        const val DAY_3 = "3d"
        const val WEEK_1 = "1w"
        const val MONTH_1 = "1m"
        const val YEAR_1 = "1y"
    }

    object CustomerServiceSource {
        const val SIGN_UP = "sign_up"
        const val SIGN_UP_MNEMONIC_PHRASE = "sign_up_mnemonic_phrase"
        const val SIGN_UP_MNEMONIC_PHRASE_CREATING = "sign_up_mnemonic_phrase_creating"
        const val SIGN_UP_PHONE_NUMBER = "sign_up_phone_number"
        const val SIGN_UP_SMS_VERIFY = "sign_up_sms_verify"
        const val SIGN_UP_FULL_NAME = "sign_up_full_name"
        const val LOGIN_PHONE_NUMER = "login_phone_numer"
        const val LOGIN_MNEMONIC_PHRASE = "login_mnemonic_phrase"
        const val LOGIN_MNEMONIC_PHRASE_SIGNING = "login_mnemonic_phrase_signing"
        const val LOGIN_SMS_VERIFY = "login_sms_verify"
        const val LOGIN_PIN_VERIFY = "login_pin_verify"
        const val PHONE_NUMBER_ADD = "phone_number_add"
        const val PHONE_NUMBER_ADD_SMS_VERIFY = "phone_number_add_sms_verify"
        const val PHONE_NUMBER_CHANGE = "phone_number_change"
        const val PHONE_NUMBER_CHANGE_SMS_VERIFY = "phone_number_change_sms_verify"
        const val TRADE_HOME = "trade_home"
        const val TRADE_DETAIL = "trade_detail"
        const val DEPOSIT = "deposit"
        const val SEND_RECIPIENT = "send_recipient"
        const val SEND_AMOUNT = "send_amount"
        const val ADDRESS_BOOK_ADD_ADDRESS = "address_book_add_address"
        const val ADDRESS_BOOK_ADD_MEMO = "address_book_add_memo"
        const val ADDRESS_BOOK_ADD_TAG = "address_book_add_tag"
        const val ADDRESS_BOOK_ADD_LABEL = "address_book_add_label"
        const val RECOVERY_KIT = "recovery_kit"
        const val MARKET_DETAIL = "market_detail"
        const val PRICE_ALERT_ADD = "price_alert_add"
        const val MORE_BOTS = "more_bots"
        const val TRANSACTION_DETAIL = "transaction_detail"
        const val ASSET_DETAIL = "asset_detail"
        const val COLLECTIBLE_DETAIL = "collectible_detail"
        const val PERPS_OPEN_POSITION = "perps_open_position"
        const val PERPS_MARKET_DETAIL = "perps_market_detail"
        const val PERPS_ALL_POSITIONS = "perps_all_positions"
        const val PERPS_ACTIVITY_DETAIL = "perps_activity_detail"
        const val TRADE_PERPS_HOME_MENU = "trade_perps_home_menu"
        const val TRADE_SIMPLE_HOME_MENU = "trade_simple_home_menu"
        const val TRADE_ADVANCED_HOME_MENU = "trade_advanced_home_menu"
        const val ADD_PHONE_NOTICE = "add_phone_notice"
        const val ADD_PHONE_VERIFY_PIN = "add_phone_verify_pin"
        const val ADD_PHONE_INPUT_PHONE = "add_phone_input_phone"
        const val ADD_PHONE_SMS_VERIFY = "add_phone_sms_verify"
    }

    object AddPhoneSource {
        const val SETTINGS = "settings"
        const val RECOVERY_KEY_GUIDE = "recovery_key_guide"
        const val BUY_GUIDE = "buy_guide"
    }

    object CaptchaType {
        const val GEETEST = "geetest"
        const val HCAPTCHA = "hcaptcha"
        const val RECAPTCHA = "recaptcha"
    }

    object BotSource {
        const val MORE_EXPLORE_DIALOG = "more_explore_dialog"
        const val MORE_EXPLORE_FAVORITE = "more_explore_favorite"
        const val CHAT_AVATAR_DIALOG = "chat_avatar_dialog"
        const val CHAT_BOTTOM_MENU = "chat_bottom_menu"
        const val CHAT_MORE_MENU = "chat_more_menu"
        const val CHAT_MESSAGE_CONTACT = "chat_message_contact"
        const val SEARCH_MAO_NAME = "search_mao_name"
        const val SEARCH_RECENT = "search_recent"
        const val SEARCH_KEY_CONTACT = "search_key_contact"
        const val SEARCH_KEY_CONVERSATION = "search_key_conversation"
        const val SEARCH_KEY_MESSAGE = "search_key_message"
        const val SCHEME = TradeSource.SCHEMA
    }

    object MarketSource {
        const val MORE_MARKET_CAP = "more_market_cap"
        const val MORE_FAVORITES = "more_favorites"
        const val MORE_SEARCH = "more_search"
        const val TOKEN_DETAIL = "token_detal"
        const val APP_CARD = "app_card"
        const val SCHEMA = "schema"
        const val MARKET_DETAIL = "market_detail"
        const val PRICE_ALERT_LIST = "price_alert_list"
    }

    object MarketShareType {
        const val SHARE_IMAGE = "share_image"
        const val COPY_LINK = "copy_link"
        const val SAVE_TO_ALBUM = "save_to_album"
    }

    object MarketAlertsType {
        const val ONE = "one"
        const val ALL = "all"
    }

    object PerpsSource {
        const val WALLET_HOME = TradeSource.WALLET_HOME
        const val MORE_EXPLORE = "more_explore"
        const val APP_CARD = TradeSource.APP_CARD
        const val FIRST_GUIDE = SpotGuideSource.FIRST_GUIDE
        const val PERPS_HOME_MENU = "perps_home_menu"
        const val PERPS_HOME_CARD = "perps_home_card"
        const val PERPS_DETAIL_CARD = "perps_detail_card"
        const val PERPS_OPEN_POSITION_SIZE = "perps_open_position_size"
        const val PERPS_MARKET_DETAIL = "perps_market_detail"
        const val PERPS_ALL_POSITIONS = "perps_all_positions"
        const val PERPS_ACTIVITY_DETAIL = "perps_activity_detail"
    }

    object PerpsDirection {
        const val LONG = "long"
        const val SHORT = "short"
    }

    fun trackTradePreview() {
        logEvent("trade_preview")
    }

    fun trackTradeEnd(wallet: String, amountValue: BigDecimal, price: String?) {
        val amountUsd = runCatching {
            val priceValue = price?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            amountValue.multiply(priceValue).toDouble()
        }.getOrDefault(0.0)
        
        val tradeAssetLevel = getTradeAssetLevel(amountUsd)
        
        logEvent("trade_end") {
            putString("wallet", wallet)
            putString("trade_asset_level", tradeAssetLevel)
        }
    }

    private fun getTradeAssetLevel(amountUsd: Double): String {
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
        logEvent("trade_transactions")
    }

    fun trackTradeDetail() {
        logEvent("trade_detail")
    }

    fun trackBuyStart(wallet: String, source: String) {
        logEvent("buy_start") {
            putString("wallet", wallet)
            putString("source", source)
        }
    }

    fun trackBuyTokenSelect() {
        logEvent("buy_token_select")
    }

    fun trackBuyFiatSelect() {
        logEvent("buy_fiat_select")
    }

    fun trackBuyPreview() {
        logEvent("buy_preview")
    }

    fun trackPerpsOpenPositionStart(direction: String, source: String) {
        logEvent("trade_perps_open_position_start") {
            putString("direction", direction)
            putString("source", source)
        }
    }

    fun trackPerpsMarginTokenSelect(chain: String?, assetSymbol: String?) {
        logEvent("trade_perps_margin_token_select") {
            putString("chain", chain)
            putString("asset_symbol", assetSymbol)
        }
    }

    fun trackPerpsAmountInputPercent(percent: String) {
        logEvent("trade_perps_amount_input_percent") {
            putString("percent", percent)
        }
    }

    fun trackPerpsAmountInputBalance() {
        logEvent("trade_perps_amount_input_balance")
    }

    fun trackPerpsLeverageSelect(leverage: String) {
        logEvent("trade_perps_leverage_select") {
            putString("leverage", leverage)
        }
    }

    fun trackPerpsPreview(leverage: String) {
        logEvent("trade_perps_preview") {
            putString("leverage", leverage)
        }
    }

    fun trackPerpsPreviewConfirm() {
        logEvent("trade_perps_preview_confirm")
    }

    fun trackPerpsPreviewCancel() {
        logEvent("trade_perps_preview_cancel")
    }

    fun trackPerpsOpenPositionEnd(leverage: Int, amountValue: BigDecimal, price: String?) {
        val amountUsd = runCatching {
            val priceValue = price?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            amountValue.multiply(priceValue).toDouble()
        }.getOrDefault(0.0)
        logEvent("trade_perps_open_position_end") {
            putString("leverage", leverage.toString())
            putString("trade_asset_level", getTradeAssetLevel(amountUsd))
        }
    }

    fun trackPerpsClosePositionStart() {
        logEvent("trade_perps_close_position_start")
    }

    fun trackPerpsClosePositionPreview() {
        logEvent("trade_perps_close_position_preview")
    }

    fun trackPerpsClosePositionPreviewConfirm() {
        logEvent("trade_perps_close_position_preview_confirm")
    }

    fun trackPerpsClosePositionPreviewCancel() {
        logEvent("trade_perps_close_position_preview_cancel")
    }

    fun trackPerpsClosePositionEnd() {
        logEvent("trade_perps_close_position_end")
    }

    fun trackPerpsAllPositions(source: String) {
        logEvent("trade_perps_all_positions") {
            putString("source", source)
        }
    }

    fun trackPerpsActivity(source: String) {
        logEvent("trade_perps_activity") {
            putString("source", source)
        }
    }

    fun trackPerpsActivityDetail(source: String) {
        logEvent("trade_perps_activity_detail") {
            putString("source", source)
        }
    }

    fun trackPerpsGuide(source: String) {
        logEvent("trade_perps_guide") {
            putString("source", source)
        }
    }

    fun trackSpotStart(wallet: String, type: String, source: String) {
        logEvent("trade_spot_start") {
            putString("wallet", wallet)
            putString("type", type)
            putString("source", source)
        }
    }

    fun trackSpotSwitchSendReceive() {
        logEvent("trade_spot_switch_send_receive")
    }

    fun trackSpotSwitchQuoteDirection() {
        logEvent("trade_spot_switch_quote_direction")
    }

    fun trackSpotPreview(sendChain: String?, sendAssetSymbol: String?, receiveChain: String?, receiveAssetSymbol: String?) {
        logEvent("trade_spot_preview") {
            putString("send_chain", sendChain)
            putString("send_asset_symbol", sendAssetSymbol)
            putString("receive_chain", receiveChain)
            putString("receive_asset_symbol", receiveAssetSymbol)
        }
    }

    fun trackSpotPreviewConfirm() {
        logEvent("trade_spot_preview_confirm")
    }

    fun trackSpotPreviewCancel() {
        logEvent("trade_spot_preview_cancel")
    }

    fun trackSpotSendInputPercent(percent: String) {
        logEvent("trade_spot_send_input_percent") {
            putString("percent", percent)
        }
    }

    fun trackSpotSendInputBalance() {
        logEvent("trade_spot_send_input_balance")
    }

    fun trackSpotPriceInputPercent(percent: String) {
        logEvent("trade_spot_price_input_percent") {
            putString("percent", percent)
        }
    }

    fun trackSpotTokenSelect(method: String, type: String, chain: String?, assetSymbol: String?) {
        logEvent("trade_spot_token_select") {
            putString("method", method)
            putString("type", type)
            putString("chain", chain)
            putString("asset_symbol", assetSymbol)
        }
    }

    fun trackSpotExpirySelect(method: String) {
        logEvent("trade_spot_expiry_select") {
            putString("method", method)
        }
    }

    fun trackSpotQuote(result: String, type: String, reason: String? = null) {
        logEvent("trade_spot_quote") {
            putString("result", result)
            putString("type", type)
            reason?.let { putString("reason", it) }
        }
    }

    fun trackSpotEnd(wallet: String, amountValue: BigDecimal, price: String?) {
        val amountUsd = runCatching {
            val priceValue = price?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            amountValue.multiply(priceValue).toDouble()
        }.getOrDefault(0.0)
        logEvent("trade_spot_end") {
            putString("wallet", wallet)
            putString("trade_asset_level", getTradeAssetLevel(amountUsd))
        }
    }

    fun trackTradeTypeSelect(type: String) {
        logEvent("trade_type_select") {
            putString("type", type)
        }
    }

    fun trackSpotTransactions(type: String) {
        logEvent("trade_spot_transactions") {
            putString("type", type)
        }
    }

    fun trackSpotDetail(type: String) {
        logEvent("trade_spot_detail") {
            putString("type", type)
        }
    }

    fun trackSpotGuide(type: String, source: String) {
        logEvent("trade_spot_guide") {
            putString("type", type)
            putString("source", source)
        }
    }

    fun trackCustomerServiceDialog(source: String, wallet: String = TradeWallet.MAIN) {
        logEvent("customer_service_dialog") {
            putString("source", source)
            putString("wallet", wallet)
        }
    }

    fun trackAddPhoneStart(source: String) {
        logEvent("add_phone_start") {
            putString("source", source)
        }
    }

    fun trackAddPhoneVerifyPin() {
        logEvent("add_phone_verify_pin")
    }

    fun trackAddPhoneInputPhone() {
        logEvent("add_phone_input_phone")
    }

    fun trackAddPhoneInputPhoneCountry() {
        logEvent("add_phone_input_phone_country")
    }

    fun trackAddPhoneSmsSendConfirmed() {
        logEvent("add_phone_sms_send_confirmed")
    }

    fun trackAddPhoneCaptcha(type: String) {
        logEvent("add_phone_captcha") {
            putString("type", type)
        }
    }

    fun trackAddPhoneSmsVerify() {
        logEvent("add_phone_sms_verify")
    }

    fun trackAddPhoneEnd() {
        logEvent("add_phone_end")
    }

    fun trackOpenBotHomePage(source: String, identityNumber: String?) {
        logEvent("open_bot_home_page") {
            putString("source", source)
            putString("identity_number", identityNumber)
        }
    }

    fun trackOpenBotConversation(source: String, identityNumber: String?) {
        logEvent("open_bot_conversation") {
            putString("source", source)
            putString("identity_number", identityNumber)
        }
    }

    fun trackMarketListRange(rangeTop: Int) {
        logEvent("market_list_range") {
            putString("range_top", rangeTop.toString())
        }
    }

    fun trackMarketListPriceChange() {
        logEvent("market_list_price_change")
    }

    fun trackMarketListOrder(column: String) {
        logEvent("market_list_order") {
            putString("column", column)
        }
    }

    fun trackMarketDetail(source: String) {
        logEvent("market_detail") {
            putString("source", source)
        }
    }

    fun trackMarketDetailShare(type: String) {
        logEvent("market_detail_share") {
            putString("type", type)
        }
    }

    fun trackMarketFavoriteAdd(source: String) {
        logEvent("market_favorite_add") {
            putString("source", source)
        }
    }

    fun trackMarketPriceAlerts(type: String) {
        logEvent("market_price_alerts") {
            putString("type", type)
        }
    }

    fun trackMarketPriceAlertAdd(source: String, frequency: String, type: String) {
        logEvent("market_price_alert_add") {
            putString("source", source)
            putString("frequency", frequency)
            putString("type", type)
        }
    }

    object SignUpStartSource {
        const val LANDING = "landing"
        const val LOGIN_MNEMONIC_PHRASE = "login_mnemonic_phrase"
        const val LOGIN_START = "login_start"
    }
}

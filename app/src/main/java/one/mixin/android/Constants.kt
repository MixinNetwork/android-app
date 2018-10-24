package one.mixin.android

object Constants {

    object API {
        const val DOMAIN = "https://mixin.one"
        const val URL = "https://api.mixin.one/"
        const val WS_URL = "wss://blaze.mixin.one"

        const val GIPHY_URL = "http://api.giphy.com/v1/"
    }

    object Account {
        const val PREF_SESSION = "pref_session"
        const val PREF_PIN_TOKEN = "pref_pin_token"
        const val PREF_NAME_ACCOUNT = "pref_name_account"
        const val PREF_NAME_TOKEN = "pref_name_token"
        const val PREF_PIN_CHECK = "pref_pin_check"
        const val PREF_PIN_INTERVAL = "pref_pin_interval"
        const val PREF_PIN_ITERATOR = "pref_pin_iterator"
        const val PREF_CAMERA_TIP = "pref_camera_tip"
        const val PREF_LOGOUT_COMPLETE = "pref_logout_complete"
        const val PREF_BIOMETRICS = "pref_biometrics"
    }

    object Scheme {
        const val CODES = "mixin://codes"
        const val PAY = "mixin://pay"
        const val USERS = "mixin://users"
        const val TRANSFER = "mixin://transfer"

        const val HTTPS_CODES = "https://mixin.one/codes"
        const val HTTPS_PAY = "https://mixin.one/pay"
        const val HTTPS_USERS = "https://mixin.one/users"
        const val HTTPS_TRANSFER = "https://mixin.one/transfer"
    }

    const val SLEEP_MILLIS: Long = 1000
    const val INTERVAL_24_HOURS: Long = 1000 * 60 * 60 * 24
    const val INTERVAL_48_HOURS: Long = 1000 * 60 * 60 * 48
    const val INTERVAL_10_MINS: Long = 1000 * 60 * 10

    const val ARGS_USER = "args_user"
    const val ARGS_USER_ID = "args_user_id"

    const val MY_QR = "my_qr"

    const val Mixin_Conversation_ID_HEADER = "Mixin-Conversation-ID"
    val KEYS = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<<")

    const val BATCH_SIZE = 700

    const val PAGE_SIZE = 20

    const val FLOOD_THREAD = "flood_thread"

    const val ACK_THREAD = "ack_thread"

    const val SINGLE_DB = "single_db"

    const val BIOMETRICS_ALIAS = "biometrics_alias"
    const val BIOMETRICS_PIN = "biometrics_pin"
    const val BIOMETRICS_IV = "biometrics_iv"
    const val BIOMETRIC_INTERVAL = "biometric_interval"
    const val BIOMETRIC_INTERVAL_DEFAULT: Long = 1000 * 60 * 60 * 2
    const val BIOMETRIC_PIN_CHECK = "biometric_pin_check"
}

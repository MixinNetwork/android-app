package one.mixin.android

import android.graphics.Color
import com.checkout.base.model.CardScheme
import com.checkout.base.model.Environment
import com.checkout.risk.RiskEnvironment
import com.google.android.gms.wallet.WalletConstants
import okhttp3.Dns
import one.mixin.android.net.CustomDns
import one.mixin.android.net.SequentialDns

object Constants {
    object API {
        const val DOMAIN = "https://mixin.one"
        const val URL = "https://api.mixin.one/"
        const val WS_URL = "wss://blaze.mixin.one"
        const val Mixin_URL = "https://mixin-api.zeromesh.net/"
        const val Mixin_WS_URL = "wss://mixin-blaze.zeromesh.net"

        const val GIPHY_URL = "https://api.giphy.com/v1/"
        const val FOURSQUARE_URL = "https://api.foursquare.com/v2/"

        const val DEFAULT_TIP_SIGN_ENDPOINT = "https://api.mixin.one/tip/notify"
    }

    object HelpLink {
        const val CENTER = "https://mixinmessenger.zendesk.com"
        const val EMERGENCY = "https://mixinmessenger.zendesk.com/hc/articles/360029154692"
        const val DEPOSIT = "https://mixinmessenger.zendesk.com/hc/articles/360018789931"
        const val DEPOSIT_NOT_SUPPORT = "https://mixinmessenger.zendesk.com/hc/en-us/articles/9954148870676"
        const val TIP = "https://tip.id"
    }

    object Tip {
        const val EPHEMERAL_SEED = "ephemeral_seed"
        const val ALIAS_EPHEMERAL_SEED = "alias_ephemeral_seed"

        const val TIP_PRIV = "tip_priv"
        const val ALIAS_TIP_PRIV = "alias_tip_priv"

        const val SPEND_SALT = "spend_salt"
        const val ALIAS_SPEND_SALT = "alias_spend_salt"
    }

    object Account {
        const val PREF_PIN_CHECK = "pref_pin_check"
        const val PREF_BIOMETRICS = "pref_biometrics"
        const val PREF_RANDOM = "pref_random"
        const val PREF_WRONG_TIME = "pref_wrong_time"
        const val PREF_RESTORE = "pref_restore"
        const val PREF_RECALL_SHOW = "pref_recall_show"
        const val PREF_HAS_WITHDRAWAL_ADDRESS_SET = "pref_has_withdrawal_address_set"
        const val PREF_RECENT_USED_BOTS = "pref_recent_used_bots"
        const val PREF_DELETE_MOBILE_CONTACTS = "pref_delete_mobile_contacts"
        const val PREF_FIAT_MAP = "pref_fiat_map"
        const val PREF_BATTERY_OPTIMIZE = "pref_battery_optimize"
        const val PREF_SYNC_CIRCLE = "pref_sync_circle"
        const val PREF_BACKUP = "pref_attachment_backup"
        const val PREF_BACKUP_DIRECTORY = "pref_attachment_backup_directory"
        const val PREF_CHECK_STORAGE = "pref_check_storage"
        const val PREF_TRIED_UPDATE_KEY = "pref_tried_update_key"
        const val PREF_DUPLICATE_TRANSFER = "pref_duplicate_transfer"
        const val PREF_STRANGER_TRANSFER = "pref_stranger_transfer"
        const val PREF_RECENT_SEARCH_ASSETS = "pref_recent_search_assets"
        const val PREF_INCOGNITO_KEYBOARD = "pref_incognito_keyboard"
        const val PREF_APP_AUTH = "pref_app_auth"
        const val PREF_APP_ENTER_BACKGROUND = "pref_app_enter_background"
        const val PREF_DEVICE_SDK = "pref_device_sdk"
        const val PREF_TEXT_SIZE = "pref_text_size"
        const val PREF_ATTACHMENT = "pref_attachment"
        const val PREF_CLEANUP_THUMB = "pref_cleanup_thumb"
        const val PREF_CLEANUP_QUOTE_CONTENT = "pref_cleanup_quote_content"
        const val PREF_TRANSFER_SCENE = "pref_transfer_scene"
        const val PREF_LOGIN_VERIFY = "pref_login_verify"
        const val PREF_NOTIFY_ENABLE_BIOMETRIC = "pref_notify_enable_biometric"
        const val PREF_SNAPSHOT_OFFSET = "pref_snapshot_offset"

        object Migration {
            const val PREF_MIGRATION_ATTACHMENT = "pref_migration_attachment"
            const val PREF_MIGRATION_ATTACHMENT_OFFSET = "pref_migration_attachment_offset"
            const val PREF_MIGRATION_ATTACHMENT_LAST = "pref_migration_attachment_last"
            const val PREF_MIGRATION_TRANSCRIPT_ATTACHMENT = "pref_migration_transcript_attachment"
            const val PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST = "pref_migration_transcript_attachment_last"
            const val PREF_MIGRATION_BACKUP = "pref_migration_backup"
        }

        object ChainAddress {
            const val EVM_ADDRESS = "evm_address"
        }
    }

    object Scheme {
        const val CODES = "mixin://codes"
        const val PAY = "mixin://pay"
        const val USERS = "mixin://users"
        const val TRANSFER = "mixin://transfer"
        const val DEVICE = "mixin://device/auth"
        const val SEND = "mixin://send"
        const val ADDRESS = "mixin://address"
        const val APPS = "mixin://apps"
        const val SNAPSHOTS = "mixin://snapshots"
        const val CONVERSATIONS = "mixin://conversations"
        const val INFO = "mixin://info"
        const val DEVICE_TRANSFER = "mixin://device-transfer"
        const val TIP = "mixin://tip"
        const val BUY = "mixin://buy"
        const val MIXIN_PAY = "mixin://mixin.one/pay/"
        const val MIXIN_MULTISIGS = "mixin://mixin.one/multisigs"
        const val MIXIN_SCHEME = "mixin://mixin.one/scheme"

        const val HTTPS_USERS = "https://mixin.one/users"
        const val HTTPS_ADDRESS = "https://mixin.one/address"
        const val HTTPS_APPS = "https://mixin.one/apps"
        const val HTTPS_PAY = "https://mixin.one/pay"
        const val HTTPS_MULTISIGS = "https://mixin.one/multisigs"
        const val HTTPS_SCHEME = "https://mixin.one/scheme"

        // web3
        const val HTTPS_MIXIN_WC = "https://mixin.one/wc"
        const val MIXIN_WC = "mixin://wc"

        // deprecated
        const val HTTPS_TRANSFER = "https://mixin.one/transfer"
        const val HTTPS_CODES = "https://mixin.one/codes"

        const val WALLET_CONNECT_PREFIX = "wc:"
    }

    object DataBase {
        const val DB_NAME = "mixin.db"
        const val MINI_VERSION = 15
        const val CURRENT_VERSION = 53

        const val FTS_DB_NAME = "fts.db"
        const val PENDING_DB_NAME = "pending.db"
    }

    object Storage {
        const val IMAGE = "IMAGE"
        const val VIDEO = "VIDEO"
        const val AUDIO = "AUDIO"
        const val DATA = "DATA"
        const val TRANSCRIPT = "TRANSCRIPT"
    }

    object BackUp {
        const val BACKUP_PERIOD = "backup_period"
        const val BACKUP_LAST_TIME = "backup_last_time"
        const val BACKUP_MEDIA = "backup_media"
    }

    object CIRCLE {
        const val CIRCLE_ID = "circle_id"
        const val CIRCLE_NAME = "circle_name"
    }

    object Download {
        const val AUTO_DOWNLOAD_MOBILE = "auto_download_mobile"
        const val AUTO_DOWNLOAD_WIFI = "auto_download_wifi"
        const val AUTO_DOWNLOAD_ROAMING = "auto_download_roaming"

        const val AUTO_DOWNLOAD_PHOTO = 0x001
        const val AUTO_DOWNLOAD_VIDEO = 0x010
        const val AUTO_DOWNLOAD_DOCUMENT = 0x100

        const val MOBILE_DEFAULT = 0x001
        const val WIFI_DEFAULT = 0x111
        const val ROAMING_DEFAULT = 0x000
    }

    object Theme {
        const val THEME_CURRENT_ID = "theme_current_id"
        const val THEME_DEFAULT_ID = 0
        const val THEME_NIGHT_ID = 1
        const val THEME_AUTO_ID = 2
    }

    object ChainId {
        const val RIPPLE_CHAIN_ID = "23dfb5a5-5d7b-48b6-905f-3970e3176e27"
        const val BITCOIN_CHAIN_ID = "c6d0c728-2624-429b-8e0d-d9d19b6592fa"
        const val ETHEREUM_CHAIN_ID = "43d61dcd-e413-450d-80b8-101d5e903357"
        const val EOS_CHAIN_ID = "6cfe566e-4aad-470b-8c9a-2fd35b49c68d"
        const val TRON_CHAIN_ID = "25dabac5-056a-48ff-b9f9-f67395dc407c"

        const val MixinVirtualMachine = "a0ffd769-5850-4b48-9651-d2ae44a3e64d"
        const val Litecoin = "76c802a2-7c88-447f-a93e-c29c9e5dd9c8"
        const val Dogecoin = "6770a1e5-6086-44d5-b60f-545f9d9e8ffd"
        const val Monero = "05c5ac01-31f9-4a69-aa8a-ab796de1d041"
        const val Dash = "6472e7e3-75fd-48b6-b1dc-28d294ee1476"
        const val Solana = "64692c23-8971-4cf4-84a7-4dd1271dd887"
        const val Polygon = "b7938396-3f94-4e0a-9179-d3440718156f"
        const val BinanceSmartChain = "1949e683-6a08-49e2-b087-d6b72398588f"
        const val Avalanche = "cbc77539-0a20-4666-8c8a-4ded62b36f0a"
        const val Arbitrum = "d0688ff7-6656-4a79-bd5f-d764bfa9bc46"
        const val Optimism = "62d5b01f-24ee-4c96-8214-8e04981d05f2"
    }

    object AssetId {
        const val MGD_ASSET_ID = "b207bce9-c248-4b8e-b6e3-e357146f3f4c"
        const val BYTOM_CLASSIC_ASSET_ID = "443e1ef5-bc9b-47d3-be77-07f328876c50"
        const val OMNI_USDT_ASSET_ID = "815b0b1a-2764-3736-8faa-42d694fa620a"
        const val USDT_ASSET_ID = "4d8c508b-91c5-375b-92b0-ee702ed2dac5"
        const val USDC_ASSET_ID = "9b180ab6-6abe-3dc0-a13f-04169eb34bfa"
    }

    object Mute {
        const val MUTE_1_HOUR = 1 * 60 * 60
        const val MUTE_8_HOURS = 8 * 60 * 60
        const val MUTE_1_WEEK = 7 * 24 * 60 * 60
        const val MUTE_1_YEAR = 365 * 24 * 60 * 60
    }

    object Locale {
        object SimplifiedChinese {
            val localeStrings = arrayOf("zh_CN", "zh_CN_#Hans", "zh_MO_#Hans", "zh_HK_#Hans", "zh_SG_#Hans")
        }

        object TraditionalChinese {
            val localeStrings = arrayOf("zh_TW", "zh_TW_#Hant", "zh_HK_#Hant", "zh_MO_#Hant")
        }

        object Russian {
            const val Language = "ru"
            const val Country = ""
        }

        object Indonesian {
            const val Language = "in"
            const val Country = ""
        }

        object Malay {
            const val Language = "ms"
            const val Country = ""
        }

        object Spanish {
            const val Language = "es"
            const val Country = ""
        }
    }

    object Debug {
        const val WEB_DEBUG = "web_debug"
        const val DB_DEBUG = "db_debug"
        const val DB_DEBUG_LOGS = "db_debug_logs"
        const val DB_DEBUG_WARNING = "db_debug_warning"
        const val LOG_AND_DEBUG = "log_and_debug"
        const val WALLET_CONNECT_DEBUG = "wallet_connect_debug"
    }

    object Colors {
        val HIGHLIGHTED = Color.parseColor("#CCEF8C")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
        val SELECT_COLOR = Color.parseColor("#660D94FC")
    }

    const val DEVICE_ID = "device_id"

    const val SLEEP_MILLIS: Long = 1000
    const val INTERVAL_24_HOURS: Long = (1000 * 60 * 60 * 24).toLong()
    const val INTERVAL_48_HOURS: Long = (1000 * 60 * 60 * 48).toLong()
    const val INTERVAL_10_MINS: Long = (1000 * 60 * 10).toLong()
    const val INTERVAL_30_MINS: Long = (1000 * 60 * 30).toLong()
    const val INTERVAL_1_MIN: Long = (1000 * 60).toLong()
    const val INTERVAL_7_DAYS: Long = INTERVAL_24_HOURS * 7
    const val DELAY_SECOND = 60
    const val ALLOW_INTERVAL: Long = (5 * 60 * 1000).toLong()

    const val SAFETY_NET_INTERVAL_KEY = "safety_net_interval_key"

    const val ARGS_USER = "args_user"
    const val ARGS_USER_ID = "args_user_id"
    const val ARGS_CONVERSATION_ID = "args_conversation_id"
    const val ARGS_ASSET_ID = "args_asset_id"
    const val ARGS_TITLE = "args_title"

    const val MY_QR = "my_qr"

    const val Mixin_Conversation_ID_HEADER = "Mixin-Conversation-ID"

    const val BATCH_SIZE = 700
    const val MARK_REMOTE_LIMIT = 500
    const val ACK_LIMIT = 100
    const val MARK_LIMIT = 10000
    const val LOGS_LIMIT = 10000

    const val PAGE_SIZE = 16
    const val FIXED_LOAD_SIZE = 48
    const val CONVERSATION_PAGE_SIZE = 15

    const val BIOMETRICS_ALIAS = "biometrics_alias"
    const val BIOMETRICS_PIN = "biometrics_pin"
    const val BIOMETRICS_IV = "biometrics_iv"
    const val BIOMETRIC_INTERVAL = "biometric_interval"
    const val BIOMETRIC_INTERVAL_DEFAULT: Long = (1000 * 60 * 60 * 2).toLong()
    const val BIOMETRIC_PIN_CHECK = "biometric_pin_check"

    const val RECENT_USED_BOTS_MAX_COUNT = 20
    const val RECENT_SEARCH_ASSETS_MAX_COUNT = 8

    const val PIN_ERROR_MAX = 5

    const val BIG_IMAGE_SIZE = 5 * 1024 * 1024

    const val DB_DELETE_MEDIA_LIMIT = 100
    const val DB_DELETE_LIMIT = 500
    const val DB_EXPIRED_LIMIT = 20

    const val MAX_THUMB_IMAGE_LENGTH = 5120
    const val DEFAULT_THUMB_IMAGE = "K0OWvn_3fQ~qj[fQfQfQfQ"

    val DNS: Dns = SequentialDns(CustomDns("8.8.8.8"), CustomDns("1.1.1.1"), CustomDns("2001:4860:4860::8888"), Dns.SYSTEM)

    const val TEAM_MIXIN_USER_ID = "773e5e77-4107-45c2-b648-8fc722ed77f5"
    const val MIXIN_BOTS_USER_ID = "68ef7899-3e81-4b3d-8124-83ae652def89"
    const val MIXIN_DATA_USER_ID = "96c1460b-c7c4-480a-a342-acaa73995a37"
    const val MIXIN_BOND_USER_ID = "84c9dfb1-bfcf-4cb4-8404-cc5a1354005b"
    const val MIXIN_FEE_USER_ID = "674d6776-d600-4346-af46-58e77d8df185"

    const val TEAM_MIXIN_USER_NAME = "Team Mixin"
    const val MIXIN_BOND_USER_NAME = "Bond Bot"
    const val MIXIN_BOTS_USER_NAME = "Mixin Bots"
    const val MIXIN_DATA_USER_NAME = "Mixin Data"

    const val DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS: String = "432000" //NFTs typically require more gas

    const val SAFE_PUBLIC_KEY = "8f94e89d03fa128a7081c5fe73c6814010c5ca74438411a42df87c6023dfa94d"

    // Only for third-party messenger user
    const val TEAM_BOT_ID = ""
    const val TEAM_BOT_NAME = ""

    object RouteConfig {
        const val PAN_ONLY = "pan_only"
        const val CRYPTOGRAM_3DS = "cryptogram_3ds"

        val SUPPORTED_METHODS =
            listOf(
                "PAN_ONLY",
                "CRYPTOGRAM_3DS",
            )

        val SUPPORTED_NETWORKS =
            listOf(
                "VISA",
                "MASTERCARD",
                "AMEX",
                "JCB",
            )

        val SUPPORTED_CARD_SCHEME = listOf(CardScheme.VISA, CardScheme.MASTERCARD, CardScheme.AMERICAN_EXPRESS, CardScheme.JCB)

        const val ROUTE_BOT_USER_ID = "61cb8dd4-16b1-4744-ba0c-7b2d2e52fc59"

        const val ROUTE_BOT_URL = "https://api.route.mixin.one"

        const val GOOGLE_PAY = "googlepay"

        const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION

        const val PAYMENTS_GATEWAY = "checkoutltd"

        val CHECKOUT_ENVIRONMENT: Environment = Environment.PRODUCTION

        val RISK_ENVIRONMENT = RiskEnvironment.PRODUCTION

        val ENVIRONMENT_3DS = com.checkout.threeds.Environment.PRODUCTION
    }
}

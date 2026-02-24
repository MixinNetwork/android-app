package one.mixin.android

import android.graphics.Color
import com.checkout.base.model.CardScheme
import com.checkout.base.model.Environment
import com.checkout.risk.RiskEnvironment
import com.google.android.gms.wallet.WalletConstants
import okhttp3.Dns
import one.mixin.android.Constants.ChainId.Arbitrum
import one.mixin.android.Constants.ChainId.Avalanche
import one.mixin.android.Constants.ChainId.BITCOIN_CHAIN_ID
import one.mixin.android.Constants.ChainId.Base
import one.mixin.android.Constants.ChainId.BinanceSmartChain
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID
import one.mixin.android.Constants.ChainId.Optimism
import one.mixin.android.Constants.ChainId.Polygon
import one.mixin.android.net.CustomDns
import one.mixin.android.net.SequentialDns

object Constants {
    const val DEFAULT_ICON_URL: String = "https://images.mixin.one/yH_I5b0GiV2zDmvrXRyr3bK5xusjfy5q7FX3lw3mM2Ryx4Dfuj6Xcw8SHNRnDKm7ZVE3_LvpKlLdcLrlFQUBhds=s128"

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
        const val TIP = "https://tip.id"
        const val INSCRIPTION = "https://mixin.one/inscriptions/"
        const val MARKETPLACE = "https://rune.fan/items/"
        const val SPACE = "https://mixin.space/tx/"
        const val CUSTOMER_SERVICE = "https://go.crisp.chat/chat/embed/?website_id=52662bba-be49-4b06-9edc-7baa9a78f714"
    }

    object Tip {
        const val EPHEMERAL_SEED = "ephemeral_seed"
        const val ALIAS_EPHEMERAL_SEED = "alias_ephemeral_seed"

        const val TIP_PRIV = "tip_priv"
        const val ALIAS_TIP_PRIV = "alias_tip_priv"

        const val MNEMONIC = "mnemonic"

        const val SPEND_SALT = "spend_salt"
        const val ALIAS_SPEND_SALT = "alias_spend_salt"

        const val ENCRYPTED_MNEMONIC = "encrypted_mnemonic"

        const val ENCRYPTED_WEB3_KEY = "encrypted_web3_key"
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
        const val PREF_RECENT_SEARCH = "pref_recent_search"
        const val PREF_DELETE_MOBILE_CONTACTS = "pref_delete_mobile_contacts"
        const val PREF_FIAT_MAP = "pref_fiat_map"
        const val PREF_BATTERY_OPTIMIZE = "pref_battery_optimize"
        const val PREF_SYNC_CIRCLE = "pref_sync_circle"
        const val PREF_BACKUP = "pref_attachment_backup"
        const val PREF_BACKUP_DIRECTORY = "pref_attachment_backup_directory"
        const val PREF_WEB3_ADDRESSES_SYNCED = "pref_web3_addresses_synced"
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
        const val PREF_LOGIN_OR_SIGN_UP = "pref_login_or_sign_up"
        const val PREF_NOTIFY_ENABLE_BIOMETRIC = "pref_notify_enable_biometric"
        const val PREF_SNAPSHOT_OFFSET = "pref_snapshot_offset"
        const val PREF_EXPLORE_SELECT = "pref_explore_select"
        const val PREF_SWAP_SLIPPAGE = "pref_swap_slippage"
        const val PREF_SWAP_LAST_PAIR = "pref_swap_last_pair"
        const val PREF_LIMIT_SWAP_LAST_PAIR = "pref_limit_swap_last_pair"
        const val PREF_WEB3_SWAP_LAST_PAIR = "pref_web3_swap_last_pair"
        const val PREF_WEB3_LIMIT_SWAP_LAST_PAIR = "pref_web3_limit_swap_last_pair"
        const val PREF_INSCRIPTION_TYPE = "pref_inscription_type"
        const val PREF_MARKET_TYPE = "pref_market_type"
        const val PREF_MARKET_ORDER = "pref_market_order"
        const val PREF_INSCRIPTION_ORDER = "pref_inscription_order"
        const val PREF_ROUTE_BOT_PK = "pref_route_bot_pk"
        const val PREF_GLOBAL_MARKET = "pref_global_market"
        const val PREF_MARKET_TOP_PERCENTAGE = "pref_market_top_percentage"
        const val PREF_QUOTE_COLOR = "pref_quote_color"

        const val PREF_HAS_USED_BUY = "pref_has_used_buy"
        const val PREF_HAS_USED_SWAP = "pref_has_used_swap"
        const val PREF_HAS_USED_SWAP_TRANSACTION = "pref_has_used_swap_transaction" // -1: No data, 0: Never used, 1: Used before
        const val PREF_HAS_USED_MARKET = "pref_has_used_market"

        const val PREF_TRADE_LIMIT_ORDER_BADGE_DISMISSED = "pref_trade_limit_order_badge_dismissed"

        const val PREF_USED_WALLET = "pref_used_wallet"

        const val PREF_HAS_USED_WALLET_LIST = "pref_has_used_wallet_list"

        const val PREF_HAS_USED_ADD_WALLET = "pref_has_used_add_wallet"

        const val PREF_WALLET_CATEGORY_FILTER = "pref_wallet_category_filter"

        const val PREF_TO_SWAP = "pref_to_swap"
        const val PREF_FROM_SWAP = "pref_from_swap"
        const val PREF_TO_LIMIT_SWAP = "pref_to_limit_swap"
        const val PREF_FROM_LIMIT_SWAP = "pref_from_limit_swap"
        const val PREF_TO_WEB3_SWAP = "pref_to_web3_swap"
        const val PREF_FROM_WEB3_SWAP = "pref_from_web3_swap"
        const val PREF_TO_WEB3_LIMIT_SWAP = "pref_to_web3_limit_swap"
        const val PREF_FROM_WEB3_LIMIT_SWAP = "pref_from_web3_limit_swap"
        const val PREF_WALLET_SEND = "pref_wallet_send"
        const val PREF_WALLET_RECEIVE = "pref_wallet_receive"

        object Migration {
            const val PREF_MIGRATION_ATTACHMENT = "pref_migration_attachment"
            const val PREF_MIGRATION_ATTACHMENT_OFFSET = "pref_migration_attachment_offset"
            const val PREF_MIGRATION_ATTACHMENT_LAST = "pref_migration_attachment_last"
            const val PREF_MIGRATION_TRANSCRIPT_ATTACHMENT = "pref_migration_transcript_attachment"
            const val PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST = "pref_migration_transcript_attachment_last"
            const val PREF_MIGRATION_BACKUP = "pref_migration_backup"
            const val PREF_MIGRATION_INSCRIPTION = "pref_migration_inscription"
            const val PREF_MIGRATION_COLLECTION = "pref_migration_inscription_collection"
        }

        object ChainAddress {
            const val EVM_ADDRESS = "evm_address"
            const val SOLANA_ADDRESS = "solana_address"
            const val BTC_ADDRESS = "btc_address"
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
        const val MIXIN_SEND = "mixin://mixin.one/send"
        const val MIXIN_PAY = "mixin://mixin.one/pay/"
        const val MIXIN_MULTISIGS = "mixin://mixin.one/multisigs"
        const val MIXIN_SCHEME = "mixin://mixin.one/scheme"
        const val MIXIN_TIP_SIGN = "mixin://mixin.one/tip/sign"
        const val MIXIN_SWAP = "mixin://mixin.one/swap"
        const val MIXIN_TRADE = "mixin://mixin.one/trade"
        const val MIXIN_MARKET = "mixin://mixin.one/markets"
        const val MIXIN_REFERRALS = "mixin://mixin.one/referrals"
        const val HTTPS_USERS = "https://mixin.one/users"
        const val HTTPS_ADDRESS = "https://mixin.one/address"
        const val HTTPS_INSCRIPTION = "https://mixin.one/inscriptions"
        const val HTTPS_MARKET = "https://mixin.one/markets"

        const val HTTPS_REFERRALS = "https://mixin.one/referrals"
        const val HTTPS_APPS = "https://mixin.one/apps"
        const val HTTPS_PAY = "https://mixin.one/pay"
        const val HTTPS_SEND = "https://mixin.one/send"
        const val HTTPS_MULTISIGS = "https://mixin.one/multisigs"
        const val HTTPS_SCHEME = "https://mixin.one/scheme"
        const val HTTPS_TIP_SIGN = "https://mixin.one/tip/sign"
        const val HTTPS_SWAP = "https://mixin.one/swap"
        const val HTTPS_TRADE = "https://mixin.one/trade"
        const val HTTPS_MEMBERSHIP = "https://mixin.one/membership"

        // web3
        const val HTTPS_MIXIN_WC = "https://mixin.one/wc"
        const val MIXIN_WC = "mixin://wc"

        // deprecated
        const val HTTPS_TRANSFER = "https://mixin.one/transfer"
        const val HTTPS_CODES = "https://mixin.one/codes"

        const val WALLET_CONNECT_PREFIX = "wc:"
        const val DEBUG = "mixin://debug"
    }

    object DataBase {
        const val DB_NAME = "mixin.db"
        const val MINI_VERSION = 15
        const val CURRENT_VERSION = 69

        const val FTS_DB_NAME = "fts.db"
        const val PENDING_DB_NAME = "pending.db"
        const val WEB3_DB_NAME = "web3.db"
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


    val Web3EvmChainIds = listOf(ETHEREUM_CHAIN_ID, Polygon, BinanceSmartChain, Base, Arbitrum, Optimism, Avalanche)
    val Web3ChainIds = listOf(ETHEREUM_CHAIN_ID, Polygon, BinanceSmartChain, Base, Arbitrum, Optimism, Avalanche)

    object ChainId {
        const val RIPPLE_CHAIN_ID = "23dfb5a5-5d7b-48b6-905f-3970e3176e27"
        const val BITCOIN_CHAIN_ID = "c6d0c728-2624-429b-8e0d-d9d19b6592fa"
        const val ETHEREUM_CHAIN_ID = "43d61dcd-e413-450d-80b8-101d5e903357"
        const val EOS_CHAIN_ID = "6cfe566e-4aad-470b-8c9a-2fd35b49c68d"
        const val TRON_CHAIN_ID = "25dabac5-056a-48ff-b9f9-f67395dc407c"
        const val SOLANA_CHAIN_ID = "64692c23-8971-4cf4-84a7-4dd1271dd887"

        const val LIGHTNING_NETWORK_CHAIN_ID = "59c09123-95cc-3ffd-a659-0f9169074cee"
        const val MixinVirtualMachine = "a0ffd769-5850-4b48-9651-d2ae44a3e64d"
        const val Base = "3fb612c5-6844-3979-ae4a-5a84e79da870"
        const val Arbitrum = "8c590110-1abc-3697-84f2-05214e6516aa"
        const val Optimism = "60360611-370c-3b69-9826-b13db93f6aba"
        const val Litecoin = "76c802a2-7c88-447f-a93e-c29c9e5dd9c8"
        const val Dogecoin = "6770a1e5-6086-44d5-b60f-545f9d9e8ffd"
        const val Monero = "05c5ac01-31f9-4a69-aa8a-ab796de1d041"
        const val Dash = "6472e7e3-75fd-48b6-b1dc-28d294ee1476"
        const val Solana = "64692c23-8971-4cf4-84a7-4dd1271dd887"
        const val Polygon = "b7938396-3f94-4e0a-9179-d3440718156f"
        const val BinanceSmartChain = "1949e683-6a08-49e2-b087-d6b72398588f"
        const val BinanceBeaconChain = "17f78d7c-ed96-40ff-980c-5dc62fecbc85"
        const val BitShares = "05891083-63d2-4f3d-bfbe-d14d7fb9b25a"
        const val MobileCoin = "eea900a8-b327-488c-8d8d-1428702fe240"

        const val Avalanche = "1f67ac58-87ba-3571-9781-e9413c046f34"

        const val TON_CHAIN_ID = "ef660437-d915-4e27-ad3f-632bfb6ba0ee"
    }

    object AssetId {
        const val MGD_ASSET_ID = "b207bce9-c248-4b8e-b6e3-e357146f3f4c"
        const val BYTOM_CLASSIC_ASSET_ID = "443e1ef5-bc9b-47d3-be77-07f328876c50"
        const val OMNI_USDT_ASSET_ID = "815b0b1a-2764-3736-8faa-42d694fa620a"
        const val XIN_ASSET_ID = "c94ac88f-4671-3976-b60a-09064f1811e8"

        const val USDT_ASSET_ETH_ID = "4d8c508b-91c5-375b-92b0-ee702ed2dac5"
        const val USDT_ASSET_TRC_ID = "b91e18ff-a9ae-3dc7-8679-e935d9a4b34b"
        const val USDT_ASSET_SOL_ID = "cb54aed4-1893-3977-b739-ec7b2e04f0c5"
        const val USDT_ASSET_POL_ID = "218bc6f4-7927-3f8e-8568-3a3725b74361"
        const val USDT_ASSET_BEP_ID = "94213408-4ee7-3150-a9c4-9c5cce421c78"
        const val USDT_ASSET_TON_ID = "7369eea0-0c69-3906-b419-e960e3595a4f"

        const val USDC_ASSET_ETH_ID = "9b180ab6-6abe-3dc0-a13f-04169eb34bfa"
        const val USDC_ASSET_SOL_ID = "de6fa523-c596-398e-b12f-6d6980544b59"
        const val USDC_ASSET_BASE_ID = "2f845564-3898-3d17-8c24-3275e96235b5"
        const val USDC_ASSET_POL_ID = "5fec1691-561d-339f-8819-63d54bf50b52"
        const val USDC_ASSET_BEP_ID = "3d3d69f1-6742-34cf-95fe-3f8964e6d307"

        val usdcAssets =
            mapOf(
                USDC_ASSET_ETH_ID to "Ethereum",
                USDC_ASSET_SOL_ID to "Solana",
                USDC_ASSET_BASE_ID to "Base",
                USDC_ASSET_POL_ID to "Polygon",
                USDC_ASSET_BEP_ID to "BSC"
            )

        val usdtAssets =
            mapOf(
                USDT_ASSET_ETH_ID to "Ethereum",
                USDT_ASSET_TRC_ID to "TRON",
                USDT_ASSET_SOL_ID to "Solana",
                USDT_ASSET_POL_ID to "Polygon",
                USDT_ASSET_BEP_ID to "BSC",
                USDT_ASSET_TON_ID to "TON",
            )

        val ethAssets = mapOf(
            ETHEREUM_CHAIN_ID to "Ethereum",
            Base to "Base",
            Optimism to "Optimism",
            Arbitrum to "Arbitrum"
        )

        val btcAssets = mapOf(
            BITCOIN_CHAIN_ID to "Bitcoin",
            LIGHTNING_NETWORK_CHAIN_ID to "Lightning",
        )

    }

    val usdIds = AssetId.usdtAssets.keys.plus(AssetId.usdcAssets.keys).toList()

    object AssetLevel {
        const val GOOD = 12
        const val VERIFIED = 11
        const val UNKNOWN = 10
        const val SPAM = 1
        const val SCAM = 0
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
    const val APP_VERSION = "app_version"

    const val SLEEP_MILLIS: Long = 1000
    const val INTERVAL_24_HOURS: Long = (1000 * 60 * 60 * 24).toLong()
    const val INTERVAL_48_HOURS: Long = (1000 * 60 * 60 * 48).toLong()
    const val INTERVAL_10_MINS: Long = (1000 * 60 * 10).toLong()
    const val INTERVAL_30_MINS: Long = (1000 * 60 * 30).toLong()
    const val INTERVAL_1_MIN: Long = (1000 * 60).toLong()
    const val INTERVAL_7_DAYS: Long = INTERVAL_24_HOURS * 7
    const val INTERVAL_60_DAYS: Long = INTERVAL_24_HOURS * 60
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
    const val MIXIN_BOND_USER_ID = "84c9dfb1-bfcf-4cb4-8404-cc5a1354005b"
    const val MIXIN_FEE_USER_ID = "674d6776-d600-4346-af46-58e77d8df185"
    const val MIXIN_ALERT_USER_ID = "e91728d9-d9f5-4e66-bc59-a3e1ed5eec7f"

    const val TEAM_MIXIN_USER_NAME = "Team Mixin"
    const val MIXIN_BOND_USER_NAME = "Bond Bot"

    const val MIXIN_FREE_FEE = "mixin free fee"

    const val DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS: String = "432000" // NFTs typically require more gas

    val SAFE_PUBLIC_KEY = listOf(
        "8f94e89d03fa128a7081c5fe73c6814010c5ca74438411a42df87c6023dfa94d",
        "2dc073588908a02284197ad78fc863e83c760dabcd5d9a508e09a799ebc1ecb8"
    )

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

        const val SAFE_BOT_USER_ID = "b5418449-9ed6-4979-a690-82690949c542"

        const val ROUTE_BOT_URL = "https://api.route.mixin.one"

        const val GOOGLE_PAY = "googlepay"

        const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION

        const val PAYMENTS_GATEWAY = "checkoutltd"

        val CHECKOUT_ENVIRONMENT: Environment = Environment.PRODUCTION

        val RISK_ENVIRONMENT = RiskEnvironment.PRODUCTION

        val ENVIRONMENT_3DS = com.checkout.threeds.Environment.PRODUCTION

        const val WEB3_URL = "https://web3-api.mixin.one"

        const val WEB3_BOT_USER_ID = "57eff6cd-038b-4ad6-abab-5792f95e05d7"
    }
}

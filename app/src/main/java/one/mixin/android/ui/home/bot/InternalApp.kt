package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.vo.App

interface BotInterface

data class Bot(val id: String, val name: String, val icon: String? = null) : BotInterface {
    constructor(app: App) : this(app.appId, app.name, app.iconUrl)
}

fun Bot.getInternalIcon(): Int = when (id) {
    VALUE_WALLET -> R.drawable.ic_bot_wallet
    VALUE_CAMERA -> R.drawable.ic_bot_camera
    VALUE_SCAN -> R.drawable.ic_bot_scan
    else -> 0
}

const val VALUE_WALLET = "1462e610-7de1-4865-bc06-d71cfcbd0329"
const val VALUE_CAMERA = "15366a81-077c-414b-8829-552c5c87a2ae"
const val VALUE_SCAN = "1cc9189a-ddcd-4b95-a18b-4411da1b8d80"

const val TOP_BOT = "top_bot"

val InternalWallet = Bot(VALUE_WALLET, MixinApplication.appContext.getString(R.string.bot_internal_wallet))
val InternalCamera = Bot(VALUE_CAMERA, MixinApplication.appContext.getString(R.string.bot_internal_camera))
val InternalScan = Bot(VALUE_SCAN, MixinApplication.appContext.getString(R.string.bot_internal_scan))

enum class BotCategory(@DrawableRes val icon: Int) {
    BOOK(R.drawable.ic_bot_category_book),
    CIRCLE(R.drawable.ic_bot_category_circle),
    EXCHANGE(R.drawable.ic_bot_category_exchange),
    GAME(R.drawable.ic_bot_category_game),
    MUSIC(R.drawable.ic_bot_category_music),
    NEWS(R.drawable.ic_bot_category_news),
    OTHER(R.drawable.ic_bot_category_other),
    SHOPPING(R.drawable.ic_bot_category_shopping),
    TEACH(R.drawable.ic_bot_category_teach),
    TOOLS(R.drawable.ic_bot_category_tools),
    VIDEO(R.drawable.ic_bot_category_video),
    WALLET(R.drawable.ic_bot_category_wallet),
    WEATHER(R.drawable.ic_bot_category_weather),
}

@DrawableRes
fun App.getCategoryIcon(): Int = when (category) {
    BotCategory.BOOK.name -> BotCategory.BOOK.icon
    BotCategory.CIRCLE.name -> BotCategory.CIRCLE.icon
    BotCategory.EXCHANGE.name -> BotCategory.EXCHANGE.icon
    BotCategory.GAME.name -> BotCategory.GAME.icon
    BotCategory.MUSIC.name -> BotCategory.MUSIC.icon
    BotCategory.NEWS.name -> BotCategory.NEWS.icon
    BotCategory.OTHER.name -> BotCategory.OTHER.icon
    BotCategory.SHOPPING.name -> BotCategory.SHOPPING.icon
    BotCategory.TEACH.name -> BotCategory.TEACH.icon
    BotCategory.TOOLS.name -> BotCategory.TOOLS.icon
    BotCategory.VIDEO.name -> BotCategory.VIDEO.icon
    BotCategory.WALLET.name -> BotCategory.WALLET.icon
    BotCategory.WEATHER.name -> BotCategory.WEATHER.icon
    else -> BotCategory.OTHER.icon
}

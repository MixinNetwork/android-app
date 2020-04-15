package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.vo.App

interface BotInterface

data class Bot(val id: String, val name: String, @DrawableRes val icon: Int) : BotInterface

const val INTERNAL_WALLET_ID = "1462e610-7de1-4865-bc06-d71cfcbd0329"
const val INTERNAL_CAMERA_ID = "15366a81-077c-414b-8829-552c5c87a2ae"
const val INTERNAL_SCAN_ID = "1cc9189a-ddcd-4b95-a18b-4411da1b8d80"

const val TOP_BOT = "top_bot"

val InternalWallet = Bot(INTERNAL_WALLET_ID, MixinApplication.appContext.getString(R.string.bot_internal_wallet), R.drawable.ic_bot_wallet)
val InternalCamera = Bot(INTERNAL_CAMERA_ID, MixinApplication.appContext.getString(R.string.bot_internal_camera), R.drawable.ic_bot_camera)
val InternalScan = Bot(INTERNAL_SCAN_ID, MixinApplication.appContext.getString(R.string.bot_internal_scan), R.drawable.ic_bot_scan)

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

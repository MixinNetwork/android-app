package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.App
import one.mixin.android.vo.BotInterface

data class Bot(val id: String, val name: String, @DrawableRes val icon: Int) : BotInterface {
    override fun getBotId() = id
}

const val INTERNAL_WALLET_ID = "1462e610-7de1-4865-bc06-d71cfcbd0329"
const val INTERNAL_CAMERA_ID = "15366a81-077c-414b-8829-552c5c87a2ae"
const val INTERNAL_SCAN_ID = "1cc9189a-ddcd-4b95-a18b-4411da1b8d80"

const val TOP_BOT = "top_bot"

val DefaultTopBots: String = GsonHelper.customGson.toJson(arrayOf(INTERNAL_WALLET_ID, INTERNAL_SCAN_ID))

val InternalWallet = Bot(INTERNAL_WALLET_ID, MixinApplication.appContext.getString(R.string.Wallet), R.drawable.ic_bot_wallet)
val InternalCamera = Bot(INTERNAL_CAMERA_ID, MixinApplication.appContext.getString(R.string.Camera), R.drawable.ic_bot_camera)
val InternalScan = Bot(INTERNAL_SCAN_ID, MixinApplication.appContext.getString(R.string.Scan_QR), R.drawable.ic_bot_scan)

enum class BotCategory(@DrawableRes val icon: Int) {
    TRADING(R.drawable.ic_bot_category_trading),
    BUSINESS(R.drawable.ic_bot_category_business),
    BOOKS(R.drawable.ic_bot_category_books),
    EDUCATION(R.drawable.ic_bot_category_education),
    SOCIAL(R.drawable.ic_bot_category_social),
    GAMES(R.drawable.ic_bot_category_games),
    MUSIC(R.drawable.ic_bot_category_music),
    NEWS(R.drawable.ic_bot_category_news),
    SHOPPING(R.drawable.ic_bot_category_shopping),
    TOOLS(R.drawable.ic_bot_category_tools),
    VIDEO(R.drawable.ic_bot_category_video),
    WALLET(R.drawable.ic_bot_category_wallet),
    PHOTO(R.drawable.ic_bot_category_photo),
    OTHER(R.drawable.ic_bot_category_other),
}

@DrawableRes
fun App.getCategoryIcon(): Int = when (category) {
    BotCategory.BOOKS.name -> BotCategory.BOOKS.icon
    BotCategory.BUSINESS.name -> BotCategory.BUSINESS.icon
    BotCategory.SOCIAL.name -> BotCategory.SOCIAL.icon
    BotCategory.TRADING.name -> BotCategory.TRADING.icon
    BotCategory.GAMES.name -> BotCategory.GAMES.icon
    BotCategory.MUSIC.name -> BotCategory.MUSIC.icon
    BotCategory.NEWS.name -> BotCategory.NEWS.icon
    BotCategory.OTHER.name -> BotCategory.OTHER.icon
    BotCategory.SHOPPING.name -> BotCategory.SHOPPING.icon
    BotCategory.EDUCATION.name -> BotCategory.EDUCATION.icon
    BotCategory.TOOLS.name -> BotCategory.TOOLS.icon
    BotCategory.VIDEO.name -> BotCategory.VIDEO.icon
    BotCategory.WALLET.name -> BotCategory.WALLET.icon
    BotCategory.PHOTO.name -> BotCategory.PHOTO.icon
    else -> BotCategory.OTHER.icon
}

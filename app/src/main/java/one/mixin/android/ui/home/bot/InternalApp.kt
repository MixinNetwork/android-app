package one.mixin.android.ui.home.bot

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

const val VALUE_WALLET = "A16BAE57-8540-4F61-890A-80CB7AB379D3"
const val VALUE_CAMERA = "D4A9E7CD-A127-42B4-BEE2-08DF17059802"
const val VALUE_SCAN = "3FFF6F27-DBE0-482C-910B-FB09356C4E40"

const val TOP_BOT = "top_bot"

val InternalWallet = Bot(VALUE_WALLET, MixinApplication.appContext.getString(R.string.bot_internal_wallet))
val InternalCamera = Bot(VALUE_CAMERA, MixinApplication.appContext.getString(R.string.bot_internal_camera))
val InternalScan = Bot(VALUE_SCAN, MixinApplication.appContext.getString(R.string.bot_internal_scan))

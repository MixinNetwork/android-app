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

const val VALUE_WALLET = "0x001"
const val VALUE_CAMERA = "0x010"
const val VALUE_SCAN = "0x100"

const val TOP_BOT = "top_bot"

val InternalWallet = Bot(VALUE_WALLET, MixinApplication.appContext.getString(R.string.bot_internal_wallet))
val InternalCamera = Bot(VALUE_CAMERA, MixinApplication.appContext.getString(R.string.bot_internal_camera))
val InternalScan = Bot(VALUE_SCAN, MixinApplication.appContext.getString(R.string.bot_internal_scan))

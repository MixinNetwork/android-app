package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.vo.App

interface AppInterface

class InternalApp(@StringRes val name: Int, @DrawableRes val icon: Int) : AppInterface

val InternalWallet = InternalApp(R.string.bot_internal_wallet, R.drawable.ic_bot_wallet)
val InternalCamera = InternalApp(R.string.bot_internal_camera, R.drawable.ic_bot_camera)
val InternalScan = InternalApp(R.string.bot_internal_scan, R.drawable.ic_bot_scan)

const val VALUE_WALLET = 0x001
const val VALUE_CAMERA = 0x010
const val VALUE_SCAN = 0x100

class BotDataSource {
    private val topApp = mutableListOf<AppInterface>()
    private val internalApp = mutableListOf<AppInterface>()

    fun init(value: Int) {
        if (value.or(0x110) == 0x111){
        }
        if (value.or(0x110) == 0x111){

        }
        if (value.or(0x110) == 0x111){

        }
    }
}
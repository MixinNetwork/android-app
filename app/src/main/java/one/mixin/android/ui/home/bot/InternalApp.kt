package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.R

interface AppInterface

class InternalApp(@StringRes val name: Int, @DrawableRes val icon: Int) : AppInterface

val InternalWallet = InternalApp(R.string.bot_internal_wallet, R.drawable.ic_bot_wallet)
val InternalCamera = InternalApp(R.string.bot_internal_camera, R.drawable.ic_bot_camera)
val InternalScan = InternalApp(R.string.bot_internal_scan, R.drawable.ic_bot_scan)

const val VALUE_WALLET = 0x001
const val VALUE_CAMERA = 0x010
const val VALUE_SCAN = 0x100



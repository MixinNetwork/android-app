package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.util.getLocalString
import one.mixin.android.vo.BotInterface

data class Bot(
    val id: String,
    val name: String,
    @StringRes val description:Int,
    @DrawableRes val icon: Int,
) : BotInterface {
    override fun getBotId() = id
}

const val INTERNAL_CAMERA_ID = "15366a81-077c-414b-8829-552c5c87a2ae"
const val INTERNAL_BUY_ID = "DC2C902F-7236-4049-85BC-5ECBD1971668"
const val INTERNAL_LINK_DESKTOP_ID = "7C4346C7-CFEF-40CD-AC70-4041FDC3D941"
const val INTERNAL_SUPPORT_ID = "77443b1f-bbr4-4aad-8b6b-b8f58761e2e9"

val InternalCamera = Bot(INTERNAL_CAMERA_ID, getLocalString(MixinApplication.appContext, R.string.Camera), R.string.take_a_photo, R.drawable.ic_bot_camera)
val InternalBuy= Bot(INTERNAL_BUY_ID, getLocalString(MixinApplication.appContext, R.string.Buy), R.string.buy_crypto_with_card, R.drawable.ic_bot_buy)
val InternalLinkDesktop= Bot(INTERNAL_LINK_DESKTOP_ID, getLocalString(MixinApplication.appContext, R.string.Link_desktop),R.string.link_desktop_description, R.drawable.ic_bot_desktop)
val InternalSupport = Bot(INTERNAL_SUPPORT_ID, getLocalString(MixinApplication.appContext, R.string.Contact_Support), R.string.leave_message_to_team_mixin, R.drawable.ic_bot_support)

val InternalBots = listOf(InternalCamera, InternalBuy, InternalLinkDesktop, InternalSupport)
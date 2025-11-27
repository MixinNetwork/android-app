package one.mixin.android.ui.home.bot

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.vo.BotInterface

data class Bot(
    val id: String,
    @StringRes val name: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
) : BotInterface {
    override fun getBotId() = id
}

const val INTERNAL_BUY_ID = "DC2C902F-7236-4049-85BC-5ECBD1971668"
const val INTERNAL_SWAP_ID = "656A2DB8-F4DC-4035-AFDF-53F7321F47DB"
const val INTERNAL_MEMBER_ID = "1592B537-E96D-45E4-B937-15E51FE9BAB7"

const val INTERNAL_REFERRAL_ID = "b35af74d-cca6-400c-a62b-5a7e659de91e"
const val INTERNAL_LINK_DESKTOP_ID = "7C4346C7-CFEF-40CD-AC70-4041FDC3D941"

const val INTERNAL_SUPPORT_ID = "77443b1f-bbr4-4aad-8b6b-b8f58761e2e9"

val InternalBuy = Bot(INTERNAL_BUY_ID, R.string.Buy, R.string.buy_crypto_with_cash, R.drawable.ic_bot_buy)
val InternalSwap = Bot(INTERNAL_SWAP_ID, R.string.Trade, R.string.trade_native_tokens, R.drawable.ic_bot_swap)
val InternalMember = Bot(INTERNAL_MEMBER_ID, R.string.Mixin_One, R.string.mixin_one_desc, R.drawable.ic_bot_member)

val InternalReferral = Bot(INTERNAL_REFERRAL_ID, R.string.Referral, R.string.referral_description, R.drawable.ic_bot_referral)
val InternalLinkDesktop = Bot(INTERNAL_LINK_DESKTOP_ID, R.string.Link_desktop, R.string.link_desktop_description, R.drawable.ic_bot_desktop)
val InternalSupport = Bot(INTERNAL_SUPPORT_ID, R.string.Contact_Support, R.string.leave_message_to_team_mixin, R.drawable.ic_bot_support)

val InternalLinkDesktopLogged = Bot(INTERNAL_LINK_DESKTOP_ID, R.string.Link_desktop, R.string.Logined, R.drawable.ic_bot_desktop_logged)

val InternalBots = listOf(InternalBuy, InternalSwap, InternalMember, InternalReferral, InternalLinkDesktop, InternalSupport)

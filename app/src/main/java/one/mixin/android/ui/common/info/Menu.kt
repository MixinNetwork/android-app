package one.mixin.android.ui.common.info

import one.mixin.android.vo.App
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.User

data class Menu(
    val title: String,
    val subtitle: String? = null,
    val style: MenuStyle = MenuStyle.Normal,
    val action: () -> Unit
)

enum class MenuStyle {
    Normal, Danger
}

sealed class Info
sealed class UserInfo(open val u: User) : Info()
data class BotInfo(override val u: User, val app: App) : UserInfo(u)
data class ProfileInfo(override val u: User) : UserInfo(u)
sealed class GroupInfo(val c: Conversation) : Info()
package one.mixin.android.ui.conversation.adapter

class Menu(
    val type: MenuType,
    val nameRes: Int?,
    val icon: Int?,
    val iconUrl: String?,
    var homeUri: String? = null,
    var name: String? = null
)

sealed class MenuType {
    object Camera : MenuType()
    object Transfer : MenuType()
    object Voice : MenuType()
    object File : MenuType()
    object Contact : MenuType()
    object App : MenuType()
}

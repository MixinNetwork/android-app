package one.mixin.android.ui.conversation.adapter

import one.mixin.android.vo.AppItem

class Menu(
    val type: MenuType,
    val nameRes: Int?,
    val icon: Int?,
    val app: AppItem?,
)

sealed class MenuType {
    object Camera : MenuType()
    object Transfer : MenuType()
    object Voice : MenuType()
    object File : MenuType()
    object Contact : MenuType()
    object App : MenuType()
    object Location : MenuType()
}

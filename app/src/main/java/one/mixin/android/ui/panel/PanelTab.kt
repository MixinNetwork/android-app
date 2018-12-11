package one.mixin.android.ui.panel

class PanelTab(
    val type: PanelTabType,
    val icon: Int?,
    val iconUrl: String?,
    val checkable: Boolean = false,
    val expandable: Boolean = false,
    var checked: Boolean = false
)

sealed class PanelTabType {
    object Gallery : PanelTabType()
    object Transfer : PanelTabType()
    object Voice : PanelTabType()
    object File : PanelTabType()
    object Contact : PanelTabType()
    object App : PanelTabType()
}
package one.mixin.android.ui.search

sealed class SearchType(val index: Int) {
    object Asset : SearchType(1)
    object Chat : SearchType(2)
    object User : SearchType(3)
    object Message : SearchType(4)
}

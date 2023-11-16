package one.mixin.android.ui.search

sealed class SearchType(val index: Int)

object TypeAsset : SearchType(1)

object TypeChat : SearchType(2)

object TypeUser : SearchType(3)

object TypeMessage : SearchType(4)

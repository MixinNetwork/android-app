package one.mixin.android.ui.search

sealed class SearchType(val index: Int)

object TypeAsset : SearchType(1)

object TypeChat : SearchType(2)

object TypeUser : SearchType(3)

object TypeMessage : SearchType(4)

object TypeMarket : SearchType(5)

object TypeBot : SearchType(6)

object TypeDapp : SearchType(7)

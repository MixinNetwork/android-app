package one.mixin.android.ui.home.inscription.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.tip.wc.SortOrder

data class SortMenuData(val menu: SortOrder, @param:DrawableRes val iconResId: Int, @param:StringRes val title: Int)

package one.mixin.android.ui.home.inscription.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.tip.wc.SortOrder

data class SortMenuData(val menu: SortOrder, @DrawableRes val iconResId: Int, @StringRes val title: Int)

package one.mixin.android.extension

import androidx.compose.ui.unit.Dp
import one.mixin.android.MixinApplication

val Int.dp: Int
    get() = MixinApplication.appContext.dpToPx(this.toFloat())
val Int.composeDp: Dp
    get() = Dp(this.toFloat())
val Int.sp: Int
    get() = MixinApplication.appContext.spToPx(this.toFloat())
val Float.dp: Int
    get() = MixinApplication.appContext.dpToPx(this)
val Float.composeDp: Dp
    get() = Dp(this)
val Float.sp: Int
    get() = MixinApplication.appContext.spToPx(this)

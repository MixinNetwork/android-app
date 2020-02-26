package one.mixin.android.extension

import one.mixin.android.MixinApplication

val Int.dp: Int
    get() = MixinApplication.appContext.dpToPx(this.toFloat())
val Float.dp: Int
    get() = MixinApplication.appContext.dpToPx(this)

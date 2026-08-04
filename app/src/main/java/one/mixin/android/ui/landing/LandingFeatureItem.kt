package one.mixin.android.ui.landing

import androidx.annotation.DrawableRes

data class LandingFeatureItem(
    @param:DrawableRes val imageResId: Int,
    val title: String,
    val description: String,
)

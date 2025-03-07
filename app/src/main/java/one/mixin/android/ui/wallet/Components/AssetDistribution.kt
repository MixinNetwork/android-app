package one.mixin.android.ui.wallet.components

data class AssetDistribution(
    val symbol: String,
    val percentage: Float, // 0.0 - 1.0
    val icons: List<String>,
    val count: Int = 1
)
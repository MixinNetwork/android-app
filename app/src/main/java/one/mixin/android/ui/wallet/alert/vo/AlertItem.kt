package one.mixin.android.ui.wallet.alert.vo

class AlertItem(
    val assetId: String,
    val iconUrl: String,
    val symbol: String,
    val type: AlertType,
    val frequency: AlertFrequency,
    val value: String,
    val lang: String,
    val createdAt: String
)

package one.mixin.android.ui.wallet

fun selectLocalizedMarketDescription(
    descriptions: Map<String, String>,
    language: String,
): String? = descriptions[language]?.takeIf { it.isNotBlank() }

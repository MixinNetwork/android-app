package one.mixin.android.billing

import com.android.billingclient.api.ProductDetails

data class SubscriptionPlanInfo(
    val planId: String,
    val pricingPhaseList: List<PricingPhaseInfo>,
    val offerToken: String
) {
    companion object {
        fun fromProductDetails(productDetails: ProductDetails): List<SubscriptionPlanInfo> {
            return productDetails.subscriptionOfferDetails?.map { offerDetail ->
                SubscriptionPlanInfo(
                    planId = offerDetail.basePlanId,
                    pricingPhaseList = offerDetail.pricingPhases.pricingPhaseList.map { phase ->
                        PricingPhaseInfo(
                            formattedPrice = phase.formattedPrice,
                            priceAmountMicros = phase.priceAmountMicros,
                            priceCurrencyCode = phase.priceCurrencyCode,
                            billingPeriod = phase.billingPeriod,
                            billingCycleCount = phase.billingCycleCount,
                            recurrenceMode = phase.recurrenceMode,
                            isIntroductoryPrice = offerDetail.offerId != null
                        )
                    },
                    offerToken = offerDetail.offerToken
                )
            } ?: emptyList()
        }
    }
}

data class PricingPhaseInfo(
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriod: String,
    val billingCycleCount: Int,
    val recurrenceMode: Int,
    val isIntroductoryPrice: Boolean = false
) {
    fun getPriceAmount(): Double = priceAmountMicros / 1_000_000.00
}

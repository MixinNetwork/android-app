package one.mixin.android.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Manager class for handling in-app purchases and subscriptions
 */
class BillingManager(private val context: Context) {

    private lateinit var billingClient: BillingClient
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.None)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus

    // Map to store product details for later use
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Timber.e("User cancelled the purchase flow.")
        } else {
            Timber.e("Billing error: ${billingResult.debugMessage}")
        }
    }

    /**
     * Initialize the BillingClient
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases() // Important: Enable pending purchase support
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // BillingClient is ready. You can query ProductDetails.
                    Timber.e("BillingClient setup successfully")
                    queryProductDetails() // Get product details
                    queryActiveSubscriptions() // Query active subscriptions
                } else {
                    Timber.e("BillingClient setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Timber.e("Billing service disconnected. Retrying...")
                // Optional: Try to reconnect
            }
        })
    }

    /**
     * Query subscription product details
     */
    private fun queryProductDetails() {
        val productIds = listOf(
            PRODUCT_ID
        )

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.e("Found ${productDetailsList.size} product details")
                for (productDetails in productDetailsList) {
                    productDetailsMap[productDetails.productId] = productDetails
                    Timber.e("Product details loaded: ${productDetails.productId}")

                    // Print more product information for debugging
                    Timber.e("Product name: ${productDetails.name}")
                    Timber.e("Product description: ${productDetails.description}")

                    // Print subscription plan information
                    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                    Timber.e("Subscription plans count: ${subscriptionOfferDetails?.size ?: 0}")

                    subscriptionOfferDetails?.forEachIndexed { index, offerDetails ->
                        Timber.e("Plan #${index + 1} - Offer ID: ${offerDetails.offerId ?: "Default"}")
                        Timber.e("Plan #${index + 1} - Offer token: ${offerDetails.offerToken}")
                        Timber.e("Plan #${index + 1} - Base plan ID: ${offerDetails.basePlanId}")

                        // Print pricing phase information
                        val pricingPhaseList = offerDetails.pricingPhases.pricingPhaseList
                        Timber.e("Plan #${index + 1} - Pricing phases: ${pricingPhaseList.size}")

                        pricingPhaseList.forEachIndexed { phaseIndex, phase ->
                            Timber.e("Plan #${index + 1} - Phase #${phaseIndex + 1} - Price: ${phase.formattedPrice}")
                            Timber.e("Plan #${index + 1} - Phase #${phaseIndex + 1} - Billing period: ${phase.billingPeriod}")
                            Timber.e("Plan #${index + 1} - Phase #${phaseIndex + 1} - Recurrence mode: ${phase.recurrenceMode}")
                            Timber.e("Plan #${index + 1} - Phase #${phaseIndex + 1} - Billing cycle count: ${phase.billingCycleCount}")
                        }
                    }
                }
            } else {
                Timber.e("Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Query user's active subscriptions
     */
    private fun queryActiveSubscriptions() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchasesList)
            } else {
                Timber.e("Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Handle purchase result
     */
    private fun handlePurchase(purchase: Purchase) {
        // Verify purchase state
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge subscription notification
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Timber.e("Purchase acknowledged: ${purchase.products}")
                    } else {
                        Timber.e("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            }

            // Update subscription status
            updateSubscriptionStatus(purchase)
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Timber.e("Purchase pending: ${purchase.products}")
        }
    }

    /**
     * Process multiple purchase results
     */
    private fun processPurchases(purchasesList: List<Purchase>) {
        // If there are no active subscriptions, set status to None
        if (purchasesList.isEmpty()) {
            _subscriptionStatus.value = SubscriptionStatus.None
            return
        }

        // Process each purchase
        for (purchase in purchasesList) {
            handlePurchase(purchase)
        }
    }

    /**
     * Update subscription status
     */
    private fun updateSubscriptionStatus(purchase: Purchase) {
        val products = purchase.products

        // Determine subscription level
        val status = when {
            products.contains(PRODUCT_ID) -> SubscriptionStatus.Basic
            else -> SubscriptionStatus.None
        }

        _subscriptionStatus.value = status
        Timber.e("Subscription status updated: $status")
    }

    /**
     * Launch subscription flow
     */
    fun launchSubscriptionFlow(
        productId: String,
        activity: android.app.Activity,
        onError: (String) -> Unit
    ) {
        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            onError("Product details not found for $productId")
            return
        }

        // Get subscription plan
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            onError("No offer token found for $productId")
            return
        }

        // Build subscription parameters
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        // Launch purchase flow
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onError("Failed to launch billing flow: ${billingResult.debugMessage}")
        }
    }

    /**
     * Manually refresh subscriptions, can be called from UI
     */
    fun refreshSubscriptions() {
        if (::billingClient.isInitialized && billingClient.isReady) {
            Timber.e("Manually refreshing subscription status")
            queryActiveSubscriptions()
        } else {
            Timber.e("BillingClient not initialized or not ready, cannot refresh")
            // Try to re-initialize
            initialize()
        }
    }

    /**
     * Subscription status enum
     */
    enum class SubscriptionStatus {
        None,
        Basic,
        Standard,
        Premium
    }

    companion object {
        private const val TAG = "BillingManager"

        // Subscription product ID
        const val PRODUCT_ID = "one.mixin.messenger.membership"

        // Singleton instance
        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

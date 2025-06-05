package one.mixin.android.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import com.android.billingclient.api.ProductDetails.PricingPhase
import one.mixin.android.session.Session
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class SubscriptionProcessStatus {
    object None : SubscriptionProcessStatus() // Initial or not subscribed
    object Loading : SubscriptionProcessStatus() // Operations in progress
    data class Subscribed(val productId: String, val purchaseToken: String) : SubscriptionProcessStatus()
    data class Error(val message: String, val billingResult: BillingResult? = null) : SubscriptionProcessStatus()
    object UserCancelled : SubscriptionProcessStatus()
    object PurchasePending : SubscriptionProcessStatus()
}

class BillingManager private constructor(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private lateinit var billingClient: BillingClient

    // StateFlow for product details
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    // StateFlow for subscription status
    private val _subscriptionStatus =
        MutableStateFlow<SubscriptionProcessStatus>(SubscriptionProcessStatus.None)
    val subscriptionStatus: StateFlow<SubscriptionProcessStatus> = _subscriptionStatus.asStateFlow()

    private var currentOrderId: String? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        coroutineScope.launch {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (purchases != null) {
                        Timber.i("onPurchasesUpdated: OK, ${purchases.size} purchases received.")
                        processNewPurchases(purchases)
                    } else {
                        Timber.w("onPurchasesUpdated: OK, but purchases list is null.")
                        // This case should ideally not happen if responseCode is OK
                        _subscriptionStatus.value = SubscriptionProcessStatus.Error("Purchase successful but no purchase data received.")
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Timber.i("onPurchasesUpdated: User cancelled the purchase flow.")
                    _subscriptionStatus.value = SubscriptionProcessStatus.UserCancelled
                }
                else -> {
                    Timber.e("onPurchasesUpdated: Error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                    _subscriptionStatus.value = SubscriptionProcessStatus.Error(
                        "Purchase failed: ${billingResult.debugMessage}",
                        billingResult
                    )
                }
            }
        }
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        _subscriptionStatus.value = SubscriptionProcessStatus.Loading
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases() // Recommended
            .build()

        connectToBillingService()
    }

    private fun connectToBillingService() {
        if (!billingClient.isReady) {
            Timber.d("Connecting to Billing Service...")
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    coroutineScope.launch {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Timber.i("BillingClient setup successfully.")
                            // Query for existing purchases and product details
                            queryProductDetailsInternal(PRODUCT_ID)
                            queryAndProcessExistingPurchases()
                        } else {
                            Timber.e("BillingClient setup failed: ${billingResult.debugMessage}")
                            _subscriptionStatus.value = SubscriptionProcessStatus.Error(
                                "Billing setup failed: ${billingResult.debugMessage}",
                                billingResult
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.w("BillingClient service disconnected. Retrying connection...")
                    // Implement your retry logic here, e.g., with backoff
                    // For simplicity, just try to reconnect after a delay
                    coroutineScope.launch {
                        delay(RECONNECT_TIMER_MILLISECONDS)
                        connectToBillingService()
                    }
                }
            })
        } else {
            Timber.i("BillingClient already connected.")
            // If already connected, ensure products and purchases are queried
            coroutineScope.launch {
                queryProductDetailsInternal(PRODUCT_ID)
                queryAndProcessExistingPurchases()
            }
        }
    }

    /**
     * Query details for a specific subscription product.
     * This is usually called once during initialization.
     */
    private suspend fun queryProductDetailsInternal(productId: String) {
        if (!billingClient.isReady) {
            Timber.w("queryProductDetailsInternal: BillingClient not ready.")
            _subscriptionStatus.value = SubscriptionProcessStatus.Error("Billing service not ready to query products.")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        Timber.d("Querying product details for: $productId")
        try {
            val result = billingClient.queryProductDetails(params) // Billing lib 7.x uses this directly
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val foundDetails = result.productDetailsList?.find { it.productId == productId }
                if (foundDetails != null) {
                    _productDetails.value = foundDetails
                    Timber.i("Product details loaded for: ${foundDetails.productId}")
                    logProductDetails(foundDetails)
                    // If not already subscribed, set to None, otherwise queryAndProcessExistingPurchases will update it
                    if (_subscriptionStatus.value is SubscriptionProcessStatus.Loading || _subscriptionStatus.value is SubscriptionProcessStatus.Error) {
                        // Check current status to avoid overriding a Subscribed state if queryAndProcessExistingPurchases runs first
                        if (_subscriptionStatus.value !is SubscriptionProcessStatus.Subscribed) {
                            _subscriptionStatus.value = SubscriptionProcessStatus.None
                        }
                    }
                } else {
                    Timber.w("Product details not found for $productId in the response.")
                    _productDetails.value = null
                    if (_subscriptionStatus.value !is SubscriptionProcessStatus.Subscribed) {
                        _subscriptionStatus.value = SubscriptionProcessStatus.Error("Product $productId not found.")
                    }
                }
            } else {
                Timber.e("Failed to query product details: ${result.billingResult.debugMessage}")
                _productDetails.value = null
                if (_subscriptionStatus.value !is SubscriptionProcessStatus.Subscribed) {
                    _subscriptionStatus.value = SubscriptionProcessStatus.Error(
                        "Failed to query product details: ${result.billingResult.debugMessage}",
                        result.billingResult
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while querying product details.")
            _productDetails.value = null
            if (_subscriptionStatus.value !is SubscriptionProcessStatus.Subscribed) {
                _subscriptionStatus.value = SubscriptionProcessStatus.Error("Exception querying products: ${e.localizedMessage}")
            }
        }
    }


    /**
     * Queries active subscriptions the user might already own.
     * Called on initialization and refresh.
     */
    suspend fun queryAndProcessExistingPurchases() {
        if (!billingClient.isReady) {
            Timber.w("queryAndProcessExistingPurchases: BillingClient not ready.")
            // Don't set error here if it's just a refresh, setup failure will handle it.
            return
        }

        Timber.d("Querying existing subscriptions...")
        _subscriptionStatus.value = SubscriptionProcessStatus.Loading
        try {
            val purchasesResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            )

            if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.i("Existing subscriptions query successful. Found ${purchasesResult.purchasesList.size} subscriptions.")
                processExistingPurchases(purchasesResult.purchasesList)
            } else {
                Timber.e("Failed to query existing subscriptions: ${purchasesResult.billingResult.debugMessage}")
                _subscriptionStatus.value = SubscriptionProcessStatus.Error(
                    "Failed to query purchases: ${purchasesResult.billingResult.debugMessage}",
                    purchasesResult.billingResult
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception querying existing purchases.")
            _subscriptionStatus.value = SubscriptionProcessStatus.Error("Exception querying purchases: ${e.localizedMessage}")
        }
    }

    private suspend fun processNewPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                Timber.i("Processing new purchase for ${purchase.products.joinToString()}: Token: ${purchase.purchaseToken}")
                Timber.w("TODO: Send new purchase token to backend: ${purchase.purchaseToken} for product ${purchase.products.firstOrNull()}")
                acknowledgePurchaseIfNeeded(purchase) // This will update status upon successful acknowledgement
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                Timber.i("New purchase is PENDING for ${purchase.products.joinToString()}. User needs to complete the payment.")
                _subscriptionStatus.value = SubscriptionProcessStatus.PurchasePending
            } else {
                Timber.w("New purchase for ${purchase.products.joinToString()} is not in PURCHASED state: ${purchase.purchaseState}")
            }
        }
    }

    private suspend fun processExistingPurchases(purchases: List<Purchase>) {
        var isActiveSubscriptionFound = false
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                Timber.i("Found existing active subscription for ${purchase.products.joinToString()}: Token: ${purchase.purchaseToken}")
                isActiveSubscriptionFound = true
                acknowledgePurchaseIfNeeded(purchase) // This will update status
                break
            }
        }

        if (!isActiveSubscriptionFound) {
            Timber.i("No active '${PRODUCT_ID}' subscription found among existing purchases.")
            if (_subscriptionStatus.value !is SubscriptionProcessStatus.Error && _subscriptionStatus.value !is SubscriptionProcessStatus.PurchasePending) {
                _subscriptionStatus.value = SubscriptionProcessStatus.None
            }
        }
    }

    private suspend fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            Timber.d("Acknowledging purchase: ${purchase.orderId}")
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            try {
                val ackResult = billingClient.acknowledgePurchase(ackParams) // Direct call in 7.x
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.i("Purchase acknowledged: ${purchase.orderId}")
                    _subscriptionStatus.value = SubscriptionProcessStatus.Subscribed(PRODUCT_ID, purchase.purchaseToken)
                } else {
                    Timber.e("Failed to acknowledge purchase ${purchase.orderId}: ${ackResult.debugMessage}")
                    _subscriptionStatus.value = SubscriptionProcessStatus.Error(
                        "Failed to acknowledge purchase: ${ackResult.debugMessage}",
                        ackResult
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during purchase acknowledgement for ${purchase.orderId}")
                _subscriptionStatus.value = SubscriptionProcessStatus.Error("Exception acknowledging purchase: ${e.localizedMessage}")
            }
        } else {
            Timber.i("Purchase already acknowledged: ${purchase.orderId}")
            _subscriptionStatus.value = SubscriptionProcessStatus.Subscribed(PRODUCT_ID, purchase.purchaseToken)
        }
    }


    /**
     * Launch the billing flow for subscribing.
     */
    fun launchSubscriptionFlow(activity: Activity, productId: String = PRODUCT_ID, planId: String? = null, orderId: String? = null) {
        if (!billingClient.isReady) {
            Timber.e("launchSubscriptionFlow: BillingClient not ready.")
            _subscriptionStatus.value = SubscriptionProcessStatus.Error("Billing service not ready to launch flow.")
            // Optionally, notify UI to show a specific error message
            return
        }

        val currentProductDetails = _productDetails.value
        if (currentProductDetails == null || currentProductDetails.productId != productId) {
            Timber.e("launchSubscriptionFlow: Product details not loaded for $productId. Attempting to query.")
            _subscriptionStatus.value = SubscriptionProcessStatus.Error("Product details for $productId not available.")
            // Optionally, trigger a refresh of product details here and ask user to retry
            coroutineScope.launch { queryProductDetailsInternal(productId) }
            return
        }

        val offerDetails = if (planId != null) {
            currentProductDetails.subscriptionOfferDetails?.find { it.basePlanId == planId }
        } else {
            currentProductDetails.subscriptionOfferDetails?.firstOrNull()
        }

        val offerToken = offerDetails?.offerToken
        if (offerToken == null) {
            Timber.e("launchSubscriptionFlow: No offer token found for $productId with planId $planId")
            _subscriptionStatus.value = SubscriptionProcessStatus.Error("No subscription offers found for $productId with planId $planId.")
            return
        }

        currentOrderId = orderId

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(currentProductDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        if (orderId != null) {
            val selfId = Session.getAccount()!!.userId
            Timber.d("Setting orderId $selfId as obfuscatedAccountId")
            billingFlowParamsBuilder.setObfuscatedAccountId(selfId)
            Timber.d("Setting orderId $orderId as setObfuscatedProfileId")
            billingFlowParamsBuilder.setObfuscatedProfileId(orderId)
        }

        val billingFlowParams = billingFlowParamsBuilder.build()

        Timber.d("Launching billing flow for $productId with offer token $offerToken and orderId $orderId")
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("Failed to launch billing flow: ${billingResult.debugMessage}")
            _subscriptionStatus.value = SubscriptionProcessStatus.Error(
                "Failed to launch billing flow: ${billingResult.debugMessage}",
                billingResult
            )
        } else {
            Timber.i("Billing flow launched successfully.")
            // Result will be delivered to PurchasesUpdatedListener
        }
    }

    /**
     * Manually refresh subscription status and product details.
     */
    fun refresh() {
        coroutineScope.launch {
            if (billingClient.isReady) {
                Timber.i("Manual refresh triggered.")
                _subscriptionStatus.value = SubscriptionProcessStatus.Loading
                queryProductDetailsInternal(PRODUCT_ID) // Re-fetch product details
                queryAndProcessExistingPurchases()      // Re-check active subscriptions
            } else {
                Timber.w("Manual refresh: BillingClient not ready. Attempting to reconnect.")
                connectToBillingService() // This will trigger queries upon successful connection
            }
        }
    }

    private fun logProductDetails(productDetails: ProductDetails) {
        Timber.d("--- Product Details for ${productDetails.productId} ---")
        Timber.d("Name: ${productDetails.name}")
        Timber.d("Title: ${productDetails.title}")
        Timber.d("Description: ${productDetails.description}")
        Timber.d("Type: ${productDetails.productType}")

        productDetails.subscriptionOfferDetails?.forEachIndexed { index, offerDetails ->
            Timber.d("  Offer #${index + 1}:")
            Timber.d("    Offer ID: ${offerDetails.offerId ?: "N/A (Default Base Plan)"}")
            Timber.d("    Base Plan ID: ${offerDetails.basePlanId}")
            Timber.d("    Offer Token: ${offerDetails.offerToken}")

            offerDetails.pricingPhases.pricingPhaseList.forEachIndexed { phaseIndex, phase ->
                Timber.d("    Pricing Phase #${phaseIndex + 1}:")
                Timber.d("      Formatted Price: ${phase.formattedPrice}")
                Timber.d("      Price Amount (micros): ${phase.priceAmountMicros}")
                Timber.d("      Price Currency Code: ${phase.priceCurrencyCode}")
                Timber.d("      Billing Period: ${phase.billingPeriod}")

                val recurrenceModeInt = phase.recurrenceMode
                Timber.d("      Raw Recurrence Mode Value: $recurrenceModeInt")

                val recurrenceModeString = when (recurrenceModeInt) {
                    0 -> "UNKNOWN_RECURRENCE_MODE (0)"
                    1 -> "NON_RECURRING (1)"
                    2 -> "INFINITE_RECURRING (2)"
                    3 -> "FINITE_RECURRING (3)"
                    else -> "UNEXPECTED_VALUE ($recurrenceModeInt)"
                }
                Timber.d("      Recurrence Mode: $recurrenceModeString")
                Timber.d("      Billing Cycle Count: ${phase.billingCycleCount}")
            }
            offerDetails.offerTags.forEach { tag -> Timber.d("    Offer Tag: $tag") }
        }
        Timber.d("--- End Product Details for ${productDetails.productId} ---")
    }

    /**
     * Call this when the BillingManager is no longer needed, e.g., in ViewModel's onCleared().
     */
    fun destroy() {
        Timber.d("Destroying BillingManager.")
        if (::billingClient.isInitialized && billingClient.isReady) {
            billingClient.endConnection()
        }
        coroutineScope.cancel() // Cancel all coroutines started by this manager
        INSTANCE = null
    }

    companion object {
        const val PRODUCT_ID = "one.mixin.messenger.membership" // Your subscription product ID
        const val PLAN_ID_100 = "one-mixin-messenger-membership-100"
        private const val RECONNECT_TIMER_MILLISECONDS = 1000L * 5 // 5 seconds

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context, coroutineScope: CoroutineScope? = null): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(
                    context.applicationContext,
                    coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) // Default scope if not provided
                ).also { INSTANCE = it }
            }
        }
    }
}


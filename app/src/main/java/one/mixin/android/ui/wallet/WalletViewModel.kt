package one.mixin.android.ui.wallet

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.CardRequirements
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.Constants.PAYMENTS_GATEWAY
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.api.request.TickerRequest
import one.mixin.android.api.response.CheckoutPaymentResponse
import one.mixin.android.api.response.CreateSessionResponse
import one.mixin.android.api.response.TickerResponse
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.putString
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshTopAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.checkout.PaymentRequest
import one.mixin.android.vo.sumsub.TokenResponse
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager,
) : ViewModel() {

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    fun assetItemsNotHidden(): LiveData<List<AssetItem>> = assetRepository.assetItemsNotHidden()

    @ExperimentalPagingApi
    fun snapshots(
        assetId: String,
        type: String? = null,
        otherType: String? = null,
        initialLoadKey: Int? = 0,
        orderByAmount: Boolean = false,
    ): LiveData<PagingData<SnapshotItem>> =
        assetRepository.snapshots(assetId, type, otherType, orderByAmount)
            .cachedIn(viewModelScope)

    fun snapshotsFromDb(
        id: String,
        type: String? = null,
        otherType: String? = null,
        initialLoadKey: Int? = 0,
        orderByAmount: Boolean = false,
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.snapshotsFromDb(id, type, otherType, orderByAmount),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    fun snapshotsByUserId(
        opponentId: String,
        initialLoadKey: Int? = 0,
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.snapshotsByUserId(opponentId),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    suspend fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<AssetItem> = assetRepository.assetItem(id)

    suspend fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    suspend fun verifyPin(code: String) = withContext(Dispatchers.IO) {
        accountRepository.verifyPin(code)
    }

    fun checkAndRefreshUsers(userIds: List<String>) = viewModelScope.launch {
        val existUsers = userRepository.findUserExist(userIds)
        val queryUsers = userIds.filter {
            !existUsers.contains(it)
        }
        if (queryUsers.isEmpty()) {
            return@launch
        }
        jobManager.addJobInBackground(RefreshUserJob(queryUsers))
    }

    suspend fun updateAssetHidden(id: String, hidden: Boolean) = assetRepository.updateHidden(id, hidden)

    fun hiddenAssets(): LiveData<List<AssetItem>> = assetRepository.hiddenAssetItems()

    fun addresses(id: String) = assetRepository.addresses(id)

    fun allSnapshots(type: String? = null, otherType: String? = null, initialLoadKey: Int? = 0, orderByAmount: Boolean = false): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.allSnapshots(type, otherType, orderByAmount = orderByAmount),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE * 2)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    suspend fun refreshPendingDeposits(asset: AssetItem) = assetRepository.pendingDeposits(asset.assetId, asset.getDestination(), asset.getTag())

    suspend fun clearPendingDepositsByAssetId(assetId: String) = assetRepository.clearPendingDepositsByAssetId(assetId)

    suspend fun findSnapshotByTransactionHashList(assetId: String, hashList: List<String>) = assetRepository.findSnapshotByTransactionHashList(assetId, hashList)

    suspend fun insertPendingDeposit(snapshot: List<Snapshot>) = assetRepository.insertPendingDeposit(snapshot)

    suspend fun getAsset(assetId: String) = withContext(Dispatchers.IO) {
        assetRepository.asset(assetId)
    }

    fun refreshHotAssets() {
        jobManager.addJobInBackground(RefreshTopAssetsJob())
    }

    fun refreshAsset(assetId: String? = null) {
        jobManager.addJobInBackground(RefreshAssetsJob(assetId))
    }

    suspend fun queryAsset(query: String): List<AssetItem> = assetRepository.queryAsset(query)

    fun saveAssets(hotAssetList: List<TopAssetItem>) {
        hotAssetList.forEach {
            jobManager.addJobInBackground(RefreshAssetsJob(it.assetId))
        }
    }

    suspend fun findAssetItemById(assetId: String) = assetRepository.findAssetItemById(assetId)

    suspend fun findOrSyncAsset(assetId: String): AssetItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.findOrSyncAsset(assetId)
        }
    }

    fun upsetAsset(asset: Asset) = viewModelScope.launch(Dispatchers.IO) {
        assetRepository.insert(asset)
    }

    fun observeTopAssets() = assetRepository.observeTopAssets()

    fun getUser(userId: String) = userRepository.getUserById(userId)

    suspend fun errorCount() = accountRepository.errorCount()

    fun refreshSnapshots(
        assetId: String? = null,
        offset: String? = null,
        opponent: String? = null,
    ) {
        jobManager.addJobInBackground(RefreshSnapshotsJob(assetId, offset, opponent))
    }

    suspend fun getSnapshots(assetId: String, offset: String?, limit: Int, opponent: String?, destination: String?, tag: String?) =
        assetRepository.getSnapshots(
            assetId,
            offset,
            limit,
            opponent,
            destination,
            if (tag?.isEmpty() == true) {
                null
            } else {
                tag
            },
        )

    suspend fun findAssetsByIds(ids: List<String>) = assetRepository.findAssetsByIds(ids)
    suspend fun assetItems() = assetRepository.assetItems()

    suspend fun fuzzySearchAssets(query: String?): List<AssetItem>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            val escapedQuery = query.trim().escapeSql()
            assetRepository.fuzzySearchAssetIgnoreAmount(escapedQuery)
        }

    fun updateRecentSearchAssets(
        defaultSharedPreferences: SharedPreferences,
        assetId: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val assetsString =
            defaultSharedPreferences.getString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, null)
        if (assetsString != null) {
            val assetsList = assetsString.split("=")
            if (assetsList.isNullOrEmpty()) {
                defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, assetId)
                return@launch
            }

            val arr = assetsList.filter { it != assetId }
                .toMutableList()
                .also {
                    if (it.size >= Constants.RECENT_SEARCH_ASSETS_MAX_COUNT) {
                        it.dropLast(1)
                    }
                    it.add(0, assetId)
                }
            defaultSharedPreferences.putString(
                Constants.Account.PREF_RECENT_SEARCH_ASSETS,
                arr.joinToString("="),
            )
        } else {
            defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, assetId)
        }
    }

    suspend fun ticker(assetId: String, offset: String?) = assetRepository.ticker(assetId, offset)

    suspend fun ticker(tickerRequest: TickerRequest): MixinResponse<TickerResponse> =
        assetRepository.ticker(tickerRequest)

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.refreshAndGetSnapshot(snapshotId)
        }
    }

    suspend fun findSnapshot(snapshotId: String): SnapshotItem? =
        assetRepository.findSnapshotById(snapshotId)

    suspend fun getExternalAddressFee(assetId: String, destination: String, tag: String?) =
        accountRepository.getExternalAddressFee(assetId, destination, tag)

    suspend fun token(): MixinResponse<TokenResponse> = assetRepository.token()

    suspend fun payment(traceRequest: PaymentRequest): MixinResponse<CheckoutPaymentResponse> = assetRepository.payment(traceRequest)

    suspend fun payment(paymentId: String): MixinResponse<CheckoutPaymentResponse> = assetRepository.payment(paymentId)

    suspend fun createSession(createSession: CreateSessionRequest): MixinResponse<CreateSessionResponse> = assetRepository.createSession(createSession)

    suspend fun getSession(sessionId: String) = assetRepository.getSession(sessionId)

    data class State(
        val googlePayAvailable: Boolean? = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // A client for interacting with the Google Pay API.
    private val paymentsClient: PaymentsClient = PaymentsUtil.createPaymentsClient(MixinApplication.appContext)

    init {
        fetchCanUseGooglePay()
    }

    /**
     * Determine the user's ability to pay with a payment method supported by your app and display
     * a Google Pay payment button.
     ) */
    private fun fetchCanUseGooglePay() {
        val isReadyToPayJson = PaymentsUtil.isReadyToPayRequest()
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())
        val task = paymentsClient.isReadyToPay(request)

        task.addOnCompleteListener { completedTask ->
            try {
                _state.update { currentState ->
                    currentState.copy(googlePayAvailable = completedTask.getResult(ApiException::class.java))
                }
            } catch (exception: ApiException) {
                Timber.w("isReadyToPay failed", exception)
            }
        }
    }

    fun getLoadPaymentDataTask(totalPrice: String, currencyCode: String): Task<PaymentData> {
        val request = PaymentDataRequest.newBuilder()
            .setTransactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice(totalPrice)
                    .setCurrencyCode(currencyCode)
                    .build(),
            )
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .setCardRequirements(
                CardRequirements.newBuilder()
                    .addAllowedCardNetworks(
                        listOf(
                            WalletConstants.CARD_NETWORK_VISA,
                            WalletConstants.CARD_NETWORK_MASTERCARD,
                        ),
                    )
                    .build(),
            )
        val params = PaymentMethodTokenizationParameters.newBuilder()
            .setPaymentMethodTokenizationType(
                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY,
            )
            .addParameter("gateway", PAYMENTS_GATEWAY)
            .addParameter("gatewayMerchantId", BuildConfig.CHCEKOUT_ID)
            .build()
        request.setPaymentMethodTokenizationParameters(params)
        return paymentsClient.loadPaymentData(request.build())
    }
}

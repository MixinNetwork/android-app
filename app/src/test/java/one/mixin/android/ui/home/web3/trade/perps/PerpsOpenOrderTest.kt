package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.birbit.android.jobqueue.config.Configuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.ResponseError
import one.mixin.android.api.request.perps.OpenOrderRequest
import one.mixin.android.api.request.perps.OpenOrderResponse
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.PerpsMarketRepository
import one.mixin.android.repository.SafeBoxStoreManager
import one.mixin.android.repository.TokenRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.startCoroutine
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PerpsOpenOrderTest {
    private lateinit var main: MixinDatabase
    private lateinit var wallet: WalletDatabase
    private lateinit var perps: PerpsDatabase
    private lateinit var jobs: MixinJobManager
    private lateinit var tokenRepository: TokenRepository
    private lateinit var marketRepository: PerpsMarketRepository
    private lateinit var routeService: RouteService
    private val viewModels = mutableListOf<PerpetualViewModel>()
    private val requests = mutableListOf<OpenOrderRequest>()
    private val request = OpenOrderRequest(
        assetId = "asset",
        marketId = "market",
        side = "long",
        amount = "10",
        leverage = 5,
        walletId = UUID.randomUUID().toString(),
        takeProfitPrice = "120",
        stopLossPrice = "80",
        leaderPositionId = "leader-position",
    )
    private val payment = OpenOrderResponse(
        orderId = "order",
        paymentUrl = "mixin://pay/receiver?asset=asset&amount=10&trace=trace",
        payAmount = "10",
        depositDestination = null,
        appId = "bot",
    )
    private var response: suspend () -> MixinResponse<OpenOrderResponse> = {
        successfulResponse()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        main = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        wallet = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        perps = Room.inMemoryDatabaseBuilder(context, PerpsDatabase::class.java)
            .setDriver(AndroidSQLiteDriver()).allowMainThreadQueries().build()
        runBlocking { perps.perpsPositionDao().getOpenPositions(request.walletId) }
        jobs = MixinJobManager(Configuration.Builder(context).inTestMode()
            .minConsumerCount(0).maxConsumerCount(1).networkUtil { 0 }.build())
        routeService = Proxy.newProxyInstance(RouteService::class.java.classLoader, arrayOf(RouteService::class.java)) { _, method, args ->
            check(method.name == "openPerpsOrder") { "Unexpected network call: ${method.name}" }
            requests += args!![0] as OpenOrderRequest
            @Suppress("UNCHECKED_CAST")
            val continuation = args.last() as Continuation<MixinResponse<OpenOrderResponse>>
            response.startCoroutine(continuation)
            COROUTINE_SUSPENDED
        } as RouteService
        marketRepository = PerpsMarketRepository(
            routeService,
            unavailableService(),
            perps,
            perps.perpsMarketDao(),
            perps.perpsFavoriteDao(),
            perps.perpsMarketCategoryDao(),
        )
        tokenRepository = TokenRepository(
            appDatabase = main,
            walletDatabase = wallet,
            tokenService = unavailableService(),
            assetService = unavailableService(),
            utxoService = unavailableService(),
            userService = unavailableService(),
            routeService = routeService,
            tokenDao = main.tokenDao(),
            tokensExtraDao = main.tokensExtraDao(),
            safeSnapshotDao = main.safeSnapshotDao(),
            addressDao = main.addressDao(),
            addressService = unavailableService(),
            hotAssetDao = main.topAssetDao(),
            traceDao = main.traceDao(),
            chainDao = main.chainDao(),
            depositDao = main.depositDao(),
            rawTransactionDao = main.rawTransactionDao(),
            outputDao = main.outputDao(),
            userDao = main.userDao(),
            inscriptionDao = main.inscriptionDao(),
            inscriptionCollectionDao = main.inscriptionCollectionDao(),
            historyPriceDao = main.historyPriceDao(),
            marketDao = main.marketDao(),
            marketCoinDao = main.marketCoinDao(),
            marketFavoredDao = main.marketFavoredDao(),
            marketCapRankDao = main.marketCapRankDao(),
            marketCategoryDao = main.marketCategoryDao(),
            alertDao = main.alertDao(),
            orderDao = wallet.orderDao(),
            web3TokenDao = wallet.web3TokenDao(),
            web3TransactionDao = wallet.web3TransactionDao(),
            web3RawTransactionDao = wallet.web3RawTransactionDao(),
            walletOutputDao = wallet.walletOutputDao(),
            web3WalletDao = wallet.web3WalletDao(),
            jobManager = jobs,
            safeBoxStoreManager = SafeBoxStoreManager(context),
            web3AddressDao = wallet.web3AddressDao(),
        )
    }

    @After
    fun tearDown() = runBlocking {
        viewModels.forEach { it.viewModelScope.cancel() }
        main.close()
        wallet.close()
        perps.close()
        withContext(Dispatchers.IO) { jobs.destroy() }
    }

    @Test
    fun unpaidOrderDoesNotBecomeAPositionOrBlockANewPreview() = runBlocking {
        val firstPreview = viewModel()
        val firstPayment = firstPreview.openPerpsOrder(request)
        val orderId = assertNotNull(requests.single().orderId)
        assertEquals(payment.copy(orderId = orderId), firstPayment)
        assertTrue(orderId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
        assertNull(perps.perpsPositionDao().getPosition(orderId))
        firstPreview.viewModelScope.cancel()

        val nextPreview = viewModel()
        assertNull(nextPreview.getOpenPerpsPosition(request.walletId, request.marketId))
        val nextPayment = nextPreview.openPerpsOrder(request)
        assertEquals(payment.copy(orderId = requests.last().orderId!!), nextPayment)
        assertNotEquals(orderId, nextPayment.orderId)
        assertEquals(listOf(request, request), requests.map { it.copy(orderId = null) })
        assertTrue(perps.perpsPositionDao().getOpenPositions(request.walletId).isEmpty())
    }

    @Test
    fun rejectedOrderDoesNotWriteAPosition() = runBlocking {
        response = { MixinResponse(ResponseError(400, 10656, "Invalid leader position")) }

        val error = assertFailsWith<MixinResponseException> { viewModel().openPerpsOrder(request) }

        assertEquals(10656, error.errorCode)
        assertEquals(listOf(request), requests.map { it.copy(orderId = null) })
        assertTrue(perps.perpsPositionDao().getOpenPositions(request.walletId).isEmpty())
    }

    @Test
    fun lostResponsePreservesTheRequestAcrossViewModelsWithoutWritingAPosition() = runBlocking {
        response = { throw IOException("Connection closed before the response") }
        val firstPreview = viewModel()

        assertFailsWith<IOException> { firstPreview.openPerpsOrder(request) }
        val original = requests.single()
        assertNotNull(original.orderId)
        firstPreview.viewModelScope.cancel()
        response = { successfulResponse() }

        viewModel().openPerpsOrder(request)

        assertEquals(listOf(original, original), requests)
        assertEquals(request, original.copy(orderId = null))
        assertTrue(perps.perpsPositionDao().getOpenPositions(request.walletId).isEmpty())
    }

    @Test
    fun missingPaymentResponsePreservesTheRequestWithoutWritingAPosition() = runBlocking {
        response = { MixinResponse() }

        assertFailsWith<DataErrorException> { viewModel().openPerpsOrder(request) }
        assertFailsWith<DataErrorException> { viewModel().openPerpsOrder(request) }

        assertEquals(2, requests.size)
        assertEquals(requests.first(), requests.last())
        assertNotNull(requests.first().orderId)
        assertEquals(request, requests.first().copy(orderId = null))
        assertTrue(perps.perpsPositionDao().getOpenPositions(request.walletId).isEmpty())
    }

    @Test
    fun cancelledRequestKeepsItsOrderIdForANewViewModel() = runBlocking<Unit> {
        response = { throw CancellationException("Preview dismissed") }
        val firstPreview = viewModel()

        assertFailsWith<CancellationException> { firstPreview.openPerpsOrder(request) }
        firstPreview.viewModelScope.cancel()
        response = { successfulResponse() }

        viewModel().openPerpsOrder(request)

        assertEquals(2, requests.size)
        assertEquals(requests.first(), requests.last())
        assertNotNull(requests.first().orderId)
    }

    @Test
    fun serverErrorsKeepTheOrderIdAcrossViewModels() = runBlocking {
        for (error in listOf(ResponseError(500, 500, "Internal error"), ResponseError(503, 10000, "Unavailable"))) {
            response = { MixinResponse(error) }
            assertFailsWith<MixinResponseException> { viewModel().openPerpsOrder(request) }
        }
        response = { throw HttpException(Response.error<Any>(503, "Unavailable".toResponseBody())) }
        assertFailsWith<HttpException> { viewModel().openPerpsOrder(request) }
        response = { successfulResponse() }

        viewModel().openPerpsOrder(request)

        assertEquals(4, requests.size)
        assertNotNull(requests.first().orderId)
        assertTrue(requests.all { it == requests.first() })
    }

    @Test
    fun changedOrderParametersUseNewIdsWithoutReplacingTheUnresolvedRequest() = runBlocking {
        response = { throw IOException("Response lost") }
        val variants = listOf(
            request,
            request.copy(assetId = "other-asset"),
            request.copy(marketId = "other-market"),
            request.copy(side = "short"),
            request.copy(amount = "11"),
            request.copy(leverage = 6),
            request.copy(walletId = UUID.randomUUID().toString()),
            request.copy(takeProfitPrice = "130"),
            request.copy(stopLossPrice = "70"),
            request.copy(leaderPositionId = "other-leader"),
            request.copy(destination = "wallet-address"),
        )

        for (variant in variants) {
            assertFailsWith<IOException> { viewModel().openPerpsOrder(variant) }
        }

        assertEquals(variants, requests.map { it.copy(orderId = null) })
        assertEquals(variants.size, requests.map { assertNotNull(it.orderId) }.toSet().size)
        assertFailsWith<IOException> { viewModel().openPerpsOrder(request) }
        assertEquals(requests.first(), requests.last())
    }

    @Test
    fun nonPayableOrderReportsTheReasonWithoutAutomaticallyCreatingAnotherOrder() = runBlocking {
        response = { throw IOException("Response lost") }
        assertFailsWith<IOException> { viewModel().openPerpsOrder(request) }
        val reason = "Order has already been paid"
        response = { MixinResponse(ResponseError(400, 10657, reason)) }

        val error = assertFailsWith<MixinResponseException> { viewModel().openPerpsOrder(request) }

        assertEquals(10657, error.errorCode)
        assertEquals(reason, error.errorDescription)
        assertEquals(2, requests.size)
        assertEquals(requests.first(), requests.last())
        response = { successfulResponse() }

        viewModel().openPerpsOrder(request)

        assertEquals(3, requests.size)
        assertNotEquals(requests.first().orderId, requests.last().orderId)
    }

    @Test
    fun explicitOrderIdIsPassedThrough() = runBlocking {
        val explicitRequest = request.copy(orderId = "019ba6bb-5c00-7b20-8eab-b1e7ab19aa2e")

        val result = viewModel().openPerpsOrder(explicitRequest)

        assertEquals(listOf(explicitRequest), requests)
        assertEquals(explicitRequest.orderId, result.orderId)
    }

    private fun successfulResponse() = MixinResponse<OpenOrderResponse>().apply {
        data = payment.copy(orderId = requireNotNull(requests.last().orderId))
    }

    private fun viewModel() = PerpetualViewModel(
        routeService,
        main.tokenDao(),
        tokenRepository,
        perps,
        perps.perpsPositionDao(),
        perps.perpsOrderDao(),
        marketRepository,
        jobs,
    ).also { viewModels += it }

    private inline fun <reified T> unavailableService(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected network call: ${method.name}")
    } as T
}

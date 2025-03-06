package one.mixin.android.di

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.google.android.gms.net.CronetProviderInstaller
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.net.cronet.okhttptransport.MixinCronetInterceptor
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.lambdapioneer.argon2kt.Argon2Kt
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Timeout
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.ALLOW_INTERVAL
import one.mixin.android.Constants.API.FOURSQUARE_URL
import one.mixin.android.Constants.API.GIPHY_URL
import one.mixin.android.Constants.API.Mixin_URL
import one.mixin.android.Constants.API.URL
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.Account.PREF_WEB3_BOT_PK
import one.mixin.android.Constants.DNS
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_URL
import one.mixin.android.Constants.RouteConfig.WEB3_URL
import one.mixin.android.MixinApplication
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.ExpiredTokenException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ContactService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.EmergencyService
import one.mixin.android.api.service.FoursquareService
import one.mixin.android.api.service.GiphyService
import one.mixin.android.api.service.MessageService
import one.mixin.android.api.service.ProvisioningService
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.api.service.TipService
import one.mixin.android.api.service.TokenService
import one.mixin.android.api.service.UserService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.api.service.Web3Service
import one.mixin.android.crypto.EncryptedProtocol
import one.mixin.android.crypto.JobSenderKey
import one.mixin.android.crypto.PinCipher
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.db.MessageHistoryDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.filterNonAscii
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.show
import one.mixin.android.extension.toUri
import one.mixin.android.job.BaseJob
import one.mixin.android.job.JobLogger
import one.mixin.android.job.JobNetworkUtil
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.MyJobService
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.session.JwtResult
import one.mixin.android.session.Session
import one.mixin.android.tip.Ephemeral
import one.mixin.android.tip.Identity
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipConstants
import one.mixin.android.tip.TipNode
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import one.mixin.android.util.ErrorHandler.Companion.AUTHENTICATION
import one.mixin.android.util.ErrorHandler.Companion.OLD_VERSION
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.LiveDataCallAdapterFactory
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.route.serializer.SafeBoxSerializer
import one.mixin.android.webrtc.CallDebugLiveData
import one.mixin.android.websocket.ChatWebSocket
import org.chromium.net.CronetEngine
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Singleton
import kotlin.math.abs

@InstallIn(SingletonComponent::class)
@Module(includes = [(BaseDbModule::class)])
object AppModule {
    private const val xServerTime = "X-Server-Time"
    private const val xRequestId = "X-Request-Id"
    private const val authorization = "Authorization"

    private const val mrAccessSign = "MR-ACCESS-SIGN"
    private const val mrAccessTimestamp = "MR-ACCESS-TIMESTAMP"

    private const val mwAccessSign = "MW-ACCESS-SIGN"
    private const val mwAccessTimestamp = "MW-ACCESS-TIMESTAMP"

    @SuppressLint("ConstantLocale")
    private val LOCALE = Locale.getDefault().language + "-" + Locale.getDefault().country
    val API_UA =
        (
            "Mixin/" + "${BuildConfig.VERSION_NAME}" +
                " (Android " + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.FINGERPRINT + "; " + "${BuildConfig.VERSION_CODE}" + "; " + LOCALE + ")"
        ).filterNonAscii()

    @Singleton
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor? {
        if (!BuildConfig.DEBUG) {
            return null
        }
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Singleton
    @Provides
    fun provideCronetEngine(app: Application): CronetEngine? {
        val ctx = app.applicationContext
        if (!ctx.isGooglePlayServicesAvailable()) {
            return null
        }
        if (!CronetProviderInstaller.isInstalled()) {
            return null
        }

        return try {
            CronetEngine.Builder(ctx)
                .addQuicHint(URL.toUri().host, 443, 443)
                .addQuicHint(Mixin_URL.toUri().host, 443, 443)
                .enableQuic(true)
                .enableHttp2(true)
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 10 * 1024)
                .build()
        } catch (e: UnsatisfiedLinkError) {
            reportException(e)
            null
        } catch (e: Exception) {
            if (e is TimeoutException || e is Timeout) {
                Timber.e(e)
            } else {
                reportException(e)
            }
            null
        }
    }

    @Singleton
    @Provides
    fun provideOkHttp(
        resolver: ContentResolver,
        httpLoggingInterceptor: HttpLoggingInterceptor?,
        engine: CronetEngine?,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.writeTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.pingInterval(15, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(false)
        builder.followRedirects(false)
        builder.dns(DNS)
        // Interceptor
        builder.addInterceptor { chain ->
            val requestId = UUID.randomUUID().toString()
            val sourceRequest = chain.request()
            val request =
                sourceRequest.newBuilder()
                    .addHeader("User-Agent", API_UA)
                    .addHeader("Accept-Language", Locale.getDefault().language)
                    .addHeader("Mixin-Device-Id", getStringDeviceId(resolver))
                    .addHeader(xRequestId, requestId)
                    .addHeader(authorization, "Bearer ${Session.signToken(Session.getAccount(), sourceRequest, requestId)}")
                    .build()
            if (MixinApplication.appContext.networkConnected()) {
                var response =
                    try {
                        chain.proceed(request)
                    } catch (e: Exception) {
                        throw e.apply {
                            if (e.isNeedSwitch()) {
                                HostSelectionInterceptor.get().switch(request)
                            }
                        }
                    }

                if (!response.isSuccessful) {
                    val code = response.code
                    if (code in 501..599) {
                        HostSelectionInterceptor.get().switch(request)
                        throw ServerErrorException(code)
                    } else if (code == 500) {
                        throw ServerErrorException(code)
                    }
                }

                var jwtResult: JwtResult? = null
                response.body?.run {
                    val bytes = this.bytes()
                    val body = bytes.toResponseBody(this.contentType())
                    response = response.newBuilder().body(body).build()
                    if (bytes.isEmpty()) return@run
                    if (request.header(xRequestId) != response.header(xRequestId)) {
                        throw DataErrorException()
                    }
                    val mixinResponse =
                        try {
                            GsonHelper.customGson.fromJson(String(bytes), MixinResponse::class.java)
                        } catch (e: JsonSyntaxException) {
                            HostSelectionInterceptor.get().switch(request)
                            throw ServerErrorException(response.code)
                        }
                    if (mixinResponse.errorCode == OLD_VERSION) {
                        MixinApplication.get().gotoOldVersionAlert()
                        return@run
                    } else if (mixinResponse.errorCode != AUTHENTICATION) {
                        return@run
                    }
                    val authorization = response.request.header(authorization)
                    if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ")) {
                        val jwt = authorization.substring(7)
                        jwtResult = Session.requestDelay(Session.getAccount(), jwt, Constants.DELAY_SECOND)
                        if (jwtResult?.isExpire == true) {
                            throw ExpiredTokenException()
                        }
                    }
                }

                if (MixinApplication.get().isOnline.get()) {
                    response.header(xServerTime)?.toLong()?.let { serverTime ->
                        val currentTime = System.currentTimeMillis()
                        if (abs(serverTime / 1000000 - System.currentTimeMillis()) >= ALLOW_INTERVAL) {
                            MixinApplication.get().gotoTimeWrong(serverTime)
                        } else if (jwtResult?.isExpire == false) {
                            jwtResult?.serverTime = serverTime / 1000000000
                            jwtResult?.currentTime = currentTime / 1000
                            val ise = IllegalStateException("Force logout. $jwtResult. request: ${request.show()}, response: ${response.show()}")
                            reportException(ise)
                            MixinApplication.get().closeAndClear()
                        }
                    }
                }

                return@addInterceptor response
            } else {
                throw NetworkException()
            }
        }
        builder.addInterceptor(HostSelectionInterceptor.get())
        httpLoggingInterceptor?.let { interceptor ->
            builder.addInterceptor(interceptor)
        }
        if (engine != null) {
            builder.addInterceptor(MixinCronetInterceptor.newBuilder(engine).build())
        }
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideHttpService(
        okHttp: OkHttpClient,
        gson: Gson,
    ): Retrofit {
        val builder =
            Retrofit.Builder()
                .baseUrl(URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttp)
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideAccountService(retrofit: Retrofit) = retrofit.create(AccountService::class.java) as AccountService

    @Singleton
    @Provides
    fun provideUserService(retrofit: Retrofit) = retrofit.create(UserService::class.java) as UserService

    @Singleton
    @Provides
    fun provideContactService(retrofit: Retrofit) = retrofit.create(ContactService::class.java) as ContactService

    @Singleton
    @Provides
    fun provideSignalKeyService(retrofit: Retrofit) = retrofit.create(SignalKeyService::class.java) as SignalKeyService

    @Singleton
    @Provides
    fun provideConversationService(retrofit: Retrofit) =
        retrofit.create(ConversationService::class.java) as ConversationService

    @Singleton
    @Provides
    fun provideMessageService(retrofit: Retrofit) = retrofit.create(MessageService::class.java) as MessageService

    @Singleton
    @Provides
    fun provideAssetService(retrofit: Retrofit) = retrofit.create(AssetService::class.java) as AssetService

    @Singleton
    @Provides
    fun provideTokenService(retrofit: Retrofit) = retrofit.create(TokenService::class.java) as TokenService

    @Singleton
    @Provides
    fun provideAuthService(retrofit: Retrofit) =
        retrofit.create(AuthorizationService::class.java) as AuthorizationService

    @Singleton
    @Provides
    fun provideAddressService(retrofit: Retrofit) = retrofit.create(AddressService::class.java) as AddressService

    @Singleton
    @Provides
    fun provideProvisioningService(retrofit: Retrofit) = retrofit.create(ProvisioningService::class.java) as ProvisioningService

    @Singleton
    @Provides
    fun provideEmergencyService(retrofit: Retrofit) = retrofit.create(EmergencyService::class.java) as EmergencyService

    @Singleton
    @Provides
    fun provideCircleService(retrofit: Retrofit) = retrofit.create(CircleService::class.java) as CircleService

    @Singleton
    @Provides
    fun provideTipService(retrofit: Retrofit) =
        retrofit.create(TipService::class.java) as TipService

    @Singleton
    @Provides
    fun provideTipNodeService(retrofit: Retrofit) =
        retrofit.create(TipNodeService::class.java) as TipNodeService

    @Singleton
    @Provides
    fun provideUtxoService(retrofit: Retrofit) =
        retrofit.create(UtxoService::class.java) as UtxoService

    @Singleton
    @Provides
    fun provideContentResolver(app: Application) = app.contentResolver as ContentResolver

    @Provides
    @Singleton
    fun provideJobNetworkUtil(
        app: Application,
        linkState: LinkState,
    ) =
        JobNetworkUtil(app.applicationContext, linkState)

    @Suppress("INACCESSIBLE_TYPE")
    @Provides
    @Singleton
    fun jobManager(
        app: Application,
        jobNetworkUtil: JobNetworkUtil,
    ): MixinJobManager {
        val builder =
            Configuration.Builder(app)
                .consumerKeepAlive(20)
                .resetDelaysOnRestart()
                .maxConsumerCount(6)
                .minConsumerCount(2)
                .injector { job ->
                    if (job is BaseJob) {
                        val entryPoint = EntryPointAccessors.fromApplication(app.applicationContext, BaseJob.JobEntryPoint::class.java)
                        entryPoint.inject(job)
                    }
                }
                .customLogger(JobLogger())
                .networkUtil(jobNetworkUtil)
        builder.scheduler(
            FrameworkJobSchedulerService
                .createSchedulerFor(app.applicationContext, MyJobService::class.java),
        )
        return MixinJobManager(builder.build())
    }

    @Provides
    @Singleton
    fun provideLinkState() = LinkState()

    @Provides
    @Singleton
    fun provideSignalProtocol(app: Application) = SignalProtocol(app.applicationContext)

    @Provides
    @Singleton
    fun provideEncryptedProtocol() = EncryptedProtocol()

    @Provides
    @Singleton
    fun provideChatWebSocket(
        @ApplicationScope applicationScope: CoroutineScope,
        okHttp: OkHttpClient,
        accountService: AccountService,
        mixinDatabase: MixinDatabase,
        pendingDatabase: PendingDatabase,
        jobManager: MixinJobManager,
        linkState: LinkState,
    ): ChatWebSocket =
        ChatWebSocket(applicationScope, okHttp, accountService, mixinDatabase, pendingDatabase, jobManager, linkState)

    @Provides
    @Singleton
    fun provideGiphyService(httpLoggingInterceptor: HttpLoggingInterceptor?): GiphyService {
        val client =
            OkHttpClient.Builder().apply {
                httpLoggingInterceptor?.let { interceptor ->
                    addNetworkInterceptor(interceptor)
                }
            }.build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(GIPHY_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build()
        return retrofit.create(GiphyService::class.java)
    }

    @Provides
    @Singleton
    fun provideFoursquareService(httpLoggingInterceptor: HttpLoggingInterceptor?): FoursquareService {
        val client =
            OkHttpClient.Builder().apply {
                httpLoggingInterceptor?.let { interceptor ->
                    addNetworkInterceptor(interceptor)
                }
            }.build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(FOURSQUARE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(client)
                .build()
        return retrofit.create(FoursquareService::class.java)
    }

    @Singleton
    @Provides
    fun provideRouteService(
        resolver: ContentResolver,
        httpLoggingInterceptor: HttpLoggingInterceptor?,
        @ApplicationContext appContext: Context,
    ): RouteService {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        builder.dns(DNS)
        val client =
            builder.apply {
                httpLoggingInterceptor?.let { interceptor ->
                    addNetworkInterceptor(interceptor)
                }
                addInterceptor { chain ->
                    val requestId = UUID.randomUUID().toString()
                    val sourceRequest = chain.request()
                    val b = sourceRequest.newBuilder()
                    b.addHeader("User-Agent", API_UA)
                        .addHeader("Accept-Language", Locale.getDefault().language)
                        .addHeader("Mixin-Device-Id", getStringDeviceId(resolver))
                        .addHeader(xRequestId, requestId)
                    val (ts, signature) = Session.getBotSignature(appContext.defaultSharedPreferences.getString(PREF_ROUTE_BOT_PK, null), sourceRequest)
                    if (!sourceRequest.url.toString().endsWith("checkout/ticker")) {
                        b.addHeader(mrAccessTimestamp, ts.toString())
                        b.addHeader(mrAccessSign, signature)
                    }
                    val request = b.build()
                    return@addInterceptor chain.proceed(request)
                }
            }.build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(ROUTE_BOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(client)
                .build()
        return retrofit.create(RouteService::class.java)
    }

    @Singleton
    @Provides
    fun provideWeb3Service(
        resolver: ContentResolver,
        httpLoggingInterceptor: HttpLoggingInterceptor?,
        @ApplicationContext appContext: Context,
    ): Web3Service {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        builder.dns(DNS)
        val client =
            builder.apply {
                addInterceptor { chain ->
                    val requestId = UUID.randomUUID().toString()
                    val sourceRequest = chain.request()
                    val b = sourceRequest.newBuilder()
                    b.addHeader("User-Agent", API_UA)
                        .addHeader("Accept-Language", Locale.getDefault().language)
                        .addHeader("Mixin-Device-Id", getStringDeviceId(resolver))
                        .addHeader(xRequestId, requestId)
                    val (ts, signature) = Session.getBotSignature(appContext.defaultSharedPreferences.getString(PREF_WEB3_BOT_PK, null), sourceRequest)
                    b.addHeader(mwAccessTimestamp, ts.toString())
                    b.addHeader(mwAccessSign, signature)
                    val request = b.build()
                    return@addInterceptor chain.proceed(request)
                }
                httpLoggingInterceptor?.let { interceptor ->
                    addNetworkInterceptor(interceptor)
                }
            }.build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(WEB3_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(client)
                .build()
        return retrofit.create(Web3Service::class.java)
    }

    @Provides
    @Singleton
    fun provideCallState() = CallStateLiveData()

    @Provides
    @Singleton
    fun provideCallDebugState() = CallDebugLiveData()

    @Provides
    @Singleton
    fun provideAudioSwitch(app: Application): AudioSwitch =
        AudioSwitch(
            app.applicationContext,
            BuildConfig.DEBUG,
            preferredDeviceList =
                listOf(
                    AudioDevice.BluetoothHeadset::class.java,
                    AudioDevice.WiredHeadset::class.java,
                    AudioDevice.Speakerphone::class.java,
                    AudioDevice.Earpiece::class.java,
                ),
        )

    @Provides
    @Singleton
    fun provideIdentity(
        tipService: TipService,
        argon2Kt: Argon2Kt,
    ) = Identity(tipService, argon2Kt)

    @Provides
    @Singleton
    fun provideEphemeral(tipService: TipService) = Ephemeral(tipService)

    @Provides
    @Singleton
    fun provideArgon2(): Argon2Kt = Argon2Kt()

    @Provides
    @Singleton
    fun provideTipNode(
        tipNodeService: TipNodeService,
        tipConfig: TipConfig,
        gson: Gson,
    ) = TipNode(tipNodeService, tipConfig, gson)

    @Provides
    @Singleton
    fun provideTipConfig() = TipConstants.tipConfig

    @Provides
    @Singleton
    fun provideTip(
        ephemeral: Ephemeral,
        identity: Identity,
        argon2Kt: Argon2Kt,
        tipNode: TipNode,
        tipService: TipService,
        accountService: AccountService,
        tipCounterSyncedLiveData: TipCounterSyncedLiveData,
    ) =
        Tip(ephemeral, identity, argon2Kt, tipService, accountService, tipNode, tipCounterSyncedLiveData)

    @Provides
    @Singleton
    fun providePinCipher(tip: Tip) = PinCipher(tip)

    @Provides
    @Singleton
    fun provideTipCounterSynced() = TipCounterSyncedLiveData()

    @Provides
    @Singleton
    fun provideTransferStatus() = TransferStatusLiveData()

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @ApplicationScope
    @Singleton
    @Provides
    fun providesApplicationScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Provides
    @Singleton
    fun provideGson() = GsonHelper.customGson

    @Provides
    @Singleton
    fun provideJson() =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
            coerceInputValues = true
            isLenient = true
        }

    @Provides
    @Singleton
    fun provideJobSenderKey(
        participantSessionDao: ParticipantSessionDao,
        signalProtocol: SignalProtocol,
        conversationApi: ConversationService,
        participantDao: ParticipantDao,
        chatWebSocket: ChatWebSocket,
        linkState: LinkState,
        messageHistoryDao: MessageHistoryDao,
    ) = JobSenderKey(
        participantSessionDao,
        signalProtocol,
        conversationApi,
        participantDao,
        chatWebSocket,
        linkState,
        messageHistoryDao,
    )

    private const val DATA_STORE_FILE_NAME = "safe_box_%s.store"

    @Singleton
    @Provides
    fun providesDataStore(
        @ApplicationContext appContext: Context,
    ): DataStore<SafeBox> {
        return DataStoreFactory.create(
            serializer = SafeBoxSerializer,
            produceFile = {
                appContext.dataStoreFile(
                    String.format(
                        DATA_STORE_FILE_NAME,
                        Session.getAccountId(),
                    ),
                )
            },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        )
    }
}

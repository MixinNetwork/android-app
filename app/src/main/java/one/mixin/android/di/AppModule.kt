package one.mixin.android.di

import android.app.Application
import android.content.ContentResolver
import android.provider.Settings
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.JsonSyntaxException
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.ALLOW_INTERVAL
import one.mixin.android.Constants.API.FOURSQUARE_URL
import one.mixin.android.Constants.API.GIPHY_URL
import one.mixin.android.Constants.API.URL
import one.mixin.android.MixinApplication
import one.mixin.android.api.ExpiredTokenException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
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
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.OffsetDao
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.di.worker.MixinWorkerFactory
import one.mixin.android.extension.filterNonAscii
import one.mixin.android.extension.networkConnected
import one.mixin.android.job.BaseJob
import one.mixin.android.job.JobLogger
import one.mixin.android.job.JobNetworkUtil
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.MyJobService
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.LiveDataCallAdapterFactory
import one.mixin.android.util.Session
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.LinkState
import one.mixin.android.websocket.ChatWebSocket
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.math.abs

@Module(includes = [(ViewModelModule::class), (BaseDbModule::class), (ReadDbModule::class)])
internal class AppModule {

    private val LOCALE = Locale.getDefault().language + "-" + Locale.getDefault().country
    private val API_UA = (
        "Mixin/" + BuildConfig.VERSION_NAME +
            " (Android " + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.FINGERPRINT + "; " + LOCALE + ")"
        ).filterNonAscii()

    private fun getDeviceId(resolver: ContentResolver): String {
        var deviceId = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)
        if (deviceId == null || deviceId == "9774d56d682e549c") {
            deviceId = FirebaseInstanceId.getInstance().id
        }
        return UUID.nameUUIDFromBytes(deviceId.toByteArray()).toString()
    }

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
    fun provideOkHttp(resolver: ContentResolver, httpLoggingInterceptor: HttpLoggingInterceptor?): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(HostSelectionInterceptor.get())
        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(StethoInterceptor())
        }
        if (httpLoggingInterceptor != null) {
            builder.addNetworkInterceptor(httpLoggingInterceptor)
        }
        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.writeTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.pingInterval(15, TimeUnit.SECONDS)

        builder.addInterceptor { chain ->
            val sourceRequest = chain.request()
            val request = sourceRequest.newBuilder()
                .addHeader("User-Agent", API_UA)
                .addHeader("Accept-Language", Locale.getDefault().language)
                .addHeader("Mixin-Device-Id", getDeviceId(resolver))
                .addHeader("Authorization", "Bearer ${Session.signToken(Session.getAccount(), sourceRequest)}")
                .build()
            if (MixinApplication.appContext.networkConnected()) {
                var response = try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    if (e.message?.contains("502") == true) {
                        throw ServerErrorException(502)
                    } else throw e.apply {
                        if (this is SocketTimeoutException || this is UnknownHostException || this is ConnectException) {
                            HostSelectionInterceptor.get().switch()
                        }
                    }
                }

                response.body?.run {
                    val bytes = this.bytes()
                    val contentType = this.contentType()
                    val body = bytes.toResponseBody(contentType)
                    response = response.newBuilder().body(body).build()
                    if (bytes.isEmpty()) return@run
                    val mixinResponse = try {
                        GsonHelper.customGson.fromJson(String(bytes), MixinResponse::class.java)
                    } catch (e: JsonSyntaxException) {
                        HostSelectionInterceptor.get().switch()
                        throw ServerErrorException(response.code)
                    }
                    if (mixinResponse.errorCode != 401) return@run
                    val authorization = response.request.header("Authorization")
                    if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ")) {
                        val jwt = authorization.substring(7)
                        if (Session.requestDelay(Session.getAccount(), jwt, Constants.DELAY_SECOND)) {
                            throw ExpiredTokenException()
                        }
                    }
                }

                if (MixinApplication.get().onlining.get()) {
                    response.header("X-Server-Time")?.toLong()?.let { serverTime ->
                        if (abs(serverTime / 1000000 - System.currentTimeMillis()) >= ALLOW_INTERVAL) {
                            MixinApplication.get().gotoTimeWrong(serverTime)
                        }
                    }
                }

                if (!response.isSuccessful) {
                    val code = response.code
                    if (code in 500..599) {
                        HostSelectionInterceptor.get().switch()
                        throw ServerErrorException(code)
                    }
                }
                return@addInterceptor response
            } else {
                throw NetworkException()
            }
        }
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideHttpService(okHttp: OkHttpClient): Retrofit {
        val builder = Retrofit.Builder()
            .baseUrl(URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
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
    fun provideContentResolver(app: Application) = app.contentResolver as ContentResolver

    @Provides
    @Singleton
    fun provideJobNetworkUtil(app: Application, linkState: LinkState) =
        JobNetworkUtil(app.applicationContext, linkState)

    @Suppress("INACCESSIBLE_TYPE")
    @Provides
    @Singleton
    fun jobManager(app: Application, appComponent: AppComponent, jobNetworkUtil: JobNetworkUtil): MixinJobManager {
        val builder = Configuration.Builder(app)
            .consumerKeepAlive(20)
            .resetDelaysOnRestart()
            .maxConsumerCount(6)
            .minConsumerCount(2)
            .injector { job ->
                if (job is BaseJob) {
                    job.inject(appComponent)
                }
            }
            .customLogger(JobLogger())
            .networkUtil(jobNetworkUtil)
        builder.scheduler(
            FrameworkJobSchedulerService
                .createSchedulerFor(app.applicationContext, MyJobService::class.java)
        )
        return MixinJobManager(builder.build())
    }

    @Provides
    fun provideWorkConfiguration(workerFactory: MixinWorkerFactory): androidx.work.Configuration {
        return androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideLinkState() = LinkState()

    @Provides
    @Singleton
    fun provideSignalProtocol(app: Application) = SignalProtocol(app.applicationContext)

    @Provides
    @Singleton
    fun provideChatWebSocket(
        okHttp: OkHttpClient,
        app: Application,
        accountService: AccountService,
        @DatabaseCategory(DatabaseCategoryEnum.BASE)
        conversationDao: ConversationDao,
        @DatabaseCategory(DatabaseCategoryEnum.BASE)
        messageDao: MessageDao,
        offsetDao: OffsetDao,
        floodMessageDao: FloodMessageDao,
        jobManager: MixinJobManager,
        linkState: LinkState,
        jobDao: JobDao
    ): ChatWebSocket =
        ChatWebSocket(okHttp, app, accountService, conversationDao, messageDao, offsetDao, floodMessageDao, jobManager, linkState, jobDao)

    @Provides
    @Singleton
    fun provideGiphyService(): GiphyService {
        val client = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(logging)
                addNetworkInterceptor(StethoInterceptor())
            }
        }.build()
        val retrofit = Retrofit.Builder()
            .baseUrl(GIPHY_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client)
            .build()
        return retrofit.create(GiphyService::class.java)
    }

    @Provides
    @Singleton
    fun provideFoursquareService(): FoursquareService {
        val client = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(logging)
                addNetworkInterceptor(StethoInterceptor())
            }
        }.build()
        val retrofit = Retrofit.Builder()
            .baseUrl(FOURSQUARE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .client(client)
            .build()
        return retrofit.create(FoursquareService::class.java)
    }

    @Provides
    @Singleton
    fun provideCallState() = CallStateLiveData()
}

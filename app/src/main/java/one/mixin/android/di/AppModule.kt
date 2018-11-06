package one.mixin.android.di

import android.app.Application
import android.content.ContentResolver
import android.provider.Settings
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.bugsnag.android.Bugsnag
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.iid.FirebaseInstanceId
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.SessionProvider
import okhttp3.internal.http2.Header
import okhttp3.logging.HttpLoggingInterceptor
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.API.GIPHY_URL
import one.mixin.android.Constants.API.URL
import one.mixin.android.MixinApplication
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.api.service.ContactService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.GiphyService
import one.mixin.android.api.service.MessageService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.OffsetDao
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.show
import one.mixin.android.job.BaseJob
import one.mixin.android.job.JobLogger
import one.mixin.android.job.JobNetworkUtil
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.MyJobService
import one.mixin.android.util.LiveDataCallAdapterFactory
import one.mixin.android.util.Session
import one.mixin.android.vo.CallState
import one.mixin.android.vo.LinkState
import one.mixin.android.websocket.ChatWebSocket
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module(includes = [(ViewModelModule::class)])
internal class AppModule {

    private val LOCALE = Locale.getDefault().language + "-" + Locale.getDefault().country
    private val API_UA = "Mixin/" + BuildConfig.VERSION_NAME +
        " (Android " + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.FINGERPRINT + "; " + LOCALE + ")"

    private fun getDeviceId(resolver: ContentResolver): String {
        var deviceId = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)
        if (deviceId == null || deviceId == "9774d56d682e549c") {
            deviceId = FirebaseInstanceId.getInstance().id
        }
        return UUID.nameUUIDFromBytes(deviceId.toByteArray()).toString()
    }

    @Singleton
    @Provides
    fun provideOkHttp(resolver: ContentResolver): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            builder.addNetworkInterceptor(logging)
            builder.addNetworkInterceptor(StethoInterceptor())
        }
        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.writeTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.pingInterval(15, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(false)
        builder.sessionProvider(object : SessionProvider {
            override fun getSession(request: Request): String {
                return "Authorization: Bearer ${Session.signToken(Session.getAccount(), request)}"
            }

            override fun getSessionHeader(request: Request): Header {
                return Header("Authorization", "Bearer ${Session.signToken(Session.getAccount(), request)}")
            }
        })

        builder.addInterceptor { chain ->
            val initTime = System.currentTimeMillis()
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", API_UA)
                .addHeader("Accept-Language", Locale.getDefault().language)
                .addHeader("Mixin-Device-Id", getDeviceId(resolver))
                .build()
            if (MixinApplication.appContext.networkConnected()) {
                val response = try {
                    chain.proceed(request).also { response ->
                        val latency = response.sentRequestAtMillis() - initTime
                        if (latency >= 300000) {
                            val ise = IllegalStateException("Request latency. init at $initTime, sent at ${response.sentRequestAtMillis()}")
                            Bugsnag.notify(ise)
                            Crashlytics.logException(ise)
                        }
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("502") == true) {
                        throw ServerErrorException(502)
                    } else throw e
                }
                if (!response.isSuccessful) {
                    val code = response.code()
                    if (code in 500..599) {
                        throw ServerErrorException(code)
                    } else if (code in 400..499) {
                        if (code == 401) {
                            val ise = IllegalStateException("Force logout. request: ${request.show()}, response: ${response.show()}")
                            Bugsnag.notify(ise)
                            Crashlytics.logException(ise)
                            MixinApplication.get().closeAndClear()
                        }
                        throw ClientErrorException(code)
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
    fun provideDb(app: Application) = MixinDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideSignalDb(app: Application) = SignalDatabase.getDatabase(app)

    @Singleton
    @Provides
    fun provideRatchetSenderKeyDao(db: SignalDatabase) = db.ratchetSenderKeyDao()

    @Singleton
    @Provides
    fun provideUserDao(db: MixinDatabase) = db.userDao()

    @Singleton
    @Provides
    fun provideConversationDao(db: MixinDatabase) = db.conversationDao()

    @Singleton
    @Provides
    fun provideMessageDao(db: MixinDatabase) = db.messageDao()

    @Singleton
    @Provides
    fun provideParticipantDao(db: MixinDatabase) = db.participantDao()

    @Singleton
    @Provides
    fun provideOffsetDao(db: MixinDatabase) = db.offsetDao()

    @Singleton
    @Provides
    fun provideAssetDao(db: MixinDatabase) = db.assetDao()

    @Singleton
    @Provides
    fun provideSnapshotDao(db: MixinDatabase) = db.snapshotDao()

    @Singleton
    @Provides
    fun provideMessageHistoryDao(db: MixinDatabase) = db.messageHistoryDao()

    @Singleton
    @Provides
    fun provideSentSenderKeyDao(db: MixinDatabase) = db.sentSenderKeyDao()

    @Singleton
    @Provides
    fun provideStickerAlbumDao(db: MixinDatabase) = db.stickerAlbumDao()

    @Singleton
    @Provides
    fun provideStickerDao(db: MixinDatabase) = db.stickerDao()

    @Singleton
    @Provides
    fun provideHyperlinkDao(db: MixinDatabase) = db.hyperlinkDao()

    @Singleton
    @Provides
    fun providesAppDao(db: MixinDatabase) = db.appDao()

    @Singleton
    @Provides
    fun providesFloodMessageDao(db: MixinDatabase) = db.floodMessageDao()

    @Singleton
    @Provides
    fun providesJobDao(db: MixinDatabase) = db.jobDao()

    @Singleton
    @Provides
    fun providesAddressDao(db: MixinDatabase) = db.addressDao()

    @Singleton
    @Provides
    fun providesResendMessageDao(db: MixinDatabase) = db.resendMessageDao()

    @Singleton
    @Provides
    fun providesStickerRelationshipDao(db: MixinDatabase) = db.stickerRelationshipDao()

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
        builder.scheduler(FrameworkJobSchedulerService
            .createSchedulerFor(app.applicationContext, MyJobService::class.java))
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
    fun provideChatWebSocket(
        okHttp: OkHttpClient,
        app: Application,
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        offsetDao: OffsetDao,
        floodMessageDao: FloodMessageDao,
        jobManager: MixinJobManager,
        linkState: LinkState,
        jobDao: JobDao
    ): ChatWebSocket =
        ChatWebSocket(okHttp, app, conversationDao, messageDao, offsetDao, floodMessageDao, jobManager, linkState, jobDao)

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
    fun provideCallState() = CallState()
}
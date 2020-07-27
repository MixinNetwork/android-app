package one.mixin.android

import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.startup.AppInitializer
import androidx.work.Configuration
import androidx.work.impl.WorkManagerInitializer
import com.bugsnag.android.Bugsnag
import com.facebook.stetho.Stetho
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import one.mixin.android.crypto.MixinSignalProtocolLogger
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.AppComponent
import one.mixin.android.di.AppInjector
import one.mixin.android.extension.clear
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MixinJobManager
import one.mixin.android.startup.FirebaseAppInitializer
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.Session
import one.mixin.android.util.language.Lingver
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.webrtc.GroupCallService
import one.mixin.android.webrtc.VoiceCallService
import one.mixin.android.webrtc.disconnect
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.uiThread
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class MixinApplication : Application(), HasAndroidInjector, Configuration.Provider, CameraXConfig.Provider {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var workConfiguration: Configuration

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var callState: CallStateLiveData

    lateinit var appComponent: AppComponent

    companion object {
        lateinit var appContext: Context
        @JvmField
        var conversationId: String? = null

        fun get(): MixinApplication = appContext as MixinApplication
    }

    override fun onCreate() {
        super.onCreate()
        init()
        SignalProtocolLoggerProvider.setProvider(MixinSignalProtocolLogger())
        appContext = applicationContext
        AndroidThreeTen.init(this)
        Lingver.init(this)
        appComponent = AppInjector.init(this)
        RxJavaPlugins.setErrorHandler {}
    }

    private fun init() {
        Bugsnag.init(this, BuildConfig.BUGSNAG_API_KEY)
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            Timber.plant(Timber.DebugTree())
        }
    }

    fun inject() {
        appComponent = AppInjector.inject(this)
    }

    override fun androidInjector() = dispatchingAndroidInjector

    override fun getWorkManagerConfiguration() = workConfiguration

    override fun getCameraXConfig() = Camera2Config.defaultConfig()

    var onlining = AtomicBoolean(false)

    fun gotoTimeWrong(serverTime: Long) {
        if (onlining.compareAndSet(true, false)) {
            val ise = IllegalStateException("Time error: Server-Time $serverTime - Local-Time ${System.currentTimeMillis()}")
            FirebaseCrashlytics.getInstance().recordException(ise)
            BlazeMessageService.stopService(this)
            if (callState.isGroupCall()) {
                disconnect<GroupCallService>(this)
            } else if (callState.isVoiceCall()) {
                disconnect<VoiceCallService>(this)
            }
            notificationManager.cancelAll()
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_WRONG_TIME, true)
            InitializeActivity.showWongTimeTop(this)
        }
    }

    fun closeAndClear() {
        if (onlining.compareAndSet(true, false)) {
            val accountId = Session.getAccountId()
            val sessionId = Session.getSessionId()
            BlazeMessageService.stopService(this)
            if (callState.isGroupCall()) {
                disconnect<GroupCallService>(this)
            } else if (callState.isVoiceCall()) {
                disconnect<VoiceCallService>(this)
            }
            notificationManager.cancelAll()
            Session.clearAccount()
            defaultSharedPreferences.clear()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            doAsync {
                clearData(sessionId)

                uiThread {
                    inject()
                    LandingActivity.show(this@MixinApplication)
                }
            }
        }
    }

    private fun clearData(sessionId: String?) {
        jobManager.cancelAllJob()
        jobManager.clear()
        MixinDatabase.getDatabase(this).participantSessionDao().clearKey(sessionId)
        SignalDatabase.getDatabase(this).clearAllTables()
    }
}

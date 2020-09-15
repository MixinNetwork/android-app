package one.mixin.android

import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.work.Configuration
import com.facebook.stetho.Stetho
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import one.mixin.android.crypto.MixinSignalProtocolLogger
import one.mixin.android.crypto.PrivacyPreference.clearPrivacyPreferences
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.AppComponent
import one.mixin.android.di.AppInjector
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.Session
import one.mixin.android.util.language.Lingver
import one.mixin.android.util.reportException
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
        Lingver.init(this)
        appComponent = AppInjector.init(this)
        RxJavaPlugins.setErrorHandler {}
        AppCenter.start(
            this,
            BuildConfig.APPCENTER_API_KEY,
            Analytics::class.java,
            Crashes::class.java
        )
    }

    private fun init() {

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
            reportException(ise)
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

    fun gotoOldVersionAlert() {
        if (onlining.compareAndSet(true, false)) {
            BlazeMessageService.stopService(this)
            if (callState.isGroupCall()) {
                disconnect<GroupCallService>(this)
            } else if (callState.isVoiceCall()) {
                disconnect<VoiceCallService>(this)
            }
            notificationManager.cancelAll()
            InitializeActivity.showOldVersionAlert(this)
        }
    }

    fun closeAndClear() {
        if (onlining.compareAndSet(true, false)) {
            val sessionId = Session.getSessionId()
            BlazeMessageService.stopService(this)
            if (callState.isGroupCall()) {
                disconnect<GroupCallService>(this)
            } else if (callState.isVoiceCall()) {
                disconnect<VoiceCallService>(this)
            }
            notificationManager.cancelAll()
            Session.clearAccount()
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
        clearPrivacyPreferences(this)
        MixinDatabase.getDatabase(this).participantSessionDao().clearKey(sessionId)
        SignalDatabase.getDatabase(this).clearAllTables()
    }
}

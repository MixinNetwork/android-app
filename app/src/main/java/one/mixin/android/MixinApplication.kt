package one.mixin.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ApplicationComponent
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.crypto.MixinSignalProtocolLogger
import one.mixin.android.crypto.PrivacyPreference.clearPrivacyPreferences
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.web.FloatingWebClip
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.ui.web.refresh
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

@HiltAndroidApp
class MixinApplication :
    Application(),
    Application.ActivityLifecycleCallbacks,
    Configuration.Provider,
    CameraXConfig.Provider {

    @Inject
    @JvmField
    var workerFactory: HiltWorkerFactory? = null

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var callState: CallStateLiveData

    @InstallIn(ApplicationComponent::class)
    @EntryPoint
    interface AppEntryPoint {
        fun inject(app: MixinApplication)
    }

    companion object {
        lateinit var appContext: Context

        @JvmField
        var conversationId: String? = null

        fun get(): MixinApplication = appContext as MixinApplication
    }

    override fun onCreate() {
        super.onCreate()
        init()
        registerActivityLifecycleCallbacks(this)
        SignalProtocolLoggerProvider.setProvider(MixinSignalProtocolLogger())
        appContext = applicationContext
        Lingver.init(this)
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
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory!!)
            .build()
    }

    override fun getCameraXConfig() = Camera2Config.defaultConfig()

    var onlining = AtomicBoolean(false)

    fun gotoTimeWrong(serverTime: Long) {
        if (onlining.compareAndSet(true, false)) {
            val ise =
                IllegalStateException("Time error: Server-Time $serverTime - Local-Time ${System.currentTimeMillis()}")
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
                    val entryPoint = EntryPointAccessors.fromApplication(
                        this@MixinApplication,
                        AppEntryPoint::class.java
                    )
                    entryPoint.inject(this@MixinApplication)
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

    private var activityInForeground = true
    var currentActivity: Activity? = null
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        activityInForeground = true
        if (activity !is WebActivity) {
            currentActivity = activity
            GlobalScope.launch(Dispatchers.Main) {
                refresh(activity)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        activityInForeground = false
        GlobalScope.launch {
            delay(200)
            if (!activityInForeground) {
                FloatingWebClip.getInstance(activity.isNightMode()).hide()
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity == currentActivity) currentActivity = null
    }
}

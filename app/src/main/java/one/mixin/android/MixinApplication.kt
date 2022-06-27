package one.mixin.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.MutableContextWrapper
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
import dagger.hilt.components.SingletonComponent
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.crypto.MixinSignalProtocolLogger
import one.mixin.android.crypto.PrivacyPreference.clearPrivacyPreferences
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.notificationManager
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.PipVideoView
import one.mixin.android.ui.auth.AppAuthActivity
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.ui.player.FloatingPlayer
import one.mixin.android.ui.player.MusicActivity
import one.mixin.android.ui.player.isMusicServiceRunning
import one.mixin.android.ui.web.FloatingWebClip
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.ui.web.refresh
import one.mixin.android.ui.web.releaseAll
import one.mixin.android.util.MemoryCallback
import one.mixin.android.util.SINGLE_DRAFT_THREAD
import one.mixin.android.util.debug.FileLogTree
import one.mixin.android.util.initNativeLibs
import one.mixin.android.util.language.Lingver
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.webrtc.GroupCallService
import one.mixin.android.webrtc.VoiceCallService
import one.mixin.android.webrtc.disconnect
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

open class MixinApplication :
    Application(),
    Application.ActivityLifecycleCallbacks,
    Configuration.Provider,
    CameraXConfig.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MixinJobManagerEntryPoint {
        fun getMixinJobManager(): MixinJobManager
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CallStateLiveDataEntryPoint {
        fun getCallStateLiveData(): CallStateLiveData
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun getHiltWorkerFactory(): HiltWorkerFactory
    }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppEntryPoint {
        fun inject(app: MixinApplication)
    }

    private fun getWorkerFactory() = EntryPointAccessors.fromApplication(this, HiltWorkerFactoryEntryPoint::class.java).getHiltWorkerFactory()

    private fun getJobManager() = EntryPointAccessors.fromApplication(this, MixinJobManagerEntryPoint::class.java).getMixinJobManager()

    private fun getCallState() = EntryPointAccessors.fromApplication(this, CallStateLiveDataEntryPoint::class.java).getCallStateLiveData()

    companion object {
        lateinit var appContext: Context

        @JvmField
        var conversationId: String? = null

        fun get(): MixinApplication = appContext as MixinApplication

        val appScope: CoroutineScope
            get() {
                return get().appScope
            }
    }

    private var activityReferences: Int = 0
    private var isActivityChangingConfigurations = false

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

        initNativeLibs(applicationContext)

        registerComponentCallbacks(MemoryCallback())
    }

    private fun init() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(FileLogTree())
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(getWorkerFactory())
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
            val callState = getCallState()
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
            val callState = getCallState()
            if (callState.isGroupCall()) {
                disconnect<GroupCallService>(this)
            } else if (callState.isVoiceCall()) {
                disconnect<VoiceCallService>(this)
            }
            notificationManager.cancelAll()
            InitializeActivity.showOldVersionAlert(this)
        }
    }

    fun closeAndClear(force: Boolean = false) {
        if (force || onlining.compareAndSet(true, false)) {
            val sessionId = Session.getSessionId()
            BlazeMessageService.stopService(this)
            val callState = getCallState()
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
            releaseAll()
            PipVideoView.release()
            get().appScope.launch {
                clearData(sessionId)

                withContext(Dispatchers.Main) {
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
        val jobManager = getJobManager()
        jobManager.cancelAllJob()
        jobManager.clear()
        clearPrivacyPreferences(this)
        MixinDatabase.getDatabase(this).participantSessionDao().clearKey(sessionId)
        SignalDatabase.getDatabase(this).clearAllTables()
    }

    var activityInForeground = true
    var currentActivity: Activity? = null
    val contextWrapper by lazy {
        MutableContextWrapper(this@MixinApplication)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is AppAuthActivity) {
            activityReferences += 1
        } else {
            // Workaround when other Activities in this task were finished
            // async and the AppAuthActivity became the task's root.
            if (activity.isTaskRoot) {
                activity.finish()
                return
            }
            appAuthShown = true
        }
        if (activityReferences == 1 && activity !is AppAuthActivity && !isActivityChangingConfigurations) {
            checkAndShowAppAuth(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        contextWrapper.baseContext = activity
        activityInForeground = true
        if (activity is MediaPagerActivity || activity is CallActivity || activity is MusicActivity || activity is WebActivity || activity is AppAuthActivity) {
            FloatingWebClip.getInstance(activity.isNightMode()).hide()
            FloatingPlayer.getInstance(activity.isNightMode()).hide()
        } else if (activity !is LandingActivity && activity !is InitializeActivity) {
            if (activity !is WebActivity) {
                currentActivity = activity
                appScope.launch(Dispatchers.Main) {
                    refresh()
                }
            }
            if (activity !is MusicActivity) {
                currentActivity = activity
                appScope.launch(Dispatchers.Main) {
                    if (isMusicServiceRunning(activity)) {
                        FloatingPlayer.getInstance(activity.isNightMode()).show()
                    }
                }
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        activityInForeground = false
        get().appScope.launch {
            delay(200)
            if (!activityInForeground) {
                FloatingWebClip.getInstance(activity.isNightMode()).hide()
                FloatingPlayer.getInstance(activity.isNightMode()).hide()
            }
        }
    }

    private var appAuthShown = false

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (activity !is AppAuthActivity) {
            activityReferences -= 1
        } else {
            appAuthShown = false
        }
        if (!appAuthShown && activity !is AppAuthActivity && activityReferences == 0 && !isActivityChangingConfigurations) {
            defaultSharedPreferences.putLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, System.currentTimeMillis())
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity == currentActivity) currentActivity = null
    }

    private val appScope by lazy {
        object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = EmptyCoroutineContext
        }
    }

    fun saveDraft(conversationId: String, draft: String) =
        appScope.launch(SINGLE_DRAFT_THREAD) {
            MixinDatabase.getDatabase(this@MixinApplication).conversationDao()
                .saveDraft(conversationId, draft)
        }

    fun checkAndShowAppAuth(activity: Activity): Boolean {
        val appAuth = defaultSharedPreferences.getInt(Constants.Account.PREF_APP_AUTH, -1)
        if (appAuth != -1) {
            if (appAuth == 0) {
                AppAuthActivity.show(activity)
                return true
            } else {
                val enterBackground =
                    defaultSharedPreferences.getLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, 0)
                val now = System.currentTimeMillis()
                val offset = if (appAuth == 1) {
                    Constants.INTERVAL_1_MIN
                } else {
                    Constants.INTERVAL_30_MINS
                }
                if (now - enterBackground > offset) {
                    AppAuthActivity.show(activity)
                    return true
                }
            }
        }
        return false
    }
}

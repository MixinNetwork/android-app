package one.mixin.android

import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.work.Configuration
import com.bugsnag.android.Bugsnag
import com.crashlytics.android.Crashlytics
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.soloader.SoLoader
import com.facebook.stetho.Stetho
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
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
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.Session
import one.mixin.android.webrtc.CallService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.uiThread
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider
import timber.log.Timber

class MixinApplication : Application(), HasAndroidInjector, Configuration.Provider {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var workConfiguration: Configuration

    @Inject
    lateinit var jobManager: MixinJobManager

    lateinit var appComponent: AppComponent

    var networkFlipperPlugin: NetworkFlipperPlugin? = null

    companion object {
        lateinit var appContext: Context
        @JvmField
        var conversationId: String? = null

        fun get(): MixinApplication = appContext as MixinApplication
    }

    override fun onCreate() {
        super.onCreate()
        init()
        FirebaseApp.initializeApp(this)
        SignalProtocolLoggerProvider.setProvider(MixinSignalProtocolLogger())
        appContext = applicationContext
        AndroidThreeTen.init(this)
        appComponent = AppInjector.init(this)
        RxJavaPlugins.setErrorHandler {}
    }

    private fun init() {
        Bugsnag.init(this, BuildConfig.BUGSNAG_API_KEY)
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            SoLoader.init(this, false)
            if (FlipperUtils.shouldEnableFlipper(this)) {
                val client = AndroidFlipperClient.getInstance(this)
                client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
                client.addPlugin(DatabasesFlipperPlugin(this))
                client.addPlugin(CrashReporterPlugin.getInstance())
                networkFlipperPlugin = NetworkFlipperPlugin()
                client.addPlugin(networkFlipperPlugin)
                client.start()
            }
            Timber.plant(Timber.DebugTree())
        }
    }

    fun inject() {
        appComponent = AppInjector.inject(this)
    }

    override fun androidInjector() = dispatchingAndroidInjector

    override fun getWorkManagerConfiguration() = workConfiguration

    var onlining = AtomicBoolean(false)

    fun gotoTimeWrong(serverTime: Long) {
        if (onlining.compareAndSet(true, false)) {
            val ise = IllegalStateException("Time error: Server-Time $serverTime - Local-Time ${System.currentTimeMillis()}")
            Crashlytics.logException(ise)
            BlazeMessageService.stopService(this)
            CallService.disconnect(this)
            notificationManager.cancelAll()
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_WRONG_TIME, true)
            InitializeActivity.showWongTimeTop(this)
        }
    }

    fun closeAndClear(toLanding: Boolean = true) {
        if (onlining.compareAndSet(true, false)) {
            BlazeMessageService.stopService(this)
            CallService.disconnect(this)
            notificationManager.cancelAll()
            Session.clearAccount()
            defaultSharedPreferences.clear()
            defaultSharedPreferences.putBoolean(Constants.Account.PREF_LOGOUT_COMPLETE, false)
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            if (toLanding) {
                doAsync {
                    clearData()

                    uiThread {
                        inject()
                        LandingActivity.show(this@MixinApplication)
                    }
                }
            } else {
                clearData()
                inject()
            }
        }
    }

    fun clearData() {
        jobManager.cancelAllJob()
        jobManager.clear()
        SignalDatabase.getDatabase(this).clearAllTables()
        MixinDatabase.getDatabase(this).clearAllTables()
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_LOGOUT_COMPLETE, true)
    }
}

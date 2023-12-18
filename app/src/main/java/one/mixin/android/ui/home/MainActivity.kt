package one.mixin.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.room.util.readVersion
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.AppCenter
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_BACKUP
import one.mixin.android.Constants.Account.PREF_BATTERY_OPTIMIZE
import one.mixin.android.Constants.Account.PREF_CHECK_STORAGE
import one.mixin.android.Constants.Account.PREF_DEVICE_SDK
import one.mixin.android.Constants.Account.PREF_LOGIN_VERIFY
import one.mixin.android.Constants.Account.PREF_SYNC_CIRCLE
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.Constants.CIRCLE.CIRCLE_NAME
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.Constants.DataBase.CURRENT_VERSION
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.Constants.DataBase.MINI_VERSION
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.INTERVAL_7_DAYS
import one.mixin.android.Constants.SAFETY_NET_INTERVAL_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.databinding.ActivityMainBinding
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.event.SessionEvent
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.areBubblesAllowedCompat
import one.mixin.android.extension.checkStorageNotLow
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.toast
import one.mixin.android.job.AttachmentMigrationJob
import one.mixin.android.job.BackupJob
import one.mixin.android.job.CleanCacheJob
import one.mixin.android.job.CleanupQuoteContentJob
import one.mixin.android.job.CleanupThumbJob
import one.mixin.android.job.MigratedFts4Job
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshCircleJob
import one.mixin.android.job.RefreshContactJob
import one.mixin.android.job.RefreshExternalSchemeJob
import one.mixin.android.job.RefreshFcmJob
import one.mixin.android.job.RefreshFiatsJob
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.job.RefreshStickerAlbumJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.RestoreTransactionJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.job.TranscriptAttachmentMigrationJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.wc.WCErrorEvent
import one.mixin.android.tip.wc.WCEvent
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.BatteryOptimizationDialogActivity
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.common.EditDialog
import one.mixin.android.ui.common.LoginVerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.NavigationController
import one.mixin.android.ui.common.PinCodeFragment.Companion.FROM_EMERGENCY
import one.mixin.android.ui.common.PinCodeFragment.Companion.FROM_LOGIN
import one.mixin.android.ui.common.PinCodeFragment.Companion.PREF_LOGIN_FROM
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.biometric.buildEmptyTransferBiometricItem
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.home.circle.CirclesFragment
import one.mixin.android.ui.home.circle.ConversationCircleEditFragment
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.landing.RestoreActivity
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_SHOW_SCAN
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.ui.search.SearchSingleFragment
import one.mixin.android.ui.tip.CheckRegisterBottomSheetDialogFragment
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipBundle
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.tip.TryConnecting
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.errorHandler
import one.mixin.android.util.RomUtil
import one.mixin.android.util.RootUtil
import one.mixin.android.util.reportException
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.widget.MaterialSearchView
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BlazeBaseActivity() {
    lateinit var navigationController: NavigationController

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var conversationService: ConversationService

    @Inject
    lateinit var userService: UserService

    @Inject
    lateinit var conversationDao: ConversationDao

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var userRepo: UserRepository

    @Inject
    lateinit var accountRepo: AccountRepository

    @Inject
    lateinit var participantDao: ParticipantDao

    @Inject
    lateinit var tip: Tip

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val updatedListener =
        InstallStateUpdatedListener { state ->
            if (isFinishing) return@InstallStateUpdatedListener
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
        }

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_NoActionBar
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_NoActionBar
    }

    private lateinit var binding: ActivityMainBinding

    private var isDesktopLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationController = NavigationController(this)

        var deviceId = defaultSharedPreferences.getString(DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = this.getStringDeviceId()
            defaultSharedPreferences.putString(DEVICE_ID, deviceId)
        } else if (deviceId != this.getStringDeviceId()) {
            defaultSharedPreferences.remove(DEVICE_ID)
            MixinApplication.get().closeAndClear(true)
            finish()
            return
        }

        if (!Session.checkToken()) {
            run {
                startActivity(Intent(this, LandingActivity::class.java))
                finish()
                return
            }
        }

        if (Session.getAccount()?.fullName.isNullOrBlank()) {
            InitializeActivity.showSetupName(this)
            finish()
            return
        }

        if (defaultSharedPreferences.getBoolean(Constants.Account.PREF_RESTORE, false)) {
            RestoreActivity.show(this)
            finish()
            return
        }

        if (defaultSharedPreferences.getBoolean(Constants.Account.PREF_WRONG_TIME, false)) {
            InitializeActivity.showWongTime(this)
            finish()
            return
        }

        MixinApplication.get().isOnline.set(true)
        if (checkNeedGo2MigrationPage()) {
            InitializeActivity.showDBUpgrade(this)
            finish()
            return
        }

        if (!getIsLoaded(this, false) ||
            !getIsSyncSession(this, false)
        ) {
            InitializeActivity.showLoading(this, false)
            finish()
            return
        }

        if (Session.shouldUpdateKey()) {
            InitializeActivity.showLoading(this, false)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            navigationController.navigateToMessage()
        }

        val account = Session.getAccount()
        account?.let {
            FirebaseCrashlytics.getInstance().setUserId(it.userId)
            AppCenter.setUserId(it.userId)
        }

        initView()
        handlerCode(intent)

        checkAsync()

        RxBus.listen(TipEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe { e ->
                handleTipEvent(e, deviceId)
            }
        RxBus.listen(WCEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe { e ->
                WalletConnectActivity.show(this, e)
            }
        RxBus.listen(WCErrorEvent::class.java)
            .autoDispose(destroyScope)
            .subscribe {
                if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    WalletConnectActivity.show(this, it.error)
                }
            }
        RxBus.listen(SessionEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe {
                isDesktopLogin = Session.getExtensionSessionId() != null
                binding.searchBar.updateDesktop(isDesktopLogin)
            }

        if (Session.getAccount()?.hasPin != true) {
            TipActivity.show(this, TipType.Create, shouldWatch = true)
        } else if (Session.getTipPub().isNullOrBlank()) {
            TipActivity.show(this, TipType.Upgrade, shouldWatch = true)
        } else {
            if (Session.hasSafe()) {
                jobManager.addJobInBackground(RefreshAccountJob(checkTip = true))
                if (defaultSharedPreferences.getBoolean(PREF_LOGIN_VERIFY, false)) {
                    LoginVerifyBottomSheetDialogFragment.newInstance().apply {
                        onDismissCallback = { success ->
                            if (success) {
                                defaultSharedPreferences.putBoolean(PREF_LOGIN_VERIFY, false)
                            } else {
                                this@MainActivity.finish()
                            }
                        }
                    }.showNow(supportFragmentManager, LoginVerifyBottomSheetDialogFragment.TAG)
                }
            } else {
                CheckRegisterBottomSheetDialogFragment.newInstance()
                    .showNow(supportFragmentManager, CheckRegisterBottomSheetDialogFragment.TAG)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            delay(10_000)
            if (MixinApplication.get().isAppAuthShown()) {
                return@launch
            }
            checkUpdate()
        }

        jobManager.addJobInBackground(SyncOutputJob())
        jobManager.addJobInBackground(RestoreTransactionJob())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RxPermissions(this)
                .request(Manifest.permission.POST_NOTIFICATIONS)
                .autoDispose(stopScope)
                .subscribe(
                    { _ -> },
                    {},
                )
        }
    }

    override fun onStart() {
        super.onStart()
        val notificationManager = getSystemService<NotificationManager>() ?: return
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && notificationManager.areBubblesAllowedCompat()).not()) {
            notificationManager.cancelAll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(updatedListener)
    }

    private fun checkAsync() =
        lifecycleScope.launch(Dispatchers.IO) {
            checkRoot()
            checkStorage()
            refreshStickerAlbum()
            refreshExternalSchemes()
            cleanCache()
            jobManager.addJobInBackground(RefreshAssetsJob())
            sendSafetyNetRequest()
            checkBatteryOptimization()

            if (!defaultSharedPreferences.getBoolean(PREF_SYNC_CIRCLE, false)) {
                jobManager.addJobInBackground(RefreshCircleJob())
                defaultSharedPreferences.putBoolean(PREF_SYNC_CIRCLE, true)
            }

            jobManager.addJobInBackground(RefreshOneTimePreKeysJob())
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && PropertyHelper.findValueByKey(PREF_BACKUP, false)) {
                jobManager.addJobInBackground(BackupJob())
            }

            if (defaultSharedPreferences.getInt(PREF_LOGIN_FROM, FROM_LOGIN) == FROM_EMERGENCY) {
                defaultSharedPreferences.putInt(PREF_LOGIN_FROM, FROM_LOGIN)
                delayShowModifyMobile()
            }

            if (Fiats.isRateEmpty()) {
                jobManager.addJobInBackground(RefreshFiatsJob())
            }

            val sdk = PropertyHelper.findValueByKey(PREF_DEVICE_SDK, -1)
            if (sdk == -1) {
                PropertyHelper.updateKeyValue(PREF_DEVICE_SDK, Build.VERSION.SDK_INT)
            } else if (sdk < Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PropertyHelper.migration()
            }

            PropertyHelper.checkAttachmentMigrated {
                jobManager.addJobInBackground(AttachmentMigrationJob())
            }

            PropertyHelper.checkTranscriptAttachmentMigrated {
                jobManager.addJobInBackground(TranscriptAttachmentMigrationJob())
            }

            PropertyHelper.checkTranscriptAttachmentMigrated {
                jobManager.addJobInBackground(TranscriptAttachmentMigrationJob())
            }

            PropertyHelper.checkBackupMigrated {
                jobManager.addJobInBackground(BackupJob(force = true, delete = true))
            }
            PropertyHelper.checkFtsMigrated {
                jobManager.addJobInBackground(MigratedFts4Job())
            }

            PropertyHelper.checkCleanupThumb {
                jobManager.addJobInBackground(CleanupThumbJob())
            }

            PropertyHelper.checkCleanupQuoteContent {
                jobManager.addJobInBackground(CleanupQuoteContentJob(-1L))
            }

            jobManager.addJobInBackground(RefreshContactJob())
            jobManager.addJobInBackground(RefreshFcmJob())

            initWalletConnect()
        }

    private fun handleTipEvent(
        e: TipEvent,
        deviceId: String,
    ) {
        val nodeCounter = e.nodeCounter
        if (nodeCounter == 1) {
            val tipType = if (Session.getAccount()?.hasPin == true) TipType.Upgrade else TipType.Create
            TipActivity.show(this, TipBundle(tipType, deviceId, TryConnecting, tipEvent = e))
        } else if (nodeCounter > 1) {
            TipActivity.show(this, TipBundle(TipType.Change, deviceId, TryConnecting, tipEvent = e))
        } else {
            reportException(IllegalStateException("Receive TipEvent nodeCounter < 1"))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun checkNeedGo2MigrationPage(): Boolean {
        val currentVersion =
            try {
                readVersion(getDatabasePath(DB_NAME))
            } catch (e: Exception) {
                0
            }
        return currentVersion > MINI_VERSION && CURRENT_VERSION != currentVersion
    }

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        val batteryOptimize = defaultSharedPreferences.getLong(PREF_BATTERY_OPTIMIZE, 0)
        val cur = System.currentTimeMillis()
        if (cur - batteryOptimize > INTERVAL_24_HOURS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !RomUtil.isEmui) {
                getSystemService<ActivityManager>()?.let { am ->
                    if (am.isBackgroundRestricted) {
                        BatteryOptimizationDialogActivity.show(this)
                    }
                }
            } else {
                getSystemService<PowerManager>()?.let { pm ->
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        BatteryOptimizationDialogActivity.show(this)
                    }
                }
            }
            defaultSharedPreferences.putLong(PREF_BATTERY_OPTIMIZE, cur)
        }
    }

    private fun delayShowModifyMobile() =
        lifecycleScope.launch {
            delay(2000)
            MaterialAlertDialogBuilder(this@MainActivity, R.style.MixinAlertDialogTheme)
                .setTitle(getString(R.string.setting_emergency_change_mobile))
                .setPositiveButton(R.string.Change) { dialog, _ ->
                    supportFragmentManager.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                        )
                            .add(R.id.root_view, VerifyFragment.newInstance(VerifyFragment.FROM_PHONE))
                            .addToBackStack(null)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.Later) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

    private fun checkRoot() {
        if (RootUtil.isDeviceRooted &&
            defaultSharedPreferences.getBoolean(
                Constants.Account.PREF_BIOMETRICS,
                false,
            )
        ) {
            BiometricUtil.deleteKey(this)
        }
    }

    private fun refreshStickerAlbum() =
        runIntervalTask(RefreshStickerAlbumJob.PREF_REFRESH_STICKER_ALBUM, INTERVAL_24_HOURS) {
            jobManager.addJobInBackground(RefreshStickerAlbumJob())
        }

    private fun refreshExternalSchemes() =
        runIntervalTask(RefreshExternalSchemeJob.PREF_REFRESH_EXTERNAL_SCHEMES, INTERVAL_24_HOURS) {
            jobManager.addJobInBackground(RefreshExternalSchemeJob())
        }

    private fun cleanCache() =
        runIntervalTask(CleanCacheJob.PREF_CLEAN_CACHE_SCHEMES, INTERVAL_7_DAYS) {
            jobManager.addJobInBackground(CleanCacheJob())
        }

    private fun sendSafetyNetRequest() {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext, 13000000) != ConnectionResult.SUCCESS) {
            return
        }
        runIntervalTask(SAFETY_NET_INTERVAL_KEY, INTERVAL_24_HOURS) {
            accountRepo.deviceCheck().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe(
                    { resp ->
                        resp.data?.let {
                            val nonce = Base64.decode(it.nonce)
                            validateSafetyNet(nonce)
                        }
                    },
                    {
                    },
                )
        }
    }

    private fun validateSafetyNet(nonce: ByteArray) {
        val client = SafetyNet.getClient(this)
        val task = client.attest(nonce, BuildConfig.SAFETYNET_API_KEY)
        task.addOnSuccessListener { safetyResp ->
            accountRepo.updateSession(SessionRequest(deviceCheckToken = safetyResp.jwsResult))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe({}, {})
        }
        task.addOnFailureListener { e ->
            reportException(e)
        }
    }

    private fun checkUpdate() {
        appUpdateManager.registerListener(updatedListener)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                (appUpdateInfo.clientVersionStalenessDays() ?: -1) >= 3 &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                        0x01,
                    )
                } catch (ignored: IntentSender.SendIntentException) {
                }
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            } else if (appUpdateInfo.installStatus() == InstallStatus.INSTALLED) {
                appUpdateManager.unregisterListener(updatedListener)
            }
        }
    }

    private fun checkStorage() {
        val lastTime = defaultSharedPreferences.getLong(PREF_CHECK_STORAGE, 0)
        if (System.currentTimeMillis() - lastTime > INTERVAL_24_HOURS) {
            defaultSharedPreferences.putLong(PREF_CHECK_STORAGE, System.currentTimeMillis())
            checkStorageNotLow(
                {
                    lifecycleScope.launch {
                        alertDialogBuilder()
                            .setTitle(R.string.storage_low_title)
                            .setMessage(R.string.storage_low_message)
                            .setCancelable(false)
                            .setNegativeButton(getString(R.string.I_know)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                },
                {
                },
            )
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        if (isFinishing) return
        Snackbar.make(
            binding.rootView,
            getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE,
        ).apply {
            setAction(getString(R.string.RESTART)) { appUpdateManager.completeUpdate() }
            setActionTextColor(getColor(R.color.colorAccent))
            show()
        }
    }

    private fun initWalletConnect() {
        if (!WalletConnect.isEnabled(this)) return

        WalletConnectV2
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlerCode(intent)
    }

    override fun recreate() {
        super.recreate()
        intent.replaceExtras(Bundle())
    }

    fun showCapture(scan: Boolean) {
        getScanResult.launch(Pair(ARGS_SHOW_SCAN, scan))
    }

    private val getScanResult =
        registerForActivityResult(CaptureActivity.CaptureContract()) { data ->
            if (data != null) {
                handlerCode(data)
            }
        }

    private var bottomSheet: DialogFragment? = null
    private var alertDialog: Dialog? = null

    private fun handlerCode(intent: Intent) {
        if (intent.hasExtra(SCAN)) {
            val scan = intent.getStringExtra(SCAN)!!
            bottomSheet?.dismiss()
            showScanBottom(scan)
            clearCodeAfterConsume(intent, SCAN)
        } else if (intent.hasExtra(URL) || (intent.action == Intent.ACTION_VIEW && intent.categories.contains(Intent.CATEGORY_BROWSABLE))) {
            val url = intent.getStringExtra(URL) ?: intent.data?.toString() ?: return
            bottomSheet?.dismiss()
            bottomSheet = LinkBottomSheetDialogFragment.newInstance(url, LinkBottomSheetDialogFragment.FROM_SCAN)
            bottomSheet?.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
            clearCodeAfterConsume(intent, URL)
        } else if (intent.hasExtra(WALLET)) {
            navigationController.pushWallet()
            clearCodeAfterConsume(intent, WALLET)
        } else if (intent.hasExtra(TRANSFER)) {
            val userId = intent.getStringExtra(TRANSFER) ?: return
            if (Session.getAccount()?.hasPin == true) {
                lifecycleScope.launch {
                    val user = userRepo.refreshUser(userId) ?: return@launch
                    TransferFragment.newInstance(buildEmptyTransferBiometricItem(user))
                        .showNow(supportFragmentManager, TransferFragment.TAG)
                }
            } else {
                toast(R.string.transfer_without_pin)
            }
            clearCodeAfterConsume(intent, TRANSFER)
        } else if (intent.extras != null && intent.extras!!.getString("conversation_id", null) != null) {
            showDialog()
            val conversationId = intent.extras!!.getString("conversation_id")!!
            clearCodeAfterConsume(intent, "conversation_id")
            Maybe.just(conversationId).map {
                val innerIntent: Intent?
                var conversation = conversationDao.findConversationById(conversationId)
                if (conversation == null) {
                    val response =
                        conversationService.getConversation(conversationId).execute().body()
                    if (response != null && response.isSuccess) {
                        response.data?.let { data ->
                            var ownerId: String = data.creatorId
                            if (data.category == ConversationCategory.CONTACT.name) {
                                ownerId =
                                    data.participants.find { p -> p.userId != Session.getAccountId() }!!.userId
                            } else if (data.category == ConversationCategory.GROUP.name) {
                                ownerId = data.creatorId
                            }
                            var c = conversationDao.findConversationById(data.conversationId)
                            if (c == null) {
                                c =
                                    Conversation(
                                        data.conversationId,
                                        ownerId,
                                        data.category,
                                        data.name,
                                        data.iconUrl,
                                        data.announcement,
                                        data.codeUrl,
                                        "",
                                        data.createdAt,
                                        null,
                                        null,
                                        null,
                                        0,
                                        ConversationStatus.SUCCESS.ordinal,
                                        null,
                                    )
                                conversation = c
                                conversationDao.upsert(c)
                            } else {
                                conversationDao.updateConversation(
                                    data.conversationId,
                                    ownerId,
                                    data.category,
                                    data.name,
                                    data.announcement,
                                    data.muteUntil,
                                    data.createdAt,
                                    data.expireIn,
                                    ConversationStatus.SUCCESS.ordinal,
                                )
                            }

                            val participants = mutableListOf<Participant>()
                            val userIdList = mutableListOf<String>()
                            for (p in data.participants) {
                                val item =
                                    Participant(conversationId, p.userId, p.role, p.createdAt!!)
                                if (p.role == ParticipantRole.OWNER.name) {
                                    participants.add(0, item)
                                } else {
                                    participants.add(item)
                                }

                                val u = userDao.findUser(p.userId)
                                if (u == null) {
                                    userIdList.add(p.userId)
                                }
                            }
                            if (userIdList.isNotEmpty()) {
                                jobManager.addJobInBackground(RefreshUserJob(userIdList))
                            }
                            participantDao.insertList(participants)
                        }
                    }
                }
                if (conversation?.isGroupConversation() == true) {
                    innerIntent = ConversationActivity.putIntent(this, conversationId)
                } else {
                    var user = userDao.findOwnerByConversationId(conversationId)
                    if (user == null) {
                        val response =
                            userService.getUserById(conversation!!.ownerId!!).execute()
                                .body()
                        if (response != null && response.isSuccess) {
                            response.data?.let { u ->
                                runBlocking { userRepo.upsert(u) }
                                user = u
                            }
                        }
                    }
                    innerIntent = ConversationActivity.putIntent(this, conversationId, user?.userId)
                }
                runOnUiThread { dismissDialog() }
                innerIntent
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope).subscribe(
                    {
                        it?.let { intent ->
                            this.startActivity(intent)
                        }
                    },
                    {
                        dismissDialog()
                        ErrorHandler.handleError(it)
                    },
                )
        } else if (intent.hasExtra(WALLET_CONNECT)) {
            val wcUrl = requireNotNull(intent.getStringExtra(WALLET_CONNECT))
            WalletConnect.connect(wcUrl)
        }
    }

    private fun showDialog() {
        alertDialog?.dismiss()
        alertDialog =
            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                show()
            }
    }

    private fun dismissDialog() {
        alertDialog?.dismiss()
        alertDialog = null
    }

    private fun clearCodeAfterConsume(
        intent: Intent,
        code: String,
    ) {
        intent.removeExtra(code)
    }

    private fun showScanBottom(scan: String) {
        bottomSheet = QrScanBottomSheetDialogFragment.newInstance(scan)
        bottomSheet?.showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
    }

    private fun initView() {
        binding.searchBar.setOnLeftClickListener {
            openSearch()
        }
        binding.searchBar.setOnGroupClickListener {
            navigationController.pushContacts()
        }
        binding.searchBar.setOnAddClickListener {
            addCircle()
        }
        binding.searchBar.setOnConfirmClickListener {
            val circlesFragment =
                supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as CirclesFragment
            circlesFragment.cancelSort()
            binding.searchBar.actionVa.showPrevious()
        }
        binding.searchBar.setOnBackClickListener {
            binding.searchBar.closeSearch()
        }
        binding.searchBar.mOnQueryTextListener =
            object : MaterialSearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    (supportFragmentManager.findFragmentByTag(SearchFragment.TAG) as? SearchFragment)?.setQueryText(
                        newText,
                    )
                    return true
                }
            }

        binding.searchBar.setSearchViewListener(
            object : MaterialSearchView.SearchViewListener {
                override fun onSearchViewClosed() {
                    navigationController.hideSearch()
                }

                override fun onSearchViewOpened() {
                    navigationController.showSearch()
                }
            },
        )
        binding.searchBar.hideAction = {
            (supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as? CirclesFragment)?.cancelSort()
        }
        binding.searchBar.logo.text = defaultSharedPreferences.getString(CIRCLE_NAME, "Mixin")
        binding.searchBar.desktop.setOnClickListener {
            DeviceFragment.newInstance().showNow(supportFragmentManager, DeviceFragment.TAG)
        }
        binding.rootView.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && binding.searchBar.isOpen) {
                binding.searchBar.closeSearch()
                true
            } else {
                false
            }
        }
        supportFragmentManager.beginTransaction().add(R.id.container_circle, circlesFragment, CirclesFragment.TAG).commit()
        observeOtherCircleUnread(defaultSharedPreferences.getString(CIRCLE_ID, null))
        isDesktopLogin = Session.getExtensionSessionId() != null
        binding.searchBar.updateDesktop(isDesktopLogin)
    }

    fun openSearch() {
        binding.searchBar.openSearch()
    }

    fun openWallet() {
        navigationController.pushWallet()
    }

    private val circlesFragment by lazy {
        CirclesFragment.newInstance()
    }

    fun closeSearch() {
        binding.searchBar.closeSearch()
    }

    fun dragSearch(progress: Float) {
        binding.searchBar.dragSearch(progress)
    }

    fun showSearchLoading() {
        binding.searchBar.showLoading()
    }

    fun hideSearchLoading() {
        binding.searchBar.hideLoading()
    }

    fun selectCircle(
        name: String?,
        circleId: String?,
    ) {
        setCircleName(name)
        defaultSharedPreferences.putString(CIRCLE_NAME, name)
        defaultSharedPreferences.putString(CIRCLE_ID, circleId)
        binding.searchBar.hideContainer()
        (supportFragmentManager.findFragmentByTag(ConversationListFragment.TAG) as? ConversationListFragment)?.circleId = circleId
        observeOtherCircleUnread(circleId)
    }

    fun setCircleName(name: String?) {
        binding.searchBar.logo.text = name ?: "Mixin"
    }

    fun openCircleEdit(circleId: String) {
        conversationDao
        lifecycleScope.launch {
            userRepo.findCircleItemByCircleIdSuspend(circleId)?.let { circleItem ->
                val circlesFragment =
                    supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as CirclesFragment?
                circlesFragment?.edit(circleItem)
            }
        }
    }

    fun sortAction() {
        binding.searchBar.actionVa.showNext()
    }

    private var dotObserver =
        Observer<Boolean> {
            binding.searchBar.dot.isVisible = it
        }
    private var dotLiveData: LiveData<Boolean>? = null

    private fun observeOtherCircleUnread(circleId: String?) =
        lifecycleScope.launch {
            dotLiveData?.removeObserver(dotObserver)
            if (circleId == null) {
                binding.searchBar.dot.isVisible = false
                return@launch
            }
            dotLiveData = userRepo.hasUnreadMessage(circleId = circleId)
            dotLiveData?.observe(this@MainActivity, dotObserver)
        }

    private fun addCircle() {
        editDialog {
            titleText = this@MainActivity.getString(R.string.Add_circle)
            maxTextCount = 64
            defaultEditEnable = false
            editMaxLines = EditDialog.MAX_LINE.toInt()
            allowEmpty = false
            rightText = android.R.string.ok
            rightAction = {
                createCircle(it)
            }
        }
    }

    private fun createCircle(name: String) {
        lifecycleScope.launch(errorHandler) {
            val dialog =
                indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    setCancelable(false)
                }
            handleMixinResponse(
                invokeNetwork = {
                    userRepo.createCircle(name)
                },
                successBlock = { response ->
                    response.data?.let { circle ->
                        userRepo.insertCircle(circle)
                        openCircleEdit(circle.circleId)
                    }
                },
                exceptionBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                },
                failureBlock = {
                    dialog.dismiss()
                    return@handleMixinResponse false
                },
            )
            dialog.dismiss()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val searchMessageFragment =
            supportFragmentManager.findFragmentByTag(SearchMessageFragment.TAG)
        val searchSingleFragment =
            supportFragmentManager.findFragmentByTag(SearchSingleFragment.TAG)
        val circlesFragment =
            supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as BaseFragment
        val conversationCircleEditFragment =
            supportFragmentManager.findFragmentByTag(ConversationCircleEditFragment.TAG)
        when {
            searchMessageFragment != null -> onBackPressedDispatcher.onBackPressed()
            searchSingleFragment != null -> onBackPressedDispatcher.onBackPressed()
            conversationCircleEditFragment != null -> onBackPressedDispatcher.onBackPressed()
            binding.searchBar.isOpen -> binding.searchBar.closeSearch()
            binding.searchBar.containerDisplay -> {
                if (!circlesFragment.onBackPressed()) {
                    binding.searchBar.hideContainer()
                } else {
                    binding.searchBar.actionVa.showPrevious()
                }
            }
            else -> {
                // https://issuetracker.google.com/issues/139738913
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && isTaskRoot) {
                    finishAfterTransition()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    companion object {
        const val URL = "url"
        const val SCAN = "scan"
        const val TRANSFER = "transfer"
        private const val WALLET = "wallet"
        const val WALLET_CONNECT = "wallet_connect"

        fun showWallet(context: Context) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(WALLET, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run {
                context.startActivity(this)
            }
        }

        fun showFromShortcut(
            activity: Activity,
            intent: Intent,
        ) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            activity.startActivity(intent)
        }

        fun show(context: Context) {
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run {
                context.startActivity(this)
            }
        }

        fun getSingleIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }

        fun getWakeUpIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                action = Intent.ACTION_MAIN
            }
        }

        fun reopen(context: Context) {
            Intent(context, MainActivity::class.java).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }.run {
                context.startActivity(this)
            }
        }
    }
}

@Suppress("SameParameterValue")
fun runIntervalTask(
    spKey: String,
    interval: Long,
    task: () -> Unit,
) {
    val defaultSharedPreferences = MixinApplication.appContext.defaultSharedPreferences
    val cur = System.currentTimeMillis()
    val last = defaultSharedPreferences.getLong(spKey, 0)
    if (cur - last > interval) {
        task.invoke()
        defaultSharedPreferences.putLong(spKey, cur)
    }
}

package one.mixin.android.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.bugsnag.android.Bugsnag
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.AppCenter
import com.uber.autodispose.autoDispose
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_search.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_ATTACHMENT
import one.mixin.android.Constants.Account.PREF_BACKUP
import one.mixin.android.Constants.Account.PREF_BATTERY_OPTIMIZE
import one.mixin.android.Constants.Account.PREF_CHECK_STORAGE
import one.mixin.android.Constants.Account.PREF_SYNC_CIRCLE
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.Constants.CIRCLE.CIRCLE_NAME
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.SAFETY_NET_INTERVAL_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.UserDao
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.alert
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.checkStorageNotLow
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.enqueueUniqueOneTimeNetworkWorkRequest
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.job.AttachmentMigrationJob
import one.mixin.android.job.BackupJob
import one.mixin.android.job.BackupMigrationJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.RefreshCircleJob
import one.mixin.android.job.RefreshFiatsJob
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.job.RefreshStickerAlbumJob
import one.mixin.android.job.RefreshStickerAlbumJob.Companion.REFRESH_STICKER_ALBUM_PRE_KEY
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.common.EditDialog
import one.mixin.android.ui.common.NavigationController
import one.mixin.android.ui.common.PinCodeFragment.Companion.FROM_EMERGENCY
import one.mixin.android.ui.common.PinCodeFragment.Companion.FROM_LOGIN
import one.mixin.android.ui.common.PinCodeFragment.Companion.PREF_LOGIN_FROM
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
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
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.errorHandler
import one.mixin.android.util.RootUtil
import one.mixin.android.util.Session
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.isGroup
import one.mixin.android.widget.MaterialSearchView
import one.mixin.android.worker.RefreshAssetsWorker
import one.mixin.android.worker.RefreshContactWorker
import one.mixin.android.worker.RefreshFcmWorker
import timber.log.Timber
import javax.inject.Inject

class MainActivity : BlazeBaseActivity() {

    @Inject
    lateinit var navigationController: NavigationController

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var conversationService: ConversationService

    @Inject
    lateinit var userService: UserService

    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var conversationDao: ConversationDao

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var userRepo: UserRepository

    @Inject
    lateinit var accountRepo: AccountRepository

    @Inject
    lateinit var participantDao: ParticipantDao

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val updatedListener = InstallStateUpdatedListener { state ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Session.checkToken()) run {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
            return
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

        MixinApplication.get().onlining.set(true)
        if (!defaultSharedPreferences.getBoolean(Constants.Account.PREF_FTS4_UPGRADE, false)) {
            InitializeActivity.showFts(this)
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

        if (defaultSharedPreferences.getInt(PREF_LOGIN_FROM, FROM_LOGIN) == FROM_EMERGENCY) {
            defaultSharedPreferences.putInt(PREF_LOGIN_FROM, FROM_LOGIN)
            delayShowModifyMobile()
        }

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            navigationController.navigateToMessage()
        }

        val account = Session.getAccount()
        Bugsnag.setUser(account?.userId, account?.identityNumber, account?.fullName)
        account?.let {
            FirebaseCrashlytics.getInstance().setUserId(it.userId)
            AppCenter.setUserId(it.userId)
        }

        if (!defaultSharedPreferences.getBoolean(PREF_SYNC_CIRCLE, false)) {
            jobManager.addJobInBackground(RefreshCircleJob())
            defaultSharedPreferences.putBoolean(PREF_SYNC_CIRCLE, true)
        }
        jobManager.addJobInBackground(RefreshOneTimePreKeysJob())
        jobManager.addJobInBackground(BackupJob())
        if (!defaultSharedPreferences.getBoolean(PREF_ATTACHMENT, false)) {
            jobManager.addJobInBackground(AttachmentMigrationJob())
        }

        if (!defaultSharedPreferences.getBoolean(PREF_BACKUP, false)) {
            jobManager.addJobInBackground(BackupMigrationJob())
        }

        lifecycleScope.launch(Dispatchers.IO) {
            jobManager.addJobInBackground(RefreshAccountJob())

            if (Fiats.isRateEmpty()) {
                jobManager.addJobInBackground(RefreshFiatsJob())
            }

            WorkManager.getInstance(this@MainActivity)
                .enqueueUniqueOneTimeNetworkWorkRequest<RefreshContactWorker>("RefreshContactWorker")
            WorkManager.getInstance(this@MainActivity)
                .enqueueUniqueOneTimeNetworkWorkRequest<RefreshFcmWorker>("RefreshFcmWorker")
            WorkManager.getInstance(this@MainActivity)
                .enqueueUniqueOneTimeNetworkWorkRequest<RefreshAssetsWorker>("RefreshAssetsWorker")
            WorkManager.getInstance(this@MainActivity).pruneWork()
        }
        checkRoot()
        checkUpdate()
        checkStorage()

        initView()
        handlerCode(intent)

        sendSafetyNetRequest()
        checkBatteryOptimization()
        refreshStickerAlbum()
    }

    override fun onStart() {
        super.onStart()
        getSystemService<NotificationManager>()?.cancelAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(updatedListener)
    }

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        val batteryOptimize = defaultSharedPreferences.getLong(PREF_BATTERY_OPTIMIZE, 0)
        val cur = System.currentTimeMillis()
        if (cur - batteryOptimize > Constants.INTERVAL_48_HOURS * 30) {
            getSystemService<PowerManager>()?.let { pm ->
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:$packageName")
                        try {
                            startActivity(this)
                        } catch (e: ActivityNotFoundException) {
                            Timber.w("Battery optimization activity not found")
                        }
                    }
                }
            }
            defaultSharedPreferences.putLong(PREF_BATTERY_OPTIMIZE, cur)
        }
    }

    private fun delayShowModifyMobile() = lifecycleScope.launch {
        delay(2000)
        MaterialAlertDialogBuilder(this@MainActivity, R.style.MixinAlertDialogTheme)
            .setTitle(getString(R.string.setting_emergency_change_mobile))
            .setPositiveButton(R.string.change) { dialog, _ ->
                supportFragmentManager.inTransaction {
                    setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom
                    )
                        .add(R.id.root_view, VerifyFragment.newInstance(VerifyFragment.FROM_PHONE))
                        .addToBackStack(null)
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.later) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkRoot() {
        if (RootUtil.isDeviceRooted && defaultSharedPreferences.getBoolean(
                Constants.Account.PREF_BIOMETRICS,
                false
            )
        ) {
            BiometricUtil.deleteKey(this)
        }
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
                    }
                )
        }
    }

    private fun validateSafetyNet(nonce: ByteArray) {
        val client = SafetyNet.getClient(this)
        val task = client.attest(nonce, BuildConfig.SafetyNet_API_KEY)
        task.addOnSuccessListener { safetyResp ->
            accountRepo.updateSession(SessionRequest(deviceCheckToken = safetyResp.jwsResult))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe({}, {})
        }
        task.addOnFailureListener { e ->
            Bugsnag.notify(e)
        }
    }

    private fun checkUpdate() {
        appUpdateManager.registerListener(updatedListener)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        0x01
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
                    alertDialogBuilder()
                        .setTitle(R.string.storage_low_title)
                        .setMessage(R.string.storage_low_message)
                        .setCancelable(false)
                        .setNegativeButton(getString(R.string.know)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                },
                {
                }
            )
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            root_view,
            getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.restart)) { appUpdateManager.completeUpdate() }
            setActionTextColor(getColor(R.color.colorAccent))
            show()
        }
    }

    private fun refreshStickerAlbum() =
        runIntervalTask(REFRESH_STICKER_ALBUM_PRE_KEY, INTERVAL_24_HOURS) {
            jobManager.addJobInBackground(RefreshStickerAlbumJob())
        }

    @Suppress("SameParameterValue")
    private fun runIntervalTask(
        spKey: String,
        interval: Long,
        task: () -> Unit
    ) {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(spKey, 0)
        if (cur - last > interval) {
            task.invoke()
            defaultSharedPreferences.putLong(spKey, cur)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlerCode(intent)
    }

    fun showCapture(scan: Boolean) {
        getScanResult.launch(Pair(ARGS_SHOW_SCAN, scan))
    }

    private val getScanResult = registerForActivityResult(CaptureActivity.CaptureContract()) { data ->
        if (data != null) {
            intent = data
            handlerCode(intent)
        }
    }

    private var bottomSheet: DialogFragment? = null
    private var alertDialog: Dialog? = null

    private fun handlerCode(intent: Intent) {
        if (intent.hasExtra(SCAN)) {
            val scan = intent.getStringExtra(SCAN)!!
            bottomSheet?.dismiss()
            showScanBottom(scan)
        } else if (intent.hasExtra(URL)) {
            val url = intent.getStringExtra(URL)!!
            bottomSheet?.dismiss()
            bottomSheet = LinkBottomSheetDialogFragment.newInstance(url)
            bottomSheet?.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(WALLET)) {
            navigationController.pushWallet()
        } else if (intent.hasExtra(TRANSFER)) {
            val userId = intent.getStringExtra(TRANSFER)
            if (Session.getAccount()?.hasPin == true) {
                TransferFragment.newInstance(userId, supportSwitchAsset = true)
                    .showNow(supportFragmentManager, TransferFragment.TAG)
            } else {
                toast(R.string.transfer_without_pin)
            }
        } else if (intent.extras != null && intent.extras!!.getString("conversation_id", null) != null) {
            alertDialog?.dismiss()
            alertDialog = alert(getString(R.string.group_wait)).show()
            val conversationId = intent.extras!!.getString("conversation_id")!!
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
                                c = Conversation(
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
                                    null
                                )
                                conversation = c
                                conversationDao.insert(c)
                            } else {
                                conversationDao.updateConversation(
                                    data.conversationId,
                                    ownerId,
                                    data.category,
                                    data.name,
                                    data.announcement,
                                    data.muteUntil,
                                    data.createdAt,
                                    ConversationStatus.SUCCESS.ordinal
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
                if (conversation?.isGroup() == true) {
                    innerIntent = ConversationActivity.putIntent(this, conversationId)
                } else {
                    var user = userDao.findPlainUserByConversationId(conversationId)
                    if (user == null) {
                        val response =
                            userService.getUsers(arrayListOf(conversation!!.ownerId!!)).execute()
                                .body()
                        if (response != null && response.isSuccess) {
                            response.data?.let { data ->
                                for (u in data) {
                                    runBlocking { userRepo.upsert(u) }
                                }
                            }
                            user = response.data?.get(0)
                        }
                    }
                    innerIntent = ConversationActivity.putIntent(this, conversationId, user?.userId)
                }
                runOnUiThread { alertDialog?.dismiss() }
                innerIntent
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope).subscribe(
                    {
                        it?.let { intent ->
                            this.startActivity(intent)
                        }
                    },
                    {
                        alertDialog?.dismiss()
                        ErrorHandler.handleError(it)
                    }
                )
        }
    }

    private fun showScanBottom(scan: String) {
        bottomSheet = QrScanBottomSheetDialogFragment.newInstance(scan)
        bottomSheet?.showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
    }

    private fun initView() {
        search_bar.setOnLeftClickListener {
            openSearch()
        }
        search_bar.setOnGroupClickListener {
            navigationController.pushContacts()
        }
        search_bar.setOnAddClickListener {
            addCircle()
        }
        search_bar.setOnConfirmClickListener {
            val circlesFragment =
                supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as CirclesFragment
            circlesFragment.cancelSort()
            search_bar?.action_va?.showPrevious()
        }

        search_bar.setOnBackClickListener {
            search_bar.closeSearch()
        }

        search_bar.mOnQueryTextListener = object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                (supportFragmentManager.findFragmentByTag(SearchFragment.TAG) as? SearchFragment)?.setQueryText(
                    newText
                )
                return true
            }
        }

        search_bar.setSearchViewListener(
            object : MaterialSearchView.SearchViewListener {
                override fun onSearchViewClosed() {
                    navigationController.hideSearch()
                }

                override fun onSearchViewOpened() {
                    navigationController.showSearch()
                }
            }
        )
        search_bar.hideAction = {
            (supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as? CirclesFragment)?.cancelSort()
        }
        search_bar?.logo?.text = defaultSharedPreferences.getString(CIRCLE_NAME, "Mixin")
        root_view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && search_bar.isOpen) {
                search_bar.closeSearch()
                true
            } else {
                false
            }
        }
        supportFragmentManager.beginTransaction().add(R.id.container_circle, circlesFragment, CirclesFragment.TAG).commit()
        observeOtherCircleUnread(defaultSharedPreferences.getString(CIRCLE_ID, null))
    }

    fun openSearch() {
        search_bar?.openSearch()
    }

    fun openWallet() {
        navigationController.pushWallet()
    }

    fun openCircle() {
        search_bar?.showContainer()
    }

    private val circlesFragment by lazy {
        CirclesFragment.newInstance()
    }

    fun closeSearch() {
        search_bar?.closeSearch()
    }

    fun dragSearch(progress: Float) {
        search_bar?.dragSearch(progress)
    }

    fun selectCircle(name: String?, circleId: String?) {
        setCircleName(name)
        defaultSharedPreferences.putString(CIRCLE_NAME, name)
        defaultSharedPreferences.putString(CIRCLE_ID, circleId)
        search_bar?.hideContainer()
        (supportFragmentManager.findFragmentByTag(ConversationListFragment.TAG) as? ConversationListFragment)?.circleId = circleId
        observeOtherCircleUnread(circleId)
    }

    fun setCircleName(name: String?) {
        search_bar?.logo?.text = name ?: "Mixin"
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
        search_bar?.action_va?.showNext()
    }

    private var dotObserver = Observer<Boolean> {
        search_bar.dot.isVisible = it
    }
    private var dotLiveData: LiveData<Boolean>? = null

    private fun observeOtherCircleUnread(circleId: String?) = lifecycleScope.launch {
        dotLiveData?.removeObserver(dotObserver)
        if (circleId == null) {
            search_bar.dot.isVisible = false
            return@launch
        }
        dotLiveData = userRepo.hasUnreadMessage(circleId = circleId)
        dotLiveData?.observe(this@MainActivity, dotObserver)
    }

    private fun addCircle() {
        editDialog {
            titleText = this@MainActivity.getString(R.string.circle_add_title)
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
            val dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                setCancelable(false)
            }
            handleMixinResponse(
                switchContext = Dispatchers.IO,
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
                }
            )
            dialog.dismiss()
        }
    }

    override fun onBackPressed() {
        val searchMessageFragment =
            supportFragmentManager.findFragmentByTag(SearchMessageFragment.TAG)
        val searchSingleFragment =
            supportFragmentManager.findFragmentByTag(SearchSingleFragment.TAG)
        val circlesFragment =
            supportFragmentManager.findFragmentByTag(CirclesFragment.TAG) as BaseFragment
        val conversationCircleEditFragment =
            supportFragmentManager.findFragmentByTag(ConversationCircleEditFragment.TAG)
        when {
            searchMessageFragment != null -> super.onBackPressed()
            searchSingleFragment != null -> super.onBackPressed()
            conversationCircleEditFragment != null -> super.onBackPressed()
            search_bar.isOpen -> search_bar.closeSearch()
            search_bar.containerDisplay -> {
                if (!circlesFragment.onBackPressed()) {
                    search_bar.hideContainer()
                } else {
                    search_bar?.action_va?.showPrevious()
                }
            }
            else -> super.onBackPressed()
        }
    }

    companion object {
        const val URL = "url"
        const val SCAN = "scan"
        const val TRANSFER = "transfer"
        private const val WALLET = "wallet"

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
            intent: Intent
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

package one.mixin.android.ui.home

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.core.content.getSystemService
import androidx.fragment.app.MixinDialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.bugsnag.android.Bugsnag
import com.crashlytics.android.Crashlytics
import com.uber.autodispose.autoDisposable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertConversation
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putLong
import one.mixin.android.job.BackupJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.job.RefreshStickerAlbumJob
import one.mixin.android.job.RefreshStickerAlbumJob.Companion.REFRESH_STICKER_ALBUM_PRE_KEY
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.RotateSignedPreKeyJob
import one.mixin.android.job.RotateSignedPreKeyJob.Companion.ROTATE_SIGNED_PRE_KEY
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.common.NavigationController
import one.mixin.android.ui.common.PinCodeFragment.Companion.FROM_EMERGENCY
import one.mixin.android.ui.common.PinCodeFragment.Companion.FROM_LOGIN
import one.mixin.android.ui.common.PinCodeFragment.Companion.PREF_LOGIN_FROM
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.landing.LoadingFragment
import one.mixin.android.ui.landing.RestoreActivity
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.ui.search.SearchSingleFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.RootUtil
import one.mixin.android.util.Session
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.isGroup
import one.mixin.android.widget.MaterialSearchView
import one.mixin.android.worker.RefreshAccountWorker
import one.mixin.android.worker.RefreshAssetsWorker
import one.mixin.android.worker.RefreshContactWorker
import one.mixin.android.worker.RefreshFcmWorker
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
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
    lateinit var participantDao: ParticipantDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Session.checkToken()) run {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
            return
        }

        if (Session.getAccount()?.full_name.isNullOrBlank()) {
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
        if (!defaultSharedPreferences.getBoolean(LoadingFragment.IS_LOADED, false)) {
            InitializeActivity.showLoading(this)
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
        Bugsnag.setUser(account?.userId, account?.identity_number, account?.full_name)
        Crashlytics.setUserIdentifier(account?.userId)

        jobManager.addJobInBackground(RefreshOneTimePreKeysJob())
        jobManager.addJobInBackground(BackupJob())

        doAsync {
            WorkManager.getInstance(this@MainActivity).enqueueOneTimeNetworkWorkRequest<RefreshAccountWorker>()
            WorkManager.getInstance(this@MainActivity).enqueueOneTimeNetworkWorkRequest<RefreshContactWorker>()
            WorkManager.getInstance(this@MainActivity).enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>()
            WorkManager.getInstance(this@MainActivity).enqueueOneTimeNetworkWorkRequest<RefreshFcmWorker>()
        }

        refreshStickerAlbum()
        rotateSignalPreKey()
        checkRoot()

        initView()
        handlerCode(intent)
    }

    override fun onStart() {
        super.onStart()
        getSystemService<NotificationManager>()?.cancelAll()
    }

    private fun delayShowModifyMobile() = lifecycleScope.launch {
        delay(2000)
        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            .setTitle(getString(R.string.setting_emergency_change_mobile))
            .setPositiveButton(R.string.change) { dialog, _ ->
                supportFragmentManager.inTransaction {
                    setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom, R.anim.slide_out_bottom)
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
        if (RootUtil.isDeviceRooted && defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)) {
            BiometricUtil.deleteKey(this)
        }
    }

    private fun rotateSignalPreKey() =
        runIntervalTask(ROTATE_SIGNED_PRE_KEY, INTERVAL_48_HOURS) {
            jobManager.addJobInBackground(RotateSignedPreKeyJob())
        }

    private fun refreshStickerAlbum() =
        runIntervalTask(REFRESH_STICKER_ALBUM_PRE_KEY, INTERVAL_24_HOURS) {
            jobManager.addJobInBackground(RefreshStickerAlbumJob())
        }

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

    private var bottomSheet: MixinDialogFragment? = null
    private var alertDialog: AlertDialog? = null

    private fun handlerCode(intent: Intent) {
        if (intent.hasExtra(SCAN)) {
            val scan = intent.getStringExtra(SCAN)
            bottomSheet?.dismiss()
            bottomSheet = QrScanBottomSheetDialogFragment.newInstance(scan)
            bottomSheet?.showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(URL)) {
            val url = intent.getStringExtra(URL)
            bottomSheet?.dismiss()
            bottomSheet = LinkBottomSheetDialogFragment.newInstance(url)
            bottomSheet?.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(WALLET)) {
            navigationController.pushWallet()
        } else if (intent.hasExtra(TRANSFER)) {
            val userId = intent.getStringExtra(TRANSFER)
            TransferFragment.newInstance(userId).showNow(supportFragmentManager, TransferFragment.TAG)
        } else if (intent.extras != null && intent.extras!!.getString("conversation_id", null) != null) {
            alertDialog?.dismiss()
            alertDialog = alert(getString(R.string.group_wait)) {}.show()
            val conversationId = intent.extras!!.getString("conversation_id")!!
            Maybe.just(conversationId).map {
                val innerIntent: Intent?
                var conversation = conversationDao.findConversationById(conversationId)
                if (conversation == null) {
                    val response = conversationService.getConversation(conversationId).execute().body()
                    if (response != null && response.isSuccess) {
                        response.data?.let { data ->
                            var ownerId: String = data.creatorId
                            if (data.category == ConversationCategory.CONTACT.name) {
                                ownerId = data.participants.find { p -> p.userId != Session.getAccountId() }!!.userId
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
                                    null)
                                conversation = c
                                conversationDao.insertConversation(c)
                            } else {
                                conversationDao.updateConversation(data.conversationId, ownerId, data.category,
                                    data.name, data.announcement, data.muteUntil, data.createdAt, ConversationStatus.SUCCESS.ordinal)
                            }

                            val participants = mutableListOf<Participant>()
                            val userIdList = mutableListOf<String>()
                            for (p in data.participants) {
                                val item = Participant(conversationId, p.userId, p.role, p.createdAt!!)
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
                        val response = userService.getUsers(arrayListOf(conversation!!.ownerId!!)).execute().body()
                        if (response != null && response.isSuccess) {
                            response.data?.let { data ->
                                for (u in data) {
                                    userRepo.upsert(u)
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
                .autoDisposable(stopScope).subscribe({
                    it?.let { intent ->
                        this.startActivity(intent)
                    }
                }, {
                    alertDialog?.dismiss()
                    ErrorHandler.handleError(it)
                })
        }
    }

    private fun initView() {
        search_bar.setOnLeftClickListener(View.OnClickListener {
            navigationController.pushWallet()
        })

        search_bar.setOnRightClickListener(View.OnClickListener {
            navigationController.pushContacts()
        })

        search_bar.setOnBackClickListener(View.OnClickListener {
            search_bar.closeSearch()
        })

        search_bar.mOnQueryTextListener = object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                (supportFragmentManager.findFragmentByTag(SearchFragment.TAG) as? SearchFragment)?.setQueryText(newText)
                return true
            }
        }

        search_bar.setSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewClosed() {
                navigationController.hideSearch()
            }

            override fun onSearchViewOpened() {
                navigationController.showSearch()
            }
        })
        root_view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && search_bar.isOpen) {
                search_bar.closeSearch()
                true
            } else {
                false
            }
        }
    }

    fun openSearch() {
        search_bar?.openSearch()
    }

    fun closeSearch() {
        search_bar?.closeSearch()
    }

    fun dragSearch(progress: Float) {
        search_bar?.dragSearch(progress)
    }

    override fun onBackPressed() {
        val searchMessageFragment = supportFragmentManager.findFragmentByTag(SearchMessageFragment.TAG)
        val searchSingleFragment = supportFragmentManager.findFragmentByTag(SearchSingleFragment.TAG)
        when {
            searchMessageFragment != null -> super.onBackPressed()
            searchSingleFragment != null -> super.onBackPressed()
            search_bar.isOpen -> search_bar.closeSearch()
            else -> super.onBackPressed()
        }
    }

    companion object {
        private const val URL = "url"
        private const val SCAN = "scan"
        private const val TRANSFER = "transfer"
        private const val WALLET = "wallet"

        fun showUrl(context: Context, url: String) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(URL, url)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run {
                context.startActivity(this)
            }
        }

        fun showTransfer(context: Context, userId: String) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(TRANSFER, userId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run {
                context.startActivity(this)
            }
        }

        fun showWallet(context: Context) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(WALLET, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run {
                context.startActivity(this)
            }
        }

        fun showScan(context: Context, text: String) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(SCAN, text)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run { context.startActivity(this) }
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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
    }
}

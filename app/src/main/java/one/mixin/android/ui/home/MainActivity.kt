package one.mixin.android.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.KeyEvent
import android.view.View
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertConversation
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshContactJob
import one.mixin.android.job.RefreshFcmTokenJob
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.job.RefreshStickerAlbumJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.RotateSignedPreKeyJob
import one.mixin.android.job.UploadContactsJob
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.common.NavigationController
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment.Companion.ARGS_MEMO
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment.Companion.ARGS_TRACE
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.landing.LoadingFragment
import one.mixin.android.ui.landing.SetupNameActivity
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.RootUtil
import one.mixin.android.util.Session
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.User
import one.mixin.android.vo.isGroup
import one.mixin.android.widget.MaterialSearchView
import org.jetbrains.anko.alert
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
    lateinit var conversationDao: ConversationDao
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var userRepo: UserRepository
    @Inject
    lateinit var participantDao: ParticipantDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Session.hasToken()) run {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
            return
        }

        if (!defaultSharedPreferences.getBoolean(LoadingFragment.IS_LOADED, false)) {
            startActivity(SetupNameActivity.getIntent(this, false))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            navigationController.navigateToMessage()
        }

        val account = Session.getAccount()
        Bugsnag.setUser(account?.userId, account?.identity_number, account?.full_name)

        jobManager.addJobInBackground(RefreshOneTimePreKeysJob())
        jobManager.addJobInBackground(RefreshAccountJob())
        jobManager.addJobInBackground(RefreshFcmTokenJob())
        jobManager.addJobInBackground(RefreshContactJob())
        jobManager.addJobInBackground(RefreshAssetsJob())
        jobManager.addJobInBackground(RefreshStickerAlbumJob())
        if (RxPermissions(this).isGranted(Manifest.permission.READ_CONTACTS)) {
            jobManager.addJobInBackground(UploadContactsJob())
        }
        rotateSignalPreKey()
        checkRoot()

        initView()
        handlerCode(intent)
    }

    private fun checkRoot() {
        if (RootUtil.isDeviceRooted && defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)) {
            BiometricUtil.deleteKey(this)
        }
    }

    private fun rotateSignalPreKey() {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(RotateSignedPreKeyJob.ROTATE_SIGNED_PRE_KEY, 0)
        if (last == 0.toLong()) {
            defaultSharedPreferences.putLong(RotateSignedPreKeyJob.ROTATE_SIGNED_PRE_KEY, cur)
        }
        if (cur - last > Constants.INTERVAL_48_HOURS) {
            jobManager.addJobInBackground(RotateSignedPreKeyJob())
            defaultSharedPreferences.putLong(RotateSignedPreKeyJob.ROTATE_SIGNED_PRE_KEY, cur)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlerCode(intent)
    }

    private var bottomSheet: DialogFragment? = null
    private var alertDialog: AlertDialog? = null

    private fun handlerCode(intent: Intent) {
        if (intent.hasExtra(SCAN)) {
            val scan = intent.getStringExtra(SCAN)
            bottomSheet?.dismiss()
            bottomSheet = QrScanBottomSheetDialogFragment.newInstance(scan)
            bottomSheet?.showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(CODE)) {
            val code = intent.getStringExtra(CODE)
            bottomSheet?.dismiss()
            bottomSheet = LinkBottomSheetDialogFragment.newInstance(code)
            bottomSheet?.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(ARGS_AMOUNT)) {
            val user = intent.getParcelableExtra<User>(ARGS_USER)
            val amount = intent.getStringExtra(ARGS_AMOUNT)
            val asset = intent.getParcelableExtra<Asset>(ARGS_ASSET)
            val trace = intent.getStringExtra(ARGS_TRACE)
            val memo = intent.getStringExtra(ARGS_MEMO)
            bottomSheet?.dismiss()
            bottomSheet = TransferBottomSheetDialogFragment.newInstance(user, amount, asset, trace, memo)
            bottomSheet?.showNow(supportFragmentManager, TransferBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(ARGS_USER)) {
            val user = intent.getParcelableExtra<User>(ARGS_USER)
            bottomSheet?.dismiss()
            UserBottomSheetDialogFragment.newInstance(user).showNow(supportFragmentManager, UserBottomSheetDialogFragment.TAG)
        } else if (intent.hasExtra(TRANSFER)) {
            val userId = intent.getStringExtra(TRANSFER)
            TransferFragment.newInstance(userId).showNow(supportFragmentManager, TransferFragment.TAG)
        } else if (intent.extras != null && intent.extras.getString("conversation_id", null) != null) {
            alertDialog?.dismiss()
            alertDialog = alert(getString(R.string.group_wait)) {}.show()
            val conversationId = intent.extras.getString("conversation_id")
            Maybe.just(conversationId).map {
                val innerIntent: Intent?
                var conversation = conversationDao.findConversationById(conversationId)
                if (conversation == null) {
                    val response = conversationService.getConversation(conversationId).execute().body()
                    if (response != null && response.isSuccess) {
                        response.data?.let { data ->
                            var ownerId: String = data.creatorId
                            if (data.category == ConversationCategory.CONTACT.name) {
                                ownerId = data.participants.find { it.userId != Session.getAccountId() }!!.userId
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
                                    data.name, data.announcement, data.createdAt, ConversationStatus.SUCCESS.ordinal)
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
                .autoDisposable(scopeProvider).subscribe({
                    it?.let {
                        this.startActivity(it)
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
            navigationController.hideSearch()
            search_bar.closeSearch()
        })

        search_bar.mOnQueryTextListener = object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                SearchFragment.getInstance().setQueryText(newText)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                SearchFragment.getInstance().setQueryText(query)
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

    override fun onBackPressed() {
        if (search_bar.isOpen) {
            navigationController.hideSearch()
            search_bar.closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val CODE = "code"
        private const val SCAN = "scan"
        private const val TRANSFER = "transfer"

        fun showGroup(context: Context, code: String) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(CODE, code)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }.run {
                context.startActivity(this)
            }
        }

        fun showUser(context: Context, user: User) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(ARGS_USER, user)
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

        fun showPay(context: Context, user: User, transferAmount: String, asset: Asset, trace: String, memo: String?) {
            Intent(context, MainActivity::class.java).apply {
                putExtra(Constants.ARGS_USER, user)
                putExtra(TransferBottomSheetDialogFragment.ARGS_AMOUNT, transferAmount)
                putExtra(ARGS_ASSET, asset)
                putExtra(TransferBottomSheetDialogFragment.ARGS_TRACE, trace)
                putExtra(TransferBottomSheetDialogFragment.ARGS_MEMO, memo)
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

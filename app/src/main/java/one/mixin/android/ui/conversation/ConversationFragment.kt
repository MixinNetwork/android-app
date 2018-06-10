package one.mixin.android.ui.conversation

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v13.view.inputmethod.InputContentInfoCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.net.toUri
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_conversation.*
import kotlinx.android.synthetic.main.view_chat_control.view.*
import kotlinx.android.synthetic.main.view_reply.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_tool.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.REQUEST_FILE
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.REQUEST_GAMERA
import one.mixin.android.extension.REQUEST_VIDEO
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.async
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.openVideo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.removeEnd
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.round
import one.mixin.android.extension.selectDocument
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.toast
import one.mixin.android.extension.translationY
import one.mixin.android.jni.OpusAudioRecorder
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.ui.camera.CameraActivity.Companion.REQUEST_CODE
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.LinkFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.conversation.adapter.AppAdapter
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.adapter.ConversationAdapter.Companion.BILL_TYPE
import one.mixin.android.ui.conversation.adapter.ConversationAdapter.Companion.FILE_TYPE
import one.mixin.android.ui.conversation.adapter.ConversationAdapter.Companion.LINK_TYPE
import one.mixin.android.ui.conversation.adapter.ConversationAdapter.Companion.MESSAGE_TYPE
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.ui.conversation.adapter.MentionAdapter.OnUserClickListener
import one.mixin.android.ui.conversation.adapter.MenuAdapter
import one.mixin.android.ui.conversation.media.DragMediaActivity
import one.mixin.android.ui.conversation.preview.PreviewDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.url.openUrlWithExtraWeb
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.WalletPasswordFragment
import one.mixin.android.util.Attachment
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.DataPackage
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCap
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.canNotForward
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.toUser
import one.mixin.android.websocket.TransferStickerData
import one.mixin.android.websocket.createAckParamBlazeMessage
import one.mixin.android.widget.ChatControlView
import one.mixin.android.widget.ContentEditText
import one.mixin.android.widget.MixinHeadersDecoration
import one.mixin.android.widget.SimpleAnimatorListener
import one.mixin.android.widget.SmoothScrollLinearLayoutManager
import one.mixin.android.widget.SmoothScrollLinearLayoutManager.Companion.FAST_SPEED
import one.mixin.android.widget.SmoothScrollLinearLayoutManager.Companion.MEDIUM_SPEED
import one.mixin.android.widget.SmoothScrollLinearLayoutManager.Companion.SLOW_SPEED
import one.mixin.android.widget.keyboard.KeyboardAwareLinearLayout.OnKeyboardHiddenListener
import one.mixin.android.widget.keyboard.KeyboardAwareLinearLayout.OnKeyboardShownListener
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ConversationFragment : LinkFragment(), OnKeyboardShownListener, OnKeyboardHiddenListener,
    OpusAudioRecorder.Callback {

    companion object {
        const val CONVERSATION_ID = "conversation_id"
        const val RECIPIENT = "recipient"
        private const val IS_GROUP = "is_group"
        private const val IS_BOT = "is_bot"
        private const val MESSAGE_ID = "message_id"
        private const val KEY_WORD = "key_word"
        private const val MESSAGES = "messages"

        private const val COVER_MAX_ALPHA = .4f

        fun putBundle(
            conversationId: String?,
            recipient: User? = null,
            isGroup: Boolean,
            messageId: String?,
            keyword: String?,
            messages: ArrayList<ForwardMessage>?,
            isBot: Boolean = false
        ): Bundle =
            Bundle().apply {
                if (conversationId == null && recipient == null) {
                    throw IllegalArgumentException("lose data")
                }
                messageId?.let {
                    putString(MESSAGE_ID, messageId)
                }
                keyword?.let {
                    putString(KEY_WORD, keyword)
                }
                putString(CONVERSATION_ID, conversationId)
                putParcelable(RECIPIENT, recipient)
                putBoolean(IS_GROUP, isGroup)
                putParcelableArrayList(MESSAGES, messages)
                putBoolean(IS_BOT, isBot)
            }

        fun newInstance(bundle: Bundle) = ConversationFragment().apply { arguments = bundle }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }
    private val chatAdapter: ConversationAdapter by lazy {
        ConversationAdapter(keyword, onItemListener, isGroup)
    }

    private val audioRecorder: OpusAudioRecorder by lazy {
        OpusAudioRecorder(requireContext()).apply { callback = this@ConversationFragment }
    }

    private val appAdapter: AppAdapter by lazy {
        AppAdapter(if (isGroup) {
            AppCap.GROUP.name
        } else {
            AppCap.CONTACT.name
        }, object : AppAdapter.OnAppClickListener {
            override fun onAppClick(url: String, name: String) {
                hideMediaLayout()
                WebBottomSheetDialogFragment
                    .newInstance(url, conversationId, name)
                    .show(fragmentManager, WebBottomSheetDialogFragment.TAG)
            }
        })
    }

    private val menuAdapter: MenuAdapter by lazy {
        MenuAdapter(object : MenuAdapter.OnMenuClickListener {
            override fun onMenuClick(id: Int) {
                when (id) {
                    R.id.menu_camera -> {
                        RxPermissions(activity!!)
                            .request(
                                Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                            .subscribe({ granted ->
                                if (granted) {
                                    imageUri = createImageUri()
                                    imageUri?.let {
                                        openCamera(it)
                                    }
                                } else {
                                    context?.openPermissionSetting()
                                }
                            }, {
                                Bugsnag.notify(it)
                            })
                        hideMediaLayout()
                    }
                    R.id.menu_gallery -> {
                        RxPermissions(activity!!)
                            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                            .subscribe({ granted ->
                                if (granted) {
                                    openGallery()
                                } else {
                                    context?.openPermissionSetting()
                                }
                            }, {
                                Bugsnag.notify(it)
                            })
                        hideMediaLayout()
                    }
                    R.id.menu_video -> {
                        RxPermissions(activity!!)
                            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                            .subscribe({ granted ->
                                if (granted) {
                                    openVideo()
                                } else {
                                    context?.openPermissionSetting()
                                }
                            }, {
                                Bugsnag.notify(it)
                            })
                        hideMediaLayout()
                    }
                    R.id.menu_document -> {
                        RxPermissions(activity!!)
                            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                            .subscribe({ granted ->
                                if (granted) {
                                    selectDocument()
                                } else {
                                    context?.openPermissionSetting()
                                }
                            }, {
                                Bugsnag.notify(it)
                            })
                        hideMediaLayout()
                    }
                    R.id.menu_transfer -> {
                        if (Session.getAccount()?.hasPin == true) {
                            recipient?.let {
                                activity?.supportFragmentManager?.inTransaction {
                                    setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                                        R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                                        .add(R.id.container, TransferFragment.newInstance(it.userId), TransferFragment.TAG)
                                        .addToBackStack(null)
                                }
                            }
                        } else {
                            activity?.supportFragmentManager?.inTransaction {
                                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R
                                    .anim.slide_in_bottom, R.anim.slide_out_bottom)
                                    .add(R.id.container, WalletPasswordFragment.newInstance(), WalletPasswordFragment.TAG)
                                    .addToBackStack(null)
                            }
                        }
                        hideMediaLayout()
                    }
                    R.id.menu_contact -> {
                        activity?.supportFragmentManager?.inTransaction {
                            setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R
                                .anim.slide_in_bottom, R.anim.slide_out_bottom)
                                .add(R.id.container, FriendsFragment.newInstance(conversationId, isGroup, isBot), FriendsFragment.TAG)
                                .addToBackStack(null)
                        }
                    }
                }
            }
        })
    }

    private val onItemListener: ConversationAdapter.OnItemListener by lazy {
        object : ConversationAdapter.OnItemListener() {
            override fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {
                if (isSelect) {
                    chatAdapter.addSelect(messageItem)
                } else {
                    chatAdapter.removeSelect(messageItem)
                }
                when {
                    chatAdapter.selectSet.isEmpty() -> tool_view.fadeOut()
                    chatAdapter.selectSet.size == 1 -> {
                        try {
                            if (chatAdapter.selectSet.valueAt(0)?.type == MessageCategory.SIGNAL_TEXT.name ||
                                chatAdapter.selectSet.valueAt(0)?.type == MessageCategory.PLAIN_TEXT.name) {
                                tool_view.copy_iv.visibility = View.VISIBLE
                            } else {
                                tool_view.copy_iv.visibility = View.GONE
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            tool_view.copy_iv.visibility = View.GONE
                        }
                    }
                    else -> {
                        tool_view.forward_iv.visibility = View.VISIBLE
                        tool_view.copy_iv.visibility = View.GONE
                    }
                }
                if (chatAdapter.selectSet.find { it.canNotForward() } != null) {
                    tool_view.forward_iv.visibility = View.GONE
                } else {
                    tool_view.forward_iv.visibility = View.VISIBLE
                }
                chatAdapter.notifyDataSetChanged()
            }

            override fun onLongClick(messageItem: MessageItem, position: Int): Boolean {
                val b = chatAdapter.addSelect(messageItem)
                if (b) {
                    if (messageItem.type == MessageCategory.SIGNAL_TEXT.name ||
                        messageItem.type == MessageCategory.PLAIN_TEXT.name) {
                        tool_view.copy_iv.visibility = View.VISIBLE
                    } else {
                        tool_view.copy_iv.visibility = View.GONE
                    }
                    if (chatAdapter.selectSet.find { it.canNotForward() } != null) {
                        tool_view.forward_iv.visibility = View.GONE
                    } else {
                        tool_view.forward_iv.visibility = View.VISIBLE
                    }
                    chatAdapter.notifyDataSetChanged()
                    tool_view.fadeIn()
                }
                return b
            }

            override fun onRetryDownload(messageId: String) {
                chatViewModel.retryDownload(messageId)
            }

            override fun onRetryUpload(messageId: String) {
                chatViewModel.retryUpload(messageId)
            }

            override fun onCancel(id: String) {
                chatViewModel.cancel(id)
            }

            override fun onImageClick(messageItem: MessageItem, view: View) {
                DragMediaActivity.show(activity!!, view, messageItem)
            }

            @TargetApi(Build.VERSION_CODES.O)
            override fun onFileClick(messageItem: MessageItem) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O &&
                    messageItem.mediaMimeType.equals("application/vnd.android.package-archive", true)) {
                    if (context!!.packageManager.canRequestPackageInstalls()) {
                        openMedia(messageItem)
                    } else {
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                    }
                } else {
                    openMedia(messageItem)
                }
            }

            @SuppressLint("CheckResult")
            override fun onUserClick(userId: String) {
                chatViewModel.getUserById(userId).autoDisposable(scopeProvider).subscribe({
                    it?.let {
                        UserBottomSheetDialogFragment.newInstance(it, conversationId).show(fragmentManager, UserBottomSheetDialogFragment.TAG)
                    }
                }, {
                    Timber.e(it)
                })
            }

            override fun onUrlClick(url: String) {
                openUrlWithExtraWeb(url, conversationId, requireFragmentManager())
            }

            override fun onMentionClick(name: String) {
            }

            override fun onAddClick() {
                recipient?.let { user ->
                    chatViewModel.updateRelationship(RelationshipRequest(user.userId,
                        RelationshipAction.ADD.name, user.fullName))
                }
            }

            override fun onBlockClick() {
                recipient?.let { user ->
                    chatViewModel.updateRelationship(RelationshipRequest(user.userId,
                        RelationshipAction.BLOCK.name, user.fullName))
                }
            }

            override fun onActionClick(action: String) {
                openUrlWithExtraWeb(action, conversationId, requireFragmentManager())
            }

            override fun onBillClick(messageItem: MessageItem) {
                activity?.addFragment(this@ConversationFragment, TransactionFragment.newInstance(
                    assetId = messageItem.assetId, snapshotId = messageItem.snapshotId), TransactionFragment.TAG)
            }

            override fun onContactCardClick(userId: String) {
                if (userId == Session.getAccountId()) {
                    activity?.addFragment(this@ConversationFragment, ProfileFragment.newInstance(), ProfileFragment.TAG)
                    return
                }
                chatViewModel.getUserById(userId).autoDisposable(scopeProvider).subscribe({
                    it?.let {
                        UserBottomSheetDialogFragment.newInstance(it, conversationId).show(fragmentManager, UserBottomSheetDialogFragment.TAG)
                    }
                }, {
                    Timber.e(it)
                })
            }
        }
    }

    private val decoration by lazy {
        MixinHeadersDecoration(chatAdapter)
    }

    private val mentionAdapter: MentionAdapter by lazy {
        MentionAdapter(object : OnUserClickListener {
            override fun onUserClick(keyword: String?, userName: String) {
                chat_control.chat_et.setText(chat_control.chat_et.text.toString().removeEnd(keyword).plus(" @$userName "))
                chat_control.chat_et.setSelection(chat_control.chat_et.text.length)
                mentionAdapter.clear()
            }
        })
    }

    private var imageUri: Uri? = null
    private fun createImageUri() = Uri.fromFile(context?.getImagePath()?.createImageTemp())

    private val conversationId: String by lazy {
        var cid = arguments!!.getString(CONVERSATION_ID)
        if (cid.isNullOrBlank()) {
            isFirstMessage = true
            cid = generateConversationId(sender.userId, recipient!!.userId)
        }
        cid
    }

    private val isGroup: Boolean by lazy {
        arguments!!.getBoolean(IS_GROUP)
    }

    private val isBot: Boolean by lazy {
        arguments!!.getBoolean(IS_BOT)
    }

    private val messageId: String? by lazy {
        arguments!!.getString(MESSAGE_ID, null)
    }

    private val keyword: String? by lazy {
        arguments!!.getString(KEY_WORD, null)
    }

    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private var recipient: User? = null
    private var app: App? = null

    private var isFirstMessage = false
    private var isFirstLoad = true
    private var isBottom = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_conversation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chat_rv.adapter = chatAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recipient = arguments!!.getParcelable(RECIPIENT)
        MixinApplication.conversationId = conversationId
        initView()
        chat_control.post { sendForwardMessages() }
    }

    private val notificationManager: NotificationManager by lazy {
        context!!.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var showGroupNotification = false
    private var disposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        input_layout.addOnKeyboardShownListener(this)
        input_layout.addOnKeyboardHiddenListener(this)
        chatViewModel.findUnreadMessages(conversationId).flatMap { it ->
            Flowable.fromIterable(it)
        }.autoDisposable(scopeProvider).subscribe({
            chatViewModel.makeMessageReadByConversationId(it.conversationId, sender.userId)
            chatViewModel.sendAckMessage(createAckParamBlazeMessage(it.messageId, MessageStatus.READ))
            notificationManager.cancel(it.conversationId.hashCode())
        }, {
            Timber.e(it)
        })
        if (isGroup) {
            if (context?.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                    ?.getBoolean(conversationId, false) == true) {
                showGroupNotification = true
                showAlert()
            }
            if (disposable == null || disposable?.isDisposed == true) {
                disposable = RxBus.listen(GroupEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it.conversationId == conversationId) {
                            showGroupNotification = true
                            showAlert()
                        }
                    }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        input_layout.removeOnKeyboardShownListener(this)
        input_layout.removeOnKeyboardHiddenListener(this)
        markRead()
        if (disposable?.isDisposed == false) {
            disposable?.dispose()
        }
        AudioPlayer.pause()
    }

    override fun onBackPressed(): Boolean {
        return when {
            tool_view.visibility == VISIBLE -> {
                closeTool()
                true
            }
            sticker_container.visibility == VISIBLE -> {
                hideStickerContainer()
                true
            }
            mediaVisibility -> {
                hideMediaLayout()
                true
            }
            else -> false
        }
    }

    private fun hideStickerContainer() {
        cover.alpha = 0f
        activity?.window?.statusBarColor = Color.TRANSPARENT
        chat_control.reset()
    }

    private fun closeTool() {
        chatAdapter.selectSet.clear()
        chatAdapter.notifyDataSetChanged()
        tool_view.fadeOut()
    }

    private fun markRead() {
        chatViewModel.makeMessageReadByConversationId(conversationId, sender.userId)
        chatAdapter.markread()
    }

    override fun onStop() {
        val draftText = chat_control.chat_et.text
        if (draftText != null) {
            context!!.async {
                chatViewModel.saveDraft(conversationId, draftText.toString())
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        MixinApplication.conversationId = null
        super.onDestroy()
        previewVideoDialogFragment?.release()
        AudioPlayer.release()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        chat_control.callback = chatControlCallback
        chat_control.activity = requireActivity()
        chat_control.inputLayout = input_layout
        chat_control.stickerContainer = sticker_container
        chat_control.chat_et.setOnClickListener {
            cover.alpha = 0f
            activity?.window?.statusBarColor = Color.TRANSPARENT
        }
        chat_control.setCircle(record_circle)
        chat_control.chat_et.setCommitContentListener(object : ContentEditText.OnCommitContentListener {
            override fun onCommitContent(inputContentInfo: InputContentInfoCompat?, flags: Int, opts: Bundle?): Boolean {
                if (inputContentInfo != null) {
                    val url = inputContentInfo.contentUri.getFilePath(requireContext())
                        ?: return false
                    sendImageMessage(url.toUri())
                }
                return true
            }
        })
        chat_control.chat_more_ib.setOnClickListener { toggleMediaLayout() }
        chat_rv.layoutManager = SmoothScrollLinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, true)
        chat_rv.addItemDecoration(decoration)

        chat_rv.itemAnimator = null

        chat_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstPosition = (chat_rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if (firstPosition > 3) {
                    if (isBottom) {
                        isBottom = false
                        showAlert()
                    }
                } else {
                    if (!isBottom) {
                        isBottom = true
                        hideAlert()
                    }
                }
            }
        })
        action_bar.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }

        if (isGroup) {
            renderGroup()
        } else {
            notNullElse(recipient, {
                chatViewModel.findUserById(it.userId).observe(this, Observer {
                    it?.let {
                        recipient = it
                        chatAdapter.recipient = it
                        renderUser(it)
                    }
                })
            }, {
                chatViewModel.findUserByConversationId(conversationId).observe(this, Observer {
                    it?.let {
                        recipient = it
                        chatAdapter.recipient = it
                        renderUser(it)
                    }
                })
            })
        }
        bg_quick_flag.setOnClickListener {
            scrollTo(0)
        }
        chatViewModel.searchConversationById(conversationId)
            .autoDisposable(scopeProvider).subscribe({
                it?.draft?.let { str ->
                    if (isAdded) {
                        chat_control.chat_et.setText(str)
                    }
                }
            }, {
                Timber.e(it)
            })
        tool_view.close_iv.setOnClickListener { activity?.onBackPressed() }
        tool_view.delete_iv.setOnClickListener {
            chatAdapter.selectSet.filter { it.type.endsWith("_AUDIO") }.forEach {
                if (AudioPlayer.get().isPlay(it.messageId)) {
                    AudioPlayer.get().pause()
                }
            }
            chatViewModel.deleteMessages(chatAdapter.selectSet)
            closeTool()
        }
        reply_view.reply_close_iv.setOnClickListener {
            reply_view.fadeOut()
        }
        tool_view.copy_iv.setOnClickListener {
            try {
                context?.getClipboardManager()?.primaryClip =
                    ClipData.newPlainText(null, chatAdapter.selectSet.valueAt(0)?.content)
                context?.toast(R.string.copy_success)
            } catch (e: ArrayIndexOutOfBoundsException) {
            }
            closeTool()
        }
        tool_view.forward_iv.setOnClickListener {
            val list = ArrayList<ForwardMessage>()
            list += chatAdapter.selectSet.sortedBy { it.createdAt }.map {
                when {
                    it.type.endsWith("_TEXT") -> ForwardMessage(ForwardCategory.TEXT.name, content = it.content)
                    it.type.endsWith("_IMAGE") -> ForwardMessage(ForwardCategory.IMAGE.name, id = it.messageId)
                    it.type.endsWith("_DATA") -> ForwardMessage(ForwardCategory.DATA.name, id = it.messageId)
                    it.type.endsWith("_VIDEO") -> ForwardMessage(ForwardCategory.VIDEO.name, id = it.messageId)
                    it.type.endsWith("_CONTACT") -> ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = it.sharedUserId)
                    it.type.endsWith("_STICKER") -> ForwardMessage(ForwardCategory.STICKER.name, id = it.messageId)
                    it.type.endsWith("_AUDIO") -> ForwardMessage(ForwardCategory.AUDIO.name, id = it.messageId)
                    else -> ForwardMessage(ForwardCategory.TEXT.name)
                }
            }
            ForwardActivity.show(context!!, list)
            closeTool()
        }
        chat_control.chat_et.requestFocus()

        media_layout.round(dip(8))
        menu_rv.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        menu_rv.adapter = menuAdapter

        if (!isBot && recipient?.isBot() != true) {
            app_rv.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            app_rv.adapter = appAdapter
        }

        shadow.setOnClickListener {
            hideMediaLayout()
        }

        if (isGroup || isBot || if (recipient != null) recipient!!.isBot() else false) {
            menuAdapter.showTransfer = false
        }
        bindData()
    }

    private fun updateSticker() {
        if (sticker_container.height == input_layout.keyboardHeight) {
            stickerAnim(sticker_container.height, input_layout.height - bar_fl.height - chat_control.height)
        } else {
            stickerAnim(sticker_container.height, input_layout.keyboardHeight)
        }
    }

    private fun bindData() {
        chatViewModel.indexUnread(conversationId, sender.userId)
            .autoDisposable(scopeProvider).subscribe({ unreadCount ->
                chatViewModel.getMessages(conversationId).observe(this, Observer {
                    Flowable.just(it).subscribeOn(Schedulers.io()).map {
                        if (it.size > 0 && !isGroup &&
                            recipient?.relationship == UserRelationship.STRANGER.name &&
                            it.find { it != null && it.userId == sender.userId } == null) {
                            if (unreadCount == 0) {
                                DataPackage(it, -1, false, true)
                            } else {
                                DataPackage(it, unreadCount, false, true)
                            }
                        } else if (isFirstLoad) {
                            var index = -1
                            if (it.isNotEmpty() && !messageId.isNullOrEmpty()) {
                                for (i in 0 until it.size) {
                                    if (it[i]?.messageId == messageId) {
                                        index = i
                                        break
                                    }
                                }
                                if (index == -1) {
                                    chatViewModel.getMessagesMinimal(conversationId).let { ids ->
                                        for (i in 0 until ids.size) {
                                            if (ids[i] == messageId) {
                                                index = i
                                                break
                                            }
                                        }
                                    }
                                }
                                DataPackage(it, index, false)
                            } else if (it.isNotEmpty()) {
                                if (unreadCount == 0) {
                                    DataPackage(it, -1, false)
                                } else {
                                    DataPackage(it, unreadCount - 1, true)
                                }
                            } else {
                                DataPackage(it, -1, false)
                            }
                        } else {
                            DataPackage(it, -1, false)
                        }
                    }.observeOn(AndroidSchedulers.mainThread()).autoDisposable(scopeProvider).subscribe({
                        if (it.data.size > 0) {
                            isFirstMessage = false
                        }
                        val data = it.data
                        val index = it.index
                        when {
                            it.isStranger -> {
                                chatAdapter.hasBottomView = true
                                chatAdapter.submitList(data)
                                chatAdapter.unreadIndex = index
                            }
                            isFirstLoad -> {
                                isFirstLoad = false
                                if (it.hasUnread && index >= 0) {
                                    chatAdapter.unreadIndex = index
                                }
                                chatAdapter.hasBottomView = false
                                chatAdapter.submitList(data)
                                if (index > 0) {
                                    scrollTo(index + 1, chat_rv.measuredHeight * 3 / 4)
                                } else {
                                    scrollTo(0)
                                }
                            }
                            else -> {
                                if (data.size > chatAdapter.itemCount) {
                                    chatAdapter.unreadIndex = null
                                    if (isBottom) {
                                        notNullElse(data[0], {
                                            when (chatAdapter.getItemType(it)) {
                                                MESSAGE_TYPE -> {
                                                    if (it.content != null && it.content.length > 500) {
                                                        scrollTo(0)
                                                    } else {
                                                        scrollTo(0, 0, when {
                                                            it.content == null -> FAST_SPEED
                                                            it.content.length > 30 -> FAST_SPEED
                                                            it.content.length > 15 -> MEDIUM_SPEED
                                                            else -> SLOW_SPEED
                                                        })
                                                    }
                                                }
                                                FILE_TYPE, BILL_TYPE, LINK_TYPE -> scrollTo(0, 0, MEDIUM_SPEED)
                                                else -> {
                                                    scrollTo(0)
                                                }
                                            }
                                        }, {
                                            scrollTo(0)
                                        })
                                    } else {
                                        scrollY(context!!.dpToPx(30f))
                                    }
                                }
                                chatAdapter.hasBottomView = false
                                chatAdapter.submitList(data)
                            }
                        }
                    }, {
                        Timber.e(it)
                    })
                })
            }, {})

        if (isBot || recipient?.isBot() == true) {
            app_rv.visibility = GONE
            extensions.visibility = GONE
            chat_control.chat_bot_ib.visibility = VISIBLE
            chatViewModel.getApp(conversationId, recipient?.userId).observe(this, Observer {
                if (it != null && it.isNotEmpty()) {
                    this.app = it[0]
                    chat_control.chat_bot_ib.setOnClickListener {
                        if (sticker_container.visibility == VISIBLE) {
                            cover.alpha = 0f
                            sticker_container.visibility = GONE
                            activity?.window?.statusBarColor = Color.TRANSPARENT
                            input_layout.hideCurrentInput(chat_control.chat_et)
                        }
                        if (mediaVisibility) {
                            hideMediaLayout()
                        }
                        this.app?.let {
                            openUrlWithExtraWeb(it.homeUri, conversationId, requireFragmentManager())
                        }
                    }
                } else {
                    chat_control.chat_bot_ib.setOnClickListener(null)
                }
            })
        } else {
            chat_control.chat_bot_ib.visibility = GONE
            chatViewModel.getApp(conversationId, recipient?.userId).observe(this, Observer {
                appAdapter.appList = it
                if (appAdapter.appList == null || appAdapter.appList!!.isEmpty()) {
                    app_rv.visibility = GONE
                    extensions.visibility = GONE
                } else {
                    app_rv.visibility = VISIBLE
                    extensions.visibility = VISIBLE
                }
            })
        }
    }

    private fun sendForwardMessages() {
        val messages = arguments!!.getParcelableArrayList<ForwardMessage>(MESSAGES)
        messages?.let {
            for (item in it) {
                if (item.id != null) {
                    sendForwardMessage(item.id)
                } else {
                    when (item.type) {
                        ForwardCategory.CONTACT.name -> {
                            sendContactMessage(item.sharedUserId!!)
                        }
                        ForwardCategory.IMAGE.name -> {
                            sendImageMessage(Uri.parse(item.mediaUrl))
                        }
                        ForwardCategory.DATA.name -> {
                            context?.getAttachment(Uri.parse(item.mediaUrl))?.let {
                                sendAttachmentMessage(it)
                            }
                        }
                        ForwardCategory.VIDEO.name -> {
                            sendVideoMessage(Uri.parse(item.mediaUrl))
                        }
                        ForwardCategory.TEXT.name -> {
                            item.content?.let { sendMessage(it) }
                        }
                    }
                }
            }
        }
        scrollTo(0)
    }

    private inline fun createConversation(crossinline action: () -> Unit) {
        if (isFirstMessage) {
            doAsync {
                chatViewModel.initConversation(conversationId, recipient!!, sender)
                isFirstMessage = false

                uiThread { action() }
            }
        } else {
            action()
        }
    }

    private fun isPlainMessage(): Boolean {
        return if (isGroup) {
            false
        } else if (isBot) {
            true
        } else {
            if (recipient != null) recipient!!.isBot() else false
        }
    }

    @SuppressLint("CheckResult")
    private fun sendImageMessage(uri: Uri) {
        createConversation {
            chatViewModel.sendImageMessage(conversationId, sender, uri, isPlainMessage())
                ?.autoDisposable(scopeProvider)?.subscribe({
                    when (it) {
                        0 -> {
                            markRead()
                        }
                        -1 -> context?.toast(R.string.error_image)
                        -2 -> context?.toast(R.string.error_format)
                    }
                    scrollTo(0)
                }, {
                    context?.toast(R.string.error_image)
                })
        }
    }

    override fun onCancel() {
        chat_control.cancelExternal()
    }

    override fun sendAudio(file: File, duration: Long, waveForm: ByteArray) {
        if (duration < 500) {
            file.deleteOnExit()
        } else {
            createConversation {
                chatViewModel.sendAudioMessage(conversationId, sender, file, duration, waveForm, isPlainMessage())
            }
        }
    }

    private fun sendVideoMessage(uri: Uri) {
        createConversation {
            chatViewModel.sendVideoMessage(conversationId, sender, uri, isPlainMessage()).autoDisposable(scopeProvider)
                .subscribe({
                }, {
                    Timber.e(it)
                })
        }
    }

    private fun sendForwardMessage(id: String?) {
        id?.let {
            createConversation {
                chatViewModel.sendFordMessage(conversationId, sender, it, isPlainMessage())
                    .autoDisposable(scopeProvider).subscribe({
                    }, {
                        Timber.e(id)
                    })
            }
        }
    }

    private fun sendAttachmentMessage(attachment: Attachment) {
        createConversation {
            chatViewModel.sendAttachmentMessage(conversationId, sender, attachment, isPlainMessage())
            markRead()
            scrollTo(0)
        }
    }

    private fun sendStickerMessage(albumId: String, name: String) {
        createConversation {
            chatViewModel.sendStickerMessage(conversationId, sender,
                TransferStickerData(albumId, name), isPlainMessage())
            markRead()
            scrollTo(0)
        }
    }

    private fun sendContactMessage(userId: String) {
        createConversation {
            chatViewModel.sendContactMessage(conversationId, sender, userId, isPlainMessage())
            markRead()
            scrollTo(0)
        }
    }

    private fun sendMessage(message: String) {
        if (message.isNotBlank()) {
            chat_control.chat_et.setText("")
            createConversation {
                chatViewModel.sendTextMessage(conversationId, sender, message, isPlainMessage())
                markRead()
                scrollTo(0)
            }
        }
    }

    private var groupName: String? = null
        set(value) {
            field = value
            chatAdapter.groupName = value
        }
    private var groupNumber: Int = 0

    @SuppressLint("SetTextI18n")
    private fun renderGroup() {
        action_bar.avatar_iv.visibility = VISIBLE
        action_bar.avatar_iv.setOnClickListener {
            showGroupNotification = false
            hideAlert()
            showGroupBottomSheet(false)
        }
        group_flag.setOnClickListener {
            showGroupNotification = false
            context!!.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                .putBoolean(conversationId, false)
            hideAlert()
            showGroupBottomSheet(true)
        }
        chatViewModel.getConversationById(conversationId).observe(this, Observer {
            it?.let {
                groupName = it.name
                action_bar.setSubTitle(groupName
                    ?: "", getString(R.string.title_participants, groupNumber))
                action_bar.avatar_iv.setGroup(it.iconUrl)
                if (it.status == ConversationStatus.QUIT.ordinal) {
                    chat_control.visibility = GONE
                    bottom_cant_send.visibility = VISIBLE
                    chat_control.chat_et.hideKeyboard()
                } else {
                    chat_control.visibility = VISIBLE
                    bottom_cant_send.visibility = GONE
                }
            }
        })
        chatViewModel.getGroupParticipantsLiveData(conversationId).observe(this, Observer { users ->
            users?.let {
                groupNumber = it.size
                action_bar.setSubTitle(groupName
                    ?: "", getString(R.string.title_participants, groupNumber))
            }
            mentionAdapter.list = users
            mentionAdapter.notifyDataSetChanged()
        })
    }

    private fun showGroupBottomSheet(expand: Boolean) {
        val bottomSheetDialogFragment = GroupBottomSheetDialogFragment.newInstance(conversationId = conversationId, expand = expand)
        bottomSheetDialogFragment.show(fragmentManager, GroupBottomSheetDialogFragment.TAG)
        bottomSheetDialogFragment.callback = object : GroupBottomSheetDialogFragment.Callback {
            override fun onDelete() {
                activity?.finish()
            }
        }
    }

    override fun onKeyboardHidden() {
    }

    override fun onKeyboardShown() {
        hideMediaLayout()
    }

    private fun renderUser(user: User) {
        action_bar.setSubTitle(user.fullName ?: "", user.identityNumber)
        action_bar.avatar_iv.visibility = VISIBLE
        action_bar.avatar_iv.setTextSize(16f)
        action_bar.avatar_iv.setInfo(if (user.fullName != null && user.fullName.isNotEmpty()) user.fullName[0]
        else ' ', user.avatarUrl, user.identityNumber)
        action_bar.avatar_iv.setOnClickListener {
            UserBottomSheetDialogFragment.newInstance(user, conversationId).show(fragmentManager, UserBottomSheetDialogFragment.TAG)
        }
        recipient?.let {
            if (it.relationship == UserRelationship.BLOCKING.name) {
                chat_control.visibility = GONE
                bottom_unblock.visibility = VISIBLE
                chat_control.chat_et.hideKeyboard()
            } else {
                chat_control.visibility = VISIBLE
                bottom_unblock.visibility = GONE
            }
        }
        bottom_unblock.setOnClickListener {
            recipient?.let { user ->
                chatViewModel.updateRelationship(RelationshipRequest(user.userId,
                    RelationshipAction.UNBLOCK.name, user.fullName))
            }
        }

        if (user.isBot()) {
            doAsync {
                val app = chatViewModel.findAppById(user.appId!!)
                if (app != null && app.creatorId == Session.getAccountId()) {
                    uiThread {
                        menuAdapter.showTransfer = true
                    }
                }
            }
        }
    }

    private fun clickSticker() {
        hideMediaLayout()
        var stickerAlbumFragment = activity?.supportFragmentManager?.findFragmentByTag(StickerAlbumFragment.TAG)
        if (stickerAlbumFragment == null) {
            stickerAlbumFragment = StickerAlbumFragment.newInstance()
            activity?.replaceFragment(stickerAlbumFragment, R.id.sticker_container, StickerAlbumFragment.TAG)
            stickerAlbumFragment.setCallback(object : StickerAlbumFragment.Callback {
                override fun onMove(dis: Float) {
                    val params = sticker_container.layoutParams
                    val targetH = params.height - dis.toInt()
                    val total = input_layout.height - bar_fl.height - bottom_layout.height
                    if (targetH <= input_layout.keyboardHeight || targetH >= total) return

                    params.height = targetH
                    sticker_container.layoutParams = params

                    val per = Math.abs(dis / (total - input_layout.keyboardHeight))
                    if (dis > 0) {
                        cover.alpha -= COVER_MAX_ALPHA * per
                    } else {
                        cover.alpha += COVER_MAX_ALPHA * per
                    }

                    val coverColor = (cover.background as ColorDrawable).color
                    activity?.window?.statusBarColor = adjustAlpha(coverColor, cover.alpha)
                }

                override fun onRelease() {
                    val curH = sticker_container.height
                    val total = input_layout.height - bar_fl.height - bottom_layout.height
                    val mid = input_layout.keyboardHeight + (total - input_layout.keyboardHeight) / 2
                    val targetH = if (curH <= mid) {
                        input_layout.keyboardHeight
                    } else {
                        total
                    }
                    stickerAnim(curH, targetH)
                }

                override fun onStickerClick(albumId: String, name: String) {
                    sendStickerMessage(albumId, name)
                    if (sticker_container.height != input_layout.keyboardHeight) {
                        stickerAnim(sticker_container.height, input_layout.keyboardHeight)
                    }
                }
            })
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    private fun stickerAnim(curH: Int, targetH: Int) {
        val anim = ValueAnimator.ofInt(curH, targetH).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        anim.addUpdateListener {
            val params = sticker_container.layoutParams
            params.height = it.animatedValue as Int
            sticker_container.layoutParams = params
        }
        anim.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                if (targetH == input_layout.height - bar_fl.height - bottom_layout.height) {
                    cover.alpha = COVER_MAX_ALPHA
                    val coverColor = (cover.background as ColorDrawable).color
                    activity?.window?.statusBarColor = adjustAlpha(coverColor, cover.alpha)
                } else {
                    cover.alpha = 0f
                    activity?.window?.statusBarColor = Color.TRANSPARENT
                }
            }
        })
        anim.start()
    }

    private var mediaVisibility = false
    private fun toggleMediaLayout() {
        if (!mediaVisibility) {
            showMediaLayout()
        } else {
            hideMediaLayout()
        }
    }

    private fun showMediaLayout() {
        if (!mediaVisibility) {
            shadow.fadeIn()
            media_layout.translationY(0f)
            chat_control.chat_et.hideKeyboard()
            hideStickerContainer()
            mediaVisibility = true
        }
    }

    private fun hideMediaLayout() {
        if (mediaVisibility) {
            shadow.fadeOut()
            media_layout.translationY(dip(350).toFloat())
            mediaVisibility = false
        }
    }

    private fun scrollTo(position: Int, offset: Int = -1, type: Float? = null) {
        context?.mainThreadDelayed({
            chat_rv?.let {
                chat_rv.post {
                    chat_rv?.let {
                        if (position == 0 && offset == 0) {
                            chat_rv.run {
                                setTag(R.id.speed_tag, type)
                                smoothScrollToPosition(position)
                            }
                        } else if (offset == -1) {
                            (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                        } else {
                            (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)
                        }
                    }
                }
            }
        }, 30)
    }

    private fun scrollY(offset: Int) {
        context?.mainThreadDelayed({
            chat_rv?.let {
                chat_rv.post {
                    chat_rv?.let {
                        chat_rv.smoothScrollBy(0, offset)
                    }
                }
            }
        }, 30)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                showPreview(it.data) { sendImageMessage(it) }
            }
        } else if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                showPreview(it) { sendImageMessage(it) }
            }
        } else if (requestCode == REQUEST_GAMERA && resultCode == Activity.RESULT_OK) {
            imageUri?.let { imageUri ->
                showPreview(imageUri) { sendImageMessage(it) }
            }
        } else if (requestCode == REQUEST_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            context?.getAttachment(uri)?.let {
                AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
                    .setMessage(if (isGroup) {
                        context!!.getString(R.string.send_file_group, it.filename, groupName)
                    } else {
                        context!!.getString(R.string.send_file, it.filename, recipient?.fullName)
                    })
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.send) { dialog, _ ->
                        sendAttachmentMessage(it)
                        dialog.dismiss()
                    }.show()
            }
        } else if (requestCode == REQUEST_VIDEO && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            showVideoPreview(uri) { sendVideoMessage(it) }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun openMedia(messageItem: MessageItem) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (messageItem.userId == Session.getAccountId()) {
            try {
                messageItem.mediaUrl?.let {
                    intent.setDataAndType(Uri.parse(it), messageItem.mediaMimeType)
                    context!!.startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                context?.toast(R.string.error_unable_to_open_media)
            }
        } else {
            try {
                messageItem.mediaUrl?.let {
                    val file = File(it)
                    if (!file.exists()) {
                        context?.toast(R.string.error_file_exists)
                    } else {
                        val uri = context!!.getUriForFile(file)
                        intent.setDataAndType(uri, messageItem.mediaMimeType)
                        context!!.startActivity(intent)
                    }
                }
            } catch (e: ActivityNotFoundException) {
                context?.toast(R.string.error_unable_to_open_media)
            }
        }
    }

    private fun showAlert() {
        if (isGroup) {
            if (showGroupNotification) {
                group_flag.visibility = VISIBLE
            } else {
                group_flag.visibility = GONE
            }
            if (!isBottom) {
                down_flag.visibility = VISIBLE
            } else {
                down_flag.visibility = GONE
            }
            bg_quick_flag.translationY(0f, 100)
        } else {
            bg_quick_flag.translationY(0f, 100)
        }
    }

    private fun hideAlert() {
        if (isGroup) {
            if (showGroupNotification) {
                group_flag.visibility = VISIBLE
            } else {
                group_flag.visibility = GONE
            }
            if (isBottom) {
                if (showGroupNotification) {
                    bg_quick_flag.translationY(context!!.dpToPx(60f).toFloat(), 100)
                } else if (isBottom) {
                    bg_quick_flag.translationY(context!!.dpToPx(130f).toFloat(), 100)
                }
            }
        } else {
            bg_quick_flag.translationY(context!!.dpToPx(130f).toFloat(), 100)
        }
    }

    private var previewDialogFragment: PreviewDialogFragment? = null

    private fun showPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewDialogFragment == null) {
            previewDialogFragment = PreviewDialogFragment.newInstance()
        }
        previewDialogFragment?.show(fragmentManager, uri, action)
    }

    private var previewVideoDialogFragment: PreviewDialogFragment? = null

    private fun showVideoPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewVideoDialogFragment == null) {
            previewVideoDialogFragment = PreviewDialogFragment.newInstance(true)
        }
        previewVideoDialogFragment?.show(fragmentManager, uri, action)
    }

    private val chatControlCallback = object : ChatControlView.Callback {
        override fun onStickerClick() {
            clickSticker()
        }

        override fun onSendClick(text: String) {
            if (text.isBlank()) {
                if (sticker_container.isShowing) {
                    updateSticker()
                } else {
                    toggleMediaLayout()
                }
            } else {
                sendMessage(text)
            }
        }

        override fun onRecordStart(audio: Boolean) {
            AudioPlayer.get().pause()
            audioRecorder.startRecording()
        }

        override fun isReady(): Boolean {
            return true
        }

        override fun onRecordEnd() {
            audioRecorder.stopRecording(true)
        }

        override fun onRecordCancel() {
            audioRecorder.stopRecording(false)
        }
    }
}
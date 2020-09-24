package one.mixin.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import kotlinx.android.synthetic.main.item_list_conversation.view.*
import kotlinx.android.synthetic.main.item_list_conversation_header.view.*
import kotlinx.android.synthetic.main.view_conversation_bottom.view.*
import kotlinx.android.synthetic.main.view_empty.*
import kotlinx.android.synthetic.main.view_empty.view.*
import kotlinx.android.synthetic.main.view_shadow_circle.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_NOTIFICATION_ON
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.Constants.Mute.MUTE_1_HOUR
import one.mixin.android.Constants.Mute.MUTE_1_WEEK
import one.mixin.android.Constants.Mute.MUTE_1_YEAR
import one.mixin.android.Constants.Mute.MUTE_8_HOURS
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.event.BotEvent
import one.mixin.android.event.CircleDeleteEvent
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.inflate
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putLong
import one.mixin.android.extension.renderConversation
import one.mixin.android.extension.timeAgo
import one.mixin.android.extension.toast
import one.mixin.android.extension.vibrate
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.LinkFragment
import one.mixin.android.ui.common.NavigationController
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.home.bot.BotManagerBottomSheetDialogFragment
import one.mixin.android.ui.home.bot.DefaultTopBots
import one.mixin.android.ui.home.bot.INTERNAL_CAMERA_ID
import one.mixin.android.ui.home.bot.INTERNAL_SCAN_ID
import one.mixin.android.ui.home.bot.INTERNAL_WALLET_ID
import one.mixin.android.ui.home.bot.TOP_BOT
import one.mixin.android.ui.home.bot.getCategoryIcon
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.util.addDynamicShortcut
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.util.maxDynamicShortcutCount
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.*
import one.mixin.android.websocket.SystemConversationAction
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import org.jetbrains.anko.doAsync
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class ConversationListFragment : LinkFragment() {

    lateinit var navigationController: NavigationController

    @Inject
    lateinit var jobManager: MixinJobManager

    private val messagesViewModel by viewModels<ConversationListViewModel>()

    private val messageAdapter by lazy {
        MessageAdapter().apply {
            registerAdapterDataObserver(messageAdapterDataObserver)
        }
    }

    private val messageAdapterDataObserver =
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                if (scrollTop) {
                    scrollTop = false
                    (message_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
                }
            }
        }

    private var distance = 0
    private var shadowVisible = true
    private val touchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    private val vibrateDis by lazy { requireContext().dpToPx(110f) }
    private var vibrated = false
    private var expanded = false

    companion object {
        fun newInstance() = ConversationListFragment()

        const val TAG = "ConversationListFragment"

        private const val DRAG_FRICTION = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_conversation_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationController = NavigationController(activity as MainActivity)
        messageAdapter.headerView = message_rv.inflate(R.layout.item_list_conversation_header, false).apply {
            header_close.setOnClickListener {
                messageAdapter.setShowHeader(false, message_rv)
                requireContext().defaultSharedPreferences.putLong(PREF_NOTIFICATION_ON, System.currentTimeMillis())
            }
            header_settings.setOnClickListener {
                requireContext().openNotificationSetting()
            }
        }
        message_rv.adapter = messageAdapter
        message_rv.itemAnimator = null
        message_rv.setHasFixedSize(true)
        message_rv.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (distance < -touchSlop && !shadowVisible) {
                        shadow_view.animate().translationY(0f).duration = 200
                        distance = 0
                        shadowVisible = true
                    } else if (distance > touchSlop && shadowVisible) {
                        shadow_view.animate().translationY(shadow_view.height.toFloat()).duration = 200
                        distance = 0
                        shadowVisible = false
                    }
                    if ((dy > 0 && shadowVisible) || (dy < 0 && !shadowVisible)) {
                        distance += dy
                    }
                }
            }
        )
        message_rv.callback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val topFl = top_fl ?: return

                if (topFl.isGone) {
                    topFl.isVisible = true
                }
                val targetH = topFl.height + (dis / DRAG_FRICTION).toInt()
                if (targetH <= 0) return

                topFl.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = targetH

                    if (height >= vibrateDis) {
                        if (!vibrated) {
                            requireContext().vibrate(longArrayOf(0, 30))
                            vibrated = true
                        }
                        animDownIcon(true)
                    } else {
                        animDownIcon(false)
                    }
                }
                val progress = min(targetH / vibrateDis.toFloat(), 1f)
                (requireActivity() as MainActivity).dragSearch(progress)
            }

            override fun onRelease(fling: Int) {
                val shouldVibrate = false
                if (shouldVibrate && !vibrated) {
                    requireContext().vibrate(longArrayOf(0, 30))
                    vibrated = true
                }
                val topFl = top_fl ?: return

                val open = (fling == FLING_DOWN && shouldVibrate) || topFl.height >= vibrateDis
                if (open) {
                    (requireActivity() as MainActivity).openSearch()
                } else {
                    (requireActivity() as MainActivity).closeSearch()
                }

                topFl.animateHeight(
                    topFl.height,
                    0,
                    onEndAction = {
                        vibrated = false
                    }
                )
                down_iv.scaleX = 1f
                down_iv.scaleY = 1f
            }
        }
        shadow_view.bot.setOnClickListener {
            BotManagerBottomSheetDialogFragment()
                .show(parentFragmentManager, BotManagerBottomSheetDialogFragment.TAG)
        }

        messageAdapter.onItemListener = object : PagedHeaderAdapter.OnItemListener<ConversationItem> {
            override fun onNormalLongClick(item: ConversationItem): Boolean {
                showBottomSheet(item)
                return true
            }

            override fun onNormalItemClick(item: ConversationItem) {
                if (item.isGroup() && (
                    item.status == ConversationStatus.START.ordinal ||
                        item.status == ConversationStatus.FAILURE.ordinal
                    )
                ) {
                    if (!requireContext().networkConnected()) {
                        context?.toast(R.string.error_network)
                        return
                    }
                    doAsync { messagesViewModel.createGroupConversation(item.conversationId) }
                } else {
                    lifecycleScope.launch {
                        val user = if (item.isContact()) {
                            messagesViewModel.suspendFindUserById(item.ownerId)
                        } else null
                        val messageId = if (item.unseenMessageCount != null && item.unseenMessageCount > 0) {
                            messagesViewModel.findFirstUnreadMessageId(item.conversationId, item.unseenMessageCount - 1)
                        } else null
                        ConversationActivity.fastShow(
                            requireContext(),
                            conversationId = item.conversationId,
                            recipient = user,
                            initialPositionMessageId = messageId,
                            unreadCount = item.unseenMessageCount ?: 0
                        )
                    }

                    doAsync {
                        updateDynamicShortcut(item)
                    }
                }
            }
        }
        start_bn.setOnClickListener {
            circleId.notNullWithElse(
                { circleId ->
                    (requireActivity() as MainActivity).openCircleEdit(circleId)
                },
                {
                    navigationController.pushContacts()
                }
            )
        }
        val circleId = defaultSharedPreferences.getString(CIRCLE_ID, null)
        if (circleId == null) {
            selectCircle(null)
        } else {
            this.circleId = circleId
        }
        RxBus.listen(CircleDeleteEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe {
                if (it.circleId == this.circleId) {
                    (requireActivity() as MainActivity).selectCircle(null, null)
                }
            }
        refreshBot()
        RxBus.listen(BotEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe {
                refreshBot()
            }
    }

    override fun onDestroyView() {
        if (isAdded) {
            messageAdapter.unregisterAdapterDataObserver(messageAdapterDataObserver)
        }
        super.onDestroyView()
    }

    private val observer by lazy {
        Observer<PagedList<ConversationItem>> { pagedList ->
            messageAdapter.submitList(pagedList)
            if (pagedList == null || pagedList.isEmpty()) {
                if (circleId == null) {
                    empty_view.info_tv.setText(R.string.empty_info)
                    empty_view.start_bn.setText(R.string.empty_start)
                } else {
                    empty_view.info_tv.setText(R.string.circle_empty_info)
                    empty_view.start_bn.setText(R.string.circle_empty_start)
                }
                empty_view.visibility = VISIBLE
            } else {
                empty_view.visibility = GONE
                pagedList
                    .filter { item: ConversationItem? ->
                        item?.isGroup() == true && (item.iconUrl() == null || !File(item.iconUrl() ?: "").exists())
                    }.forEach {
                        jobManager.addJobInBackground(GenerateAvatarJob(it.conversationId))
                    }
            }
        }
    }

    private var liveData: LiveData<PagedList<ConversationItem>>? = null
    var circleId: String? = null
        set(value) {
            if (field != value) {
                field = value
                selectCircle(circleId)
            }
        }

    private var scrollTop = false
    private fun selectCircle(circleId: String?) {
        liveData?.removeObserver(observer)
        val liveData = messagesViewModel.observeConversations(circleId)
        liveData.observe(viewLifecycleOwner, observer)
        scrollTop = true
        this.liveData = liveData
    }

    private fun animDownIcon(expand: Boolean) {
        val shouldAnim = if (expand) !expanded else expanded
        if (!shouldAnim) return

        down_iv.animate().apply {
            interpolator = BounceInterpolator()
        }.scaleX(if (expand) 1.5f else 1f).start()
        down_iv.animate().apply {
            interpolator = BounceInterpolator()
        }.scaleY(if (expand) 1.5f else 1f).start()
        expanded = expand
    }

    @SuppressLint("InflateParams")
    fun showBottomSheet(conversationItem: ConversationItem) {
        val conversationId = conversationItem.conversationId
        val isMute = conversationItem.isMute()
        val hasPin = conversationItem.pinTime != null
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_conversation_bottom, null)
        builder.setCustomView(view)
        view.mute_tv.setText(
            if (isMute) {
                R.string.un_mute
            } else {
                R.string.mute
            }
        )
        val bottomSheet = builder.create()
        view.mute_tv.setOnClickListener {
            if (isMute) {
                unMute(conversationItem)
            } else {
                showMuteDialog(conversationItem)
            }
            bottomSheet.dismiss()
        }
        view.delete_tv.setOnClickListener {
            alertDialogBuilder()
                .setMessage(getString(R.string.conversation_delete_tip))
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    bottomSheet.dismiss()
                }
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val lm = message_rv.layoutManager as LinearLayoutManager
                    val lastCompleteVisibleItem = lm.findLastCompletelyVisibleItemPosition()
                    val firstCompleteVisibleItem = lm.findFirstCompletelyVisibleItemPosition()
                    if (lastCompleteVisibleItem - firstCompleteVisibleItem <= messageAdapter.itemCount &&
                        lm.findFirstVisibleItemPosition() == 0
                    ) {
                        shadow_view.animate().translationY(0f).duration = 200
                    }
                    messagesViewModel.deleteConversation(conversationId)
                    bottomSheet.dismiss()
                }
                .show()
        }
        if (hasPin) {
            view.pin_tv.setText(R.string.conversation_pin_clear)
            view.pin_tv.setOnClickListener {
                messagesViewModel.updateConversationPinTimeById(conversationId, circleId, null)
                bottomSheet.dismiss()
            }
        } else {
            view.pin_tv.setText(R.string.conversation_pin)
            view.pin_tv.setOnClickListener {
                messagesViewModel.updateConversationPinTimeById(conversationId, circleId, nowInUtc())
                bottomSheet.dismiss()
            }
        }

        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()
        val notificationTime = requireContext().defaultSharedPreferences.getLong(PREF_NOTIFICATION_ON, 0)
        if (System.currentTimeMillis() - notificationTime > INTERVAL_48_HOURS) {
            messageAdapter.setShowHeader(!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled(), message_rv)
        } else {
            messageAdapter.setShowHeader(false, message_rv)
        }
    }

    private fun updateDynamicShortcut(item: ConversationItem) {
        val dynamicShortcuts = ShortcutManagerCompat.getDynamicShortcuts(requireContext())
        val exist = dynamicShortcuts.find { it.id == item.conversationId }
        if (exist != null) {
            dynamicShortcuts.remove(exist)
            dynamicShortcuts.add(exist)
            val result = ShortcutManagerCompat.updateShortcuts(requireContext(), dynamicShortcuts)
            Timber.d("$TAG updateShortcuts result: $result")
            return
        }
        Glide.with(requireContext())
            .asBitmap()
            .load(item.iconUrl())
            .listener(
                object : RequestListener<Bitmap> {
                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (dynamicShortcuts.size >= maxDynamicShortcutCount) {
                            val last = dynamicShortcuts[dynamicShortcuts.size - 1]
                            ShortcutManagerCompat.removeDynamicShortcuts(requireContext(), listOf(last.id))
                        }
                        val result = addDynamicShortcut(
                            requireContext(),
                            item.conversationId,
                            item.getConversationName(), resource!!,
                            ConversationActivity.getShortcutIntent(
                                requireContext(),
                                item.conversationId,
                                item.ownerId
                            )
                        )
                        Timber.d("$TAG addDynamicShortcut result: $result")
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }
            ).submit()
    }

    private fun refreshBot() {
        lifecycleScope.launch {
            shadow_view.right_icon.isInvisible = true
            shadow_view.left_icon.isInvisible = true
            requireContext().defaultSharedPreferences.getString(TOP_BOT, DefaultTopBots)?.let {
                GsonHelper.customGson.fromJson(it, Array<String>::class.java).forEachIndexed { index, id ->
                    if (index > 1) return@launch
                    val view: ImageView =
                        if (index == 0) {
                            shadow_view.left_icon
                        } else {
                            shadow_view.right_icon
                        }

                    when (id) {
                        INTERNAL_WALLET_ID -> {
                            view.isVisible = true
                            view.setImageResource(R.drawable.ic_bot_category_wallet)
                            view.setOnClickListener {
                                (requireActivity() as MainActivity).openWallet()
                            }
                        }
                        INTERNAL_CAMERA_ID -> {
                            view.isVisible = true
                            view.setImageResource(R.drawable.ic_bot_category_camera)
                            view.setOnClickListener {
                                openCamera(false)
                            }
                        }
                        INTERNAL_SCAN_ID -> {
                            view.isVisible = true
                            view.setImageResource(R.drawable.ic_bot_category_scan)
                            view.setOnClickListener {
                                openCamera(true)
                            }
                        }
                        else -> {
                            messagesViewModel.findAppById(id)?.notNullWithElse(
                                { app ->
                                    view.isVisible = true
                                    view.setImageResource(app.getCategoryIcon())
                                    view.setOnClickListener {
                                        WebBottomSheetDialogFragment.newInstance(app.homeUri, null, app).show(
                                            parentFragmentManager,
                                            WebBottomSheetDialogFragment.TAG
                                        )
                                    }
                                },
                                {
                                    view.isInvisible = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openCamera(scan: Boolean) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    (requireActivity() as? MainActivity)?.showCapture(scan)
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    class MessageAdapter : PagedHeaderAdapter<ConversationItem>(ConversationItem.DIFF_CALLBACK) {

        override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
            MessageHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_list_conversation,
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is MessageHolder) {
                val pos = getPos(position)
                getItem(pos)?.let {
                    holder.bind(onItemListener, it)
                }
            }
        }
    }

    class MessageHolder constructor(containerView: View) : NormalHolder(containerView) {
        var context: Context = itemView.context
        private fun getText(id: Int) = context.getText(id).toString()

        @SuppressLint("SetTextI18n")
        fun bind(
            onItemClickListener: PagedHeaderAdapter.OnItemListener<ConversationItem>?,
            conversationItem: ConversationItem
        ) {
            val id = Session.getAccountId()
            conversationItem.getConversationName().let {
                itemView.name_tv.text = it
            }
            itemView.group_name_tv.visibility = GONE
            itemView.mention_flag.isVisible = conversationItem.mentionCount != null && conversationItem.mentionCount > 0
            when {
                conversationItem.messageStatus == MessageStatus.FAILED.name -> {
                    conversationItem.content?.let {
                        setConversationName(conversationItem)
                        itemView.msg_tv.setText(R.string.conversation_waiting)
                    }
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_fail)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_TEXT.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_TEXT.name -> {
                    conversationItem.content?.let {
                        setConversationName(conversationItem)
                        if (conversationItem.mentions != null) {
                            itemView.msg_tv.renderConversation(it, MentionRenderCache.singleton.getMentionRenderContext(conversationItem.mentions) {})
                        } else {
                            itemView.msg_tv.text = it
                        }
                    }
                    null
                }
                conversationItem.contentType == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> {
                    itemView.msg_tv.setText(R.string.conversation_status_transfer)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_transfer)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_STICKER.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_STICKER.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_sticker)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_stiker)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_IMAGE.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_IMAGE.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_pic)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_pic)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_VIDEO.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_VIDEO.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_video)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_video)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_LIVE.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_LIVE.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_live)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_live)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_DATA.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_DATA.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_file)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_file)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_POST.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_POST.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.text = MarkwonUtil.parseContent(conversationItem.content)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_file)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_LOCATION.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_LOCATION.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_location)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_location)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_AUDIO.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_AUDIO.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_audio)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_audio)
                }
                conversationItem.contentType == MessageCategory.APP_BUTTON_GROUP.name -> {
                    itemView.group_name_tv.visibility = GONE
                    val buttons = Gson().fromJson(conversationItem.content, Array<AppButtonData>::class.java)
                    var content = ""
                    buttons.map { content += "[" + it.label + "]" }
                    itemView.msg_tv.text = content
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_touch_app)
                }
                conversationItem.contentType == MessageCategory.APP_CARD.name -> {
                    itemView.group_name_tv.visibility = GONE
                    val cardData = Gson().fromJson(conversationItem.content, AppCardData::class.java)
                    itemView.msg_tv.text = "[${cardData.title}]"
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_touch_app)
                }
                conversationItem.contentType == MessageCategory.SIGNAL_CONTACT.name ||
                    conversationItem.contentType == MessageCategory.PLAIN_CONTACT.name -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_contact)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_contact)
                }
                conversationItem.isCallMessage() -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_voice)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_voice)
                }
                conversationItem.isRecall() -> {
                    setConversationName(conversationItem)
                    if (id == conversationItem.senderId) {
                        itemView.msg_tv.setText(R.string.chat_recall_me)
                    } else {
                        itemView.msg_tv.text = itemView.context.getString(R.string.chat_recall_delete)
                    }
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_recall)
                }
                conversationItem.isGroupVoiceCall() -> {
                    setConversationName(conversationItem)
                    itemView.msg_tv.setText(R.string.conversation_status_group_call)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_voice)
                }
                conversationItem.contentType == MessageCategory.SYSTEM_CONVERSATION.name -> {
                    when (conversationItem.actionName) {
                        SystemConversationAction.CREATE.name -> {
                            itemView.msg_tv.text =
                                String.format(
                                    getText(R.string.chat_group_create),
                                    if (id == conversationItem.senderId) {
                                        getText(R.string.chat_you_start)
                                    } else {
                                        conversationItem.name
                                    },
                                    conversationItem.groupName
                                )
                        }
                        SystemConversationAction.ADD.name -> {
                            itemView.msg_tv.text =
                                String.format(
                                    getText(R.string.chat_group_add),
                                    if (id == conversationItem.senderId) {
                                        getText(R.string.chat_you_start)
                                    } else {
                                        conversationItem.senderFullName
                                    },
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.chat_you)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.REMOVE.name -> {
                            itemView.msg_tv.text =
                                String.format(
                                    getText(R.string.chat_group_remove),
                                    if (id == conversationItem.senderId) {
                                        getText(R.string.chat_you_start)
                                    } else {
                                        conversationItem.senderFullName
                                    },
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.chat_you)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.JOIN.name -> {
                            itemView.msg_tv.text =
                                String.format(
                                    getText(R.string.chat_group_join),
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.chat_you_start)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.EXIT.name -> {
                            itemView.msg_tv.text =
                                String.format(
                                    getText(R.string.chat_group_exit),
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.chat_you_start)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.ROLE.name -> {
                            itemView.msg_tv.text = getText(R.string.group_role)
                        }
                        else -> {
                            itemView.msg_tv.text = ""
                        }
                    }
                    null
                }
                else -> {
                    itemView.msg_tv.text = ""
                    null
                }
            }.also {
                it.notNullWithElse(
                    { drawable ->
                        drawable.setBounds(0, 0, itemView.context.dpToPx(12f), itemView.context.dpToPx(12f))
                        itemView.msg_type.setImageDrawable(drawable)
                        itemView.msg_type.isVisible = true
                    },
                    {
                        itemView.msg_type.isVisible = false
                    }
                )
            }

            if (conversationItem.senderId == Session.getAccountId() &&
                conversationItem.contentType != MessageCategory.SYSTEM_CONVERSATION.name &&
                conversationItem.contentType != MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name &&
                !conversationItem.isCallMessage() && !conversationItem.isRecall() &&
                !conversationItem.isGroupVoiceCall()
            ) {
                when (conversationItem.messageStatus) {
                    MessageStatus.SENDING.name -> AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_status_sending
                    )
                    MessageStatus.SENT.name -> AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_status_sent_large
                    )
                    MessageStatus.DELIVERED.name -> AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_status_delivered
                    )
                    MessageStatus.READ.name -> AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_status_read_dark
                    )
                    else -> {
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_sending)
                    }
                }.also {
                    it?.setBounds(0, 0, itemView.context.dpToPx(14f), itemView.context.dpToPx(14f))
                    itemView.msg_status.setImageDrawable(it)
                    itemView.msg_status.visibility = VISIBLE
                }
            } else {
                itemView.msg_status.visibility = GONE
            }
            conversationItem.createdAt?.let {
                itemView.time_tv.timeAgo(it)
            }
            if (conversationItem.pinTime == null) {
                itemView.msg_pin.visibility = GONE
                if (conversationItem.isGroup() && conversationItem.status == ConversationStatus.START.ordinal) {
                    itemView.pb.visibility = VISIBLE
                    itemView.unread_tv.visibility = GONE
                } else {
                    itemView.pb.visibility = GONE
                    conversationItem.unseenMessageCount.notEmptyWithElse(
                        { itemView.unread_tv.text = "$it"; itemView.unread_tv.visibility = VISIBLE },
                        { itemView.unread_tv.visibility = GONE }
                    )

                    if (conversationItem.isGroup() && conversationItem.status == ConversationStatus.FAILURE.ordinal) {
                        itemView.msg_tv.text = getText(R.string.group_click_create_tip)
                    }
                }
            } else {
                itemView.msg_pin.visibility = VISIBLE
                if (conversationItem.isGroup() && conversationItem.status == ConversationStatus.START.ordinal) {
                    itemView.pb.visibility = VISIBLE
                    itemView.unread_tv.visibility = GONE
                } else {
                    itemView.pb.visibility = GONE
                    conversationItem.unseenMessageCount.notEmptyWithElse(
                        { itemView.unread_tv.text = "$it"; itemView.unread_tv.visibility = VISIBLE; },
                        { itemView.unread_tv.visibility = GONE }
                    )
                }
            }

            itemView.mute_iv.visibility = if (conversationItem.isMute()) VISIBLE else GONE
            if (conversationItem.isMute()) {
                itemView.unread_tv.setBackgroundResource(R.drawable.bg_unread_mute)
                itemView.unread_tv.setTextColor(context.colorFromAttribute(R.attr.badger_text_mute))
            } else {
                itemView.unread_tv.setBackgroundResource(R.drawable.bg_unread)
                itemView.unread_tv.setTextColor(context.colorFromAttribute(R.attr.badger_text))
            }

            conversationItem.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)

            if (conversationItem.isGroup()) {
                itemView.avatar_iv.setGroup(conversationItem.iconUrl())
            } else {
                itemView.avatar_iv.setInfo(
                    conversationItem.getConversationName(),
                    conversationItem.iconUrl(),
                    conversationItem.ownerId
                )
            }
            itemView.setOnClickListener { onItemClickListener?.onNormalItemClick(conversationItem) }
            itemView.setOnLongClickListener {
                onItemClickListener.notNullWithElse({ it.onNormalLongClick(conversationItem) }, false)
            }
        }

        @SuppressLint("SetTextI18n")
        private fun setConversationName(conversationItem: ConversationItem) {
            if (conversationItem.isGroup() && conversationItem.senderId != Session.getAccountId()) {
                itemView.group_name_tv.text = "${conversationItem.senderFullName}: "
                itemView.group_name_tv.visibility = VISIBLE
            } else {
                itemView.group_name_tv.visibility = GONE
            }
        }
    }

    private fun showMuteDialog(conversationItem: ConversationItem) {
        val choices = arrayOf(
            getString(R.string.contact_mute_1hour),
            getString(R.string.contact_mute_8hours),
            getString(R.string.contact_mute_1week),
            getString(R.string.contact_mute_1year)
        )
        var duration = MUTE_8_HOURS
        var whichItem = 0
        alertDialogBuilder()
            .setTitle(getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                if (conversationItem.isGroup()) {
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = { messagesViewModel.mute(duration.toLong(), conversationId = conversationItem.conversationId) },
                            successBlock = { response ->
                                messagesViewModel.updateGroupMuteUntil(conversationItem.conversationId, response.data!!.muteUntil)
                                context?.toast(getString(R.string.contact_mute_title) + " ${conversationItem.groupName} " + choices[whichItem])
                            }
                        )
                    }
                } else {
                    val account = Session.getAccount()
                    account?.let {
                        lifecycleScope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    messagesViewModel.mute(
                                        duration.toLong(),
                                        senderId = it.userId,
                                        recipientId = conversationItem.ownerId
                                    )
                                },
                                successBlock = { response ->
                                    messagesViewModel.updateMuteUntil(conversationItem.ownerId, response.data!!.muteUntil)
                                    context?.toast(getString(R.string.contact_mute_title) + "  ${conversationItem.name}  " + choices[whichItem])
                                }
                            )
                        }
                    }
                }

                dialog.dismiss()
            }
            .setSingleChoiceItems(choices, 0) { _, which ->
                whichItem = which
                when (which) {
                    0 -> duration = MUTE_1_HOUR
                    1 -> duration = MUTE_8_HOURS
                    2 -> duration = MUTE_1_WEEK
                    3 -> duration = MUTE_1_YEAR
                }
            }
            .show()
    }

    private fun unMute(conversationItem: ConversationItem) {
        if (conversationItem.isGroup()) {
            lifecycleScope.launch {
                handleMixinResponse(
                    invokeNetwork = {
                        messagesViewModel.mute(0, conversationId = conversationItem.conversationId)
                    },
                    successBlock = { response ->
                        messagesViewModel.updateGroupMuteUntil(conversationItem.conversationId, response.data!!.muteUntil)
                        context?.toast(getString(R.string.un_mute) + " ${conversationItem.groupName}")
                    }
                )
            }
        } else {
            Session.getAccount()?.let {
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            messagesViewModel.mute(0, senderId = it.userId, recipientId = conversationItem.ownerId)
                        },
                        successBlock = { response ->
                            messagesViewModel.updateMuteUntil(conversationItem.ownerId, response.data!!.muteUntil)
                            context?.toast(getString(R.string.un_mute) + " ${conversationItem.name}")
                        }
                    )
                }
            }
        }
    }
}

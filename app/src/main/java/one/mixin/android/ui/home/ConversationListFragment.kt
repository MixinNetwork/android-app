package one.mixin.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
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
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants.CIRCLE.CIRCLE_ID
import one.mixin.android.Constants.Mute.MUTE_1_HOUR
import one.mixin.android.Constants.Mute.MUTE_1_WEEK
import one.mixin.android.Constants.Mute.MUTE_1_YEAR
import one.mixin.android.Constants.Mute.MUTE_8_HOURS
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentConversationListBinding
import one.mixin.android.databinding.ItemListConversationBinding
import one.mixin.android.databinding.ViewConversationBottomBinding
import one.mixin.android.event.BotEvent
import one.mixin.android.event.CircleDeleteEvent
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.notEmptyWithElse
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.renderMessage
import one.mixin.android.extension.timeAgo
import one.mixin.android.extension.toast
import one.mixin.android.job.GenerateAvatarJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.LinkFragment
import one.mixin.android.ui.common.NavigationController
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.bot.BotManagerBottomSheetDialogFragment
import one.mixin.android.ui.home.bot.DefaultTopBots
import one.mixin.android.ui.home.bot.INTERNAL_CAMERA_ID
import one.mixin.android.ui.home.bot.INTERNAL_SCAN_ID
import one.mixin.android.ui.home.bot.INTERNAL_WALLET_ID
import one.mixin.android.ui.home.bot.TOP_BOT
import one.mixin.android.ui.home.bot.getCategoryIcon
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.BulletinBoard
import one.mixin.android.util.EmergencyContactBulletin
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.NewWalletBulletin
import one.mixin.android.util.NotificationBulletin
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.PinMessageMinimal
import one.mixin.android.vo.explain
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isCallMessage
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isContactConversation
import one.mixin.android.vo.isData
import one.mixin.android.vo.isGroupCall
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPin
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.showVerifiedOrBot
import one.mixin.android.websocket.SystemConversationAction
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BulletinView
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.picker.toTimeInterval
import java.io.File
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class ConversationListFragment : LinkFragment() {

    private lateinit var navigationController: NavigationController

    @Inject
    lateinit var jobManager: MixinJobManager

    private var _binding: FragmentConversationListBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val conversationListViewModel by viewModels<ConversationListViewModel>()

    private val messageAdapter by lazy {
        MessageAdapter().apply {
            registerAdapterDataObserver(messageAdapterDataObserver)
        }
    }

    private lateinit var bulletinView: BulletinView

    private val bulletinBoard = BulletinBoard()

    private val messageAdapterDataObserver =
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                if (scrollTop) {
                    scrollTop = false
                    if (isAdded) {
                        (binding.messageRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            0,
                            0
                        )
                    }
                }
            }
        }

    private var distance = 0
    private var shadowVisible = true
    private val touchSlop: Int by lazy {
        ViewConfiguration.get(requireContext()).scaledTouchSlop
    }

    private val vibrateDis by lazy { requireContext().dpToPx(110f) }
    private var vibrated = false
    private var expanded = false

    private var enterJob: Job? = null

    companion object {
        fun newInstance() = ConversationListFragment()

        const val TAG = "ConversationListFragment"

        private const val DRAG_FRICTION = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getContentView() = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationController = NavigationController(activity as MainActivity)
        bulletinView = BulletinView(requireContext()).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 8.dp
                marginStart = 16.dp
                marginEnd = 16.dp
            }
        }
        messageAdapter.headerView = bulletinView
        binding.messageRv.adapter = messageAdapter
        binding.messageRv.itemAnimator = null
        binding.messageRv.setHasFixedSize(true)
        binding.messageRv.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (distance < -touchSlop && !shadowVisible) {
                        binding.shadowFl.animate().translationY(0f).duration = 200
                        distance = 0
                        shadowVisible = true
                    } else if (distance > touchSlop && shadowVisible) {
                        binding.shadowFl.animate()
                            .translationY(binding.shadowFl.height.toFloat()).duration = 200
                        distance = 0
                        shadowVisible = false
                    }
                    if ((dy > 0 && shadowVisible) || (dy < 0 && !shadowVisible)) {
                        distance += dy
                    }
                }
            }
        )
        binding.messageRv.callback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val topFl = binding.topFl

                if (topFl.isGone) {
                    topFl.isVisible = true
                }
                val targetH = topFl.height + (dis / DRAG_FRICTION).toInt()
                if (targetH <= 0) return

                topFl.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = targetH

                    if (height >= vibrateDis) {
                        if (!vibrated) {
                            requireContext().clickVibrate()
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
                    requireContext().clickVibrate()
                    vibrated = true
                }
                val topFl = _binding?.topFl

                val open = (fling == FLING_DOWN && shouldVibrate) || topFl?.height ?: 0 >= vibrateDis
                if (open) {
                    (requireActivity() as MainActivity).openSearch()
                } else {
                    (requireActivity() as MainActivity).closeSearch()
                }

                topFl?.animateHeight(
                    topFl.height,
                    0,
                    onEndAction = {
                        vibrated = false
                    }
                )
                _binding?.downIv?.apply {
                    scaleX = 1f
                    scaleY = 1f
                }
            }
        }
        binding.shadowView.more.setOnClickListener {
            BotManagerBottomSheetDialogFragment()
                .show(parentFragmentManager, BotManagerBottomSheetDialogFragment.TAG)
        }

        messageAdapter.onItemListener =
            object : PagedHeaderAdapter.OnItemListener<ConversationItem> {
                override fun onNormalLongClick(item: ConversationItem): Boolean {
                    showBottomSheet(item)
                    return true
                }

                override fun onNormalItemClick(item: ConversationItem) {
                    if (item.isGroupConversation() && (
                        item.status == ConversationStatus.START.ordinal ||
                            item.status == ConversationStatus.FAILURE.ordinal
                        )
                    ) {
                        if (!requireContext().networkConnected()) {
                            toast(R.string.Network_error)
                            return
                        }
                        lifecycleScope.launch(Dispatchers.IO) { conversationListViewModel.createGroupConversation(item.conversationId) }
                    } else {
                        enterJob?.cancel()
                        enterJob = lifecycleScope.launch {
                            val user = if (item.isContactConversation()) {
                                conversationListViewModel.suspendFindUserById(item.ownerId)
                            } else null
                            val messageId =
                                if (item.unseenMessageCount != null && item.unseenMessageCount > 0) {
                                    conversationListViewModel.findFirstUnreadMessageId(
                                        item.conversationId,
                                        item.unseenMessageCount - 1
                                    )
                                } else null
                            ConversationActivity.fastShow(
                                requireContext(),
                                conversationId = item.conversationId,
                                recipient = user,
                                initialPositionMessageId = messageId,
                                unreadCount = item.unseenMessageCount ?: 0
                            )
                        }
                    }
                }
            }
        binding.emptyView.startBn.setOnClickListener {
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
        _binding = null
    }

    private val observer by lazy {
        Observer<PagedList<ConversationItem>> { pagedList ->
            messageAdapter.submitList(pagedList)
            if (pagedList == null || pagedList.isEmpty()) {
                if (circleId == null) {
                    binding.emptyView.infoTv.setText(R.string.chat_list_empty_info)
                    binding.emptyView.startBn.setText(R.string.Start_Messaging)
                } else {
                    binding.emptyView.infoTv.setText(R.string.circle_no_conversation_hint)
                    binding.emptyView.startBn.setText(R.string.Add_conversations)
                }
                binding.emptyView.root.isVisible = true
            } else {
                binding.emptyView.root.isVisible = false
                pagedList
                    .filter { item: ConversationItem? ->
                        item?.isGroupConversation() == true && (
                            item.iconUrl() == null || !File(
                                item.iconUrl() ?: ""
                            ).exists()
                            )
                    }.forEach {
                        jobManager.addJobInBackground(GenerateAvatarJob(it.conversationId))
                    }
            }
        }
    }

    private var conversationLiveData: LiveData<PagedList<ConversationItem>>? = null
    var circleId: String? = null
        set(value) {
            if (field != value) {
                field = value
                selectCircle(circleId)
            }
        }

    private var scrollTop = false
    private fun selectCircle(circleId: String?) {
        conversationLiveData?.removeObserver(observer)
        val liveData = conversationListViewModel.observeConversations(circleId)
        liveData.observe(viewLifecycleOwner, observer)
        scrollTop = true
        this.conversationLiveData = liveData
    }

    private fun animDownIcon(expand: Boolean) {
        val shouldAnim = if (expand) !expanded else expanded
        if (!shouldAnim) return

        binding.downIv.animate().apply {
            interpolator = BounceInterpolator()
        }.scaleX(if (expand) 1.5f else 1f).start()
        binding.downIv.animate().apply {
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
        val viewBinding = ViewConversationBottomBinding.inflate(LayoutInflater.from(ContextThemeWrapper(requireActivity(), R.style.Custom)), null, false)
        builder.setCustomView(viewBinding.root)
        viewBinding.muteTv.setText(
            if (isMute) {
                R.string.Unmute
            } else {
                R.string.Mute
            }
        )
        val bottomSheet = builder.create()
        viewBinding.muteTv.setOnClickListener {
            if (isMute) {
                unMute(conversationItem)
            } else {
                showMuteDialog(conversationItem)
            }
            bottomSheet.dismiss()
        }
        viewBinding.deleteTv.setOnClickListener {
            alertDialogBuilder()
                .setTitle(getString(R.string.conversation_delete_title, conversationItem.getConversationName()))
                .setMessage(getString(R.string.conversation_delete_tip))
                .setNegativeButton(R.string.Cancel) { dialog, _ ->
                    dialog.dismiss()
                    bottomSheet.dismiss()
                }
                .setPositiveButton(R.string.Confirm) { _, _ ->
                    val lm = binding.messageRv.layoutManager as LinearLayoutManager
                    val lastCompleteVisibleItem = lm.findLastCompletelyVisibleItemPosition()
                    val firstCompleteVisibleItem = lm.findFirstCompletelyVisibleItemPosition()
                    if (lastCompleteVisibleItem - firstCompleteVisibleItem <= messageAdapter.itemCount &&
                        lm.findFirstVisibleItemPosition() == 0
                    ) {
                        binding.shadowFl.animate().translationY(0f).duration = 200
                    }
                    conversationListViewModel.deleteConversation(conversationId)
                    bottomSheet.dismiss()
                }
                .show()
        }
        if (hasPin) {
            viewBinding.pinTv.setText(R.string.Unpin)
            viewBinding.pinTv.setOnClickListener {
                conversationListViewModel.updateConversationPinTimeById(conversationId, circleId, null)
                bottomSheet.dismiss()
            }
        } else {
            viewBinding.pinTv.setText(R.string.pin_title)
            viewBinding.pinTv.setOnClickListener {
                conversationListViewModel.updateConversationPinTimeById(
                    conversationId,
                    circleId,
                    nowInUtc()
                )
                bottomSheet.dismiss()
            }
        }

        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            val totalUsd = conversationListViewModel.findTotalUSDBalance()

            val shown = bulletinBoard
                .addBulletin(NewWalletBulletin(bulletinView, requireActivity() as MainActivity, ::onClose))
                .addBulletin(NotificationBulletin(bulletinView, ::onClose))
                .addBulletin(EmergencyContactBulletin(bulletinView, totalUsd >= 100, ::onClose))
                .post()
            messageAdapter.setShowHeader(shown, binding.messageRv)
        }
    }

    private fun onClose(type: BulletinView.Type) {
        val shown = if (type.ordinal < BulletinView.Type.values().size - 1) {
            bulletinBoard.post()
        } else false
        messageAdapter.setShowHeader(shown, binding.messageRv)
    }

    private fun refreshBot() {
        lifecycleScope.launch {
            binding.shadowView.firstIv.isGone = true
            binding.shadowView.secondIv.isGone = true
            binding.shadowView.thirdIv.isGone = true
            requireContext().defaultSharedPreferences.getString(TOP_BOT, DefaultTopBots)?.let {
                val bots = GsonHelper.customGson.fromJson(it, Array<String>::class.java)
                bots.forEachIndexed { index, id ->
                    if (index > 2) return@launch
                    val view: ImageView =
                        when (index) {
                            0 -> {
                                binding.shadowView.firstIv
                            }
                            1 -> {
                                binding.shadowView.secondIv
                            }
                            else -> {
                                binding.shadowView.thirdIv
                            }
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
                            conversationListViewModel.findAppById(id)?.notNullWithElse(
                                { app ->
                                    view.isVisible = true
                                    view.setImageResource(app.getCategoryIcon())
                                    view.setOnClickListener {
                                        WebActivity.show(requireContext(), app.homeUri, null, app)
                                    }
                                },
                                {
                                    view.isInvisible = true
                                }
                            )
                        }
                    }
                }
                if (bots.size < 3) {
                    val dp88 = 88.dp
                    val dp32 = 32.dp
                    binding.shadowView.children.forEach { v ->
                        v.updateLayoutParams<LinearLayoutCompat.LayoutParams> {
                            width = dp88
                            height = dp88
                        }
                        v.setPadding(dp32)
                    }
                } else {
                    val dp80 = 80.dp
                    val dp28 = 28.dp
                    binding.shadowView.children.forEach { v ->
                        v.updateLayoutParams<LinearLayoutCompat.LayoutParams> {
                            width = dp80
                            height = dp80
                        }
                        v.setPadding(dp28)
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
                ItemListConversationBinding.inflate(
                    LayoutInflater.from(parent.context),
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

    class MessageHolder constructor(val binding: ItemListConversationBinding) :
        NormalHolder(binding.root) {
        var context: Context = itemView.context
        private fun getText(id: Int) = context.getText(id).toString()

        @SuppressLint("SetTextI18n")
        fun bind(
            onItemClickListener: PagedHeaderAdapter.OnItemListener<ConversationItem>?,
            conversationItem: ConversationItem
        ) {
            val id = Session.getAccountId()
            conversationItem.getConversationName().let {
                binding.nameTv.text = it
            }
            binding.groupNameTv.visibility = GONE
            binding.msgExpire.isVisible = conversationItem.isExpire()
            binding.mentionFlag.isVisible =
                conversationItem.mentionCount != null && conversationItem.mentionCount > 0
            when {
                conversationItem.messageStatus == MessageStatus.FAILED.name -> {
                    conversationItem.content?.let {
                        setConversationName(conversationItem)
                        binding.msgTv.setText(
                            if (conversationItem.isSignal()) {
                                R.string.Waiting_for_this_message
                            } else {
                                R.string.chat_decryption_failed
                            }
                        )
                    }
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_fail)
                }
                conversationItem.messageStatus == MessageStatus.UNKNOWN.name -> {
                    conversationItem.content?.let {
                        conversationItem.content.let {
                            setConversationName(conversationItem)
                            binding.msgTv.setText(R.string.message_not_support)
                        }
                    }
                    null
                }
                conversationItem.isText() -> {
                    conversationItem.content?.let {
                        setConversationName(conversationItem)
                        if (conversationItem.mentions != null) {
                            binding.msgTv.renderMessage(
                                it,
                                MentionRenderCache.singleton.getMentionRenderContext(
                                    conversationItem.mentions
                                )
                            )
                        } else {
                            binding.msgTv.text = it
                        }
                    }
                    null
                }
                conversationItem.contentType == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> {
                    binding.msgTv.setText(R.string.content_transfer)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_transfer)
                }
                conversationItem.isSticker() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_sticker)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_stiker)
                }
                conversationItem.isImage() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_photo)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_pic)
                }
                conversationItem.isVideo() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_video)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_video)
                }
                conversationItem.isLive() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_live)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_live)
                }
                conversationItem.isData() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_file)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_file)
                }
                conversationItem.isPost() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.text = MarkwonUtil.parseContent(conversationItem.content)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_file)
                }
                conversationItem.isTranscript() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_transcript)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_transcript)
                }
                conversationItem.isLocation() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_location)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_location)
                }
                conversationItem.isAudio() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_audio)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_audio)
                }
                conversationItem.contentType == MessageCategory.APP_BUTTON_GROUP.name -> {
                    binding.groupNameTv.visibility = GONE
                    val buttons =
                        Gson().fromJson(conversationItem.content, Array<AppButtonData>::class.java)
                    var content = ""
                    buttons.map { content += "[" + it.label + "]" }
                    binding.msgTv.text = content
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_touch_app)
                }
                conversationItem.contentType == MessageCategory.APP_CARD.name -> {
                    binding.groupNameTv.visibility = GONE
                    val cardData =
                        Gson().fromJson(conversationItem.content, AppCardData::class.java)
                    binding.msgTv.text = "[${cardData.title}]"
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_touch_app)
                }
                conversationItem.isContact() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_contact)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_contact)
                }
                conversationItem.isCallMessage() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_voice)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_voice)
                }
                conversationItem.isRecall() -> {
                    setConversationName(conversationItem)
                    if (id == conversationItem.senderId) {
                        binding.msgTv.setText(R.string.You_deleted_this_message)
                    } else {
                        binding.msgTv.text =
                            itemView.context.getString(R.string.This_message_was_deleted)
                    }
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_recall)
                }
                conversationItem.isGroupCall() -> {
                    setConversationName(conversationItem)
                    binding.msgTv.setText(R.string.content_group_call)
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_voice)
                }
                conversationItem.contentType == MessageCategory.MESSAGE_PIN.name -> {
                    val pinMessage = try {
                        GsonHelper.customGson.fromJson(
                            conversationItem.content,
                            PinMessageMinimal::class.java
                        )
                    } catch (e: Exception) {
                        null
                    }
                    if (conversationItem.mentions != null) {
                        binding.msgTv.renderMessage(
                            String.format(
                                getText(R.string.chat_pin_message),
                                if (Session.getAccountId() == conversationItem.participantUserId) {
                                    getText(R.string.You)
                                } else {
                                    conversationItem.senderFullName
                                },
                                pinMessage?.let { msg ->
                                    " \"${msg.content}\""
                                } ?: getText(R.string.a_message)
                            ),
                            MentionRenderCache.singleton.getMentionRenderContext(
                                conversationItem.mentions
                            )
                        )
                    } else {
                        binding.msgTv.text = String.format(
                            getText(R.string.chat_pin_message),
                            if (id == conversationItem.senderId) {
                                getText(R.string.You)
                            } else {
                                conversationItem.senderFullName
                            },
                            pinMessage.explain(itemView.context)
                        )
                    }
                    null
                }
                conversationItem.contentType == MessageCategory.SYSTEM_CONVERSATION.name -> {
                    when (conversationItem.actionName) {
                        SystemConversationAction.CREATE.name -> {
                            binding.msgTv.text =
                                String.format(
                                    getText(R.string.created_this_group),
                                    if (id == conversationItem.senderId) {
                                        getText(R.string.You)
                                    } else {
                                        conversationItem.name
                                    }
                                )
                        }
                        SystemConversationAction.ADD.name -> {
                            binding.msgTv.text =
                                String.format(
                                    getText(R.string.chat_group_add),
                                    if (id == conversationItem.senderId) {
                                        getText(R.string.You)
                                    } else {
                                        conversationItem.senderFullName
                                    },
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.you)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.REMOVE.name -> {
                            binding.msgTv.text =
                                String.format(
                                    getText(R.string.chat_group_remove),
                                    if (id == conversationItem.senderId) {
                                        getText(R.string.You)
                                    } else {
                                        conversationItem.senderFullName
                                    },
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.you)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.JOIN.name -> {
                            binding.msgTv.text =
                                String.format(
                                    getText(R.string.chat_group_join),
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.You)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.EXIT.name -> {
                            binding.msgTv.text =
                                String.format(
                                    getText(R.string.chat_group_exit),
                                    if (id == conversationItem.participantUserId) {
                                        getText(R.string.You)
                                    } else {
                                        conversationItem.participantFullName
                                    }
                                )
                        }
                        SystemConversationAction.ROLE.name -> {
                            binding.msgTv.text = getText(R.string.group_role)
                        }
                        SystemConversationAction.EXPIRE.name -> {
                            val timeInterval = conversationItem.content?.toLongOrNull()
                            val name = if (id == conversationItem.senderId) {
                                getText(R.string.You)
                            } else {
                                conversationItem.senderFullName
                            }
                            binding.msgTv.text =
                                when {
                                    timeInterval == null -> {
                                        String.format(
                                            getText(R.string.changed_disappearing_message_settings), name
                                        )
                                    }
                                    timeInterval <= 0 -> {
                                        String.format(
                                            getText(R.string.disable_disappearing_message), name
                                        )
                                    }
                                    else -> {
                                        String.format(
                                            getText(R.string.set_disappearing_message_time_to),
                                            name,
                                            toTimeInterval(timeInterval)
                                        )
                                    }
                                }
                        }
                        else -> {
                            binding.msgTv.text = ""
                        }
                    }
                    null
                }
                else -> {
                    binding.msgTv.text = ""
                    null
                }
            }.also {
                it.notNullWithElse(
                    { drawable ->
                        drawable.setBounds(
                            0,
                            0,
                            itemView.context.dpToPx(12f),
                            itemView.context.dpToPx(12f)
                        )
                        binding.msgType.setImageDrawable(drawable)
                        binding.msgType.isVisible = true
                    },
                    {
                        binding.msgType.isVisible = false
                    }
                )
            }

            if (conversationItem.senderId == Session.getAccountId() &&
                conversationItem.contentType != MessageCategory.SYSTEM_CONVERSATION.name &&
                conversationItem.contentType != MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name &&
                conversationItem.messageStatus != MessageStatus.FAILED.name &&
                !conversationItem.isCallMessage() && !conversationItem.isRecall() &&
                !conversationItem.isGroupCall() &&
                !conversationItem.isPin()
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
                        AppCompatResources.getDrawable(
                            itemView.context,
                            R.drawable.ic_status_sending
                        )
                    }
                }.also {
                    it?.setBounds(0, 0, itemView.context.dpToPx(14f), itemView.context.dpToPx(14f))
                    binding.msgStatus.setImageDrawable(it)
                    binding.msgStatus.visibility = VISIBLE
                    if (conversationItem.messageStatus == MessageStatus.SENDING.name) {
                        (it as Animatable).start()
                    }
                }
            } else {
                binding.msgStatus.visibility = GONE
            }
            conversationItem.createdAt?.let {
                binding.timeTv.timeAgo(it)
            }
            if (conversationItem.pinTime == null) {
                binding.msgPin.visibility = GONE
                if (conversationItem.isGroupConversation() && conversationItem.status == ConversationStatus.START.ordinal) {
                    binding.pb.visibility = VISIBLE
                    binding.unreadTv.visibility = GONE
                } else {
                    binding.pb.visibility = GONE
                    conversationItem.unseenMessageCount.notEmptyWithElse(
                        {
                            binding.unreadTv.text = "$it"; binding.unreadTv.visibility = VISIBLE
                        },
                        { binding.unreadTv.visibility = GONE }
                    )

                    if (conversationItem.isGroupConversation() && conversationItem.status == ConversationStatus.FAILURE.ordinal) {
                        binding.msgTv.text = getText(R.string.group_click_create_tip)
                    }
                }
            } else {
                binding.msgPin.visibility = VISIBLE
                if (conversationItem.isGroupConversation() && conversationItem.status == ConversationStatus.START.ordinal) {
                    binding.pb.visibility = VISIBLE
                    binding.unreadTv.visibility = GONE
                } else {
                    binding.pb.visibility = GONE
                    conversationItem.unseenMessageCount.notEmptyWithElse(
                        {
                            binding.unreadTv.text = "$it"; binding.unreadTv.visibility =
                                VISIBLE
                        },
                        { binding.unreadTv.visibility = GONE }
                    )
                }
            }

            binding.muteIv.visibility = if (conversationItem.isMute()) VISIBLE else GONE
            if (conversationItem.isMute()) {
                binding.unreadTv.setBackgroundResource(R.drawable.bg_unread_mute)
                binding.unreadTv.setTextColor(context.colorFromAttribute(R.attr.badger_text_mute))
            } else {
                binding.unreadTv.setBackgroundResource(R.drawable.bg_unread)
                binding.unreadTv.setTextColor(context.colorFromAttribute(R.attr.badger_text))
            }

            conversationItem.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
            if (conversationItem.isGroupConversation()) {
                binding.avatarIv.setGroup(conversationItem.iconUrl())
            } else {
                binding.avatarIv.setInfo(
                    conversationItem.getConversationName(),
                    conversationItem.iconUrl(),
                    conversationItem.ownerId
                )
            }
            itemView.setOnClickListener { onItemClickListener?.onNormalItemClick(conversationItem) }
            itemView.setOnLongClickListener {
                onItemClickListener.notNullWithElse(
                    { it.onNormalLongClick(conversationItem) },
                    false
                )
            }
        }

        @SuppressLint("SetTextI18n")
        private fun setConversationName(conversationItem: ConversationItem) {
            if (conversationItem.isGroupConversation() && conversationItem.senderId != Session.getAccountId()) {
                binding.groupNameTv.text = "${conversationItem.senderFullName}: "
                binding.groupNameTv.visibility = VISIBLE
            } else {
                binding.groupNameTv.visibility = GONE
            }
        }
    }

    private fun showMuteDialog(conversationItem: ConversationItem) {
        val choices = arrayOf(
            getString(R.string.one_hour),
            resources.getQuantityString(R.plurals.Hour, 8, 8),
            getString(R.string.one_week),
            getString(R.string.one_year)
        )
        var duration = MUTE_8_HOURS
        var whichItem = 0
        alertDialogBuilder()
            .setTitle(getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                if (conversationItem.isGroupConversation()) {
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                conversationListViewModel.mute(
                                    duration.toLong(),
                                    conversationId = conversationItem.conversationId
                                )
                            },
                            successBlock = { response ->
                                conversationListViewModel.updateGroupMuteUntil(
                                    conversationItem.conversationId,
                                    response.data!!.muteUntil
                                )
                                toast(getString(R.string.contact_mute_title) + " ${conversationItem.groupName} " + choices[whichItem])
                            }
                        )
                    }
                } else {
                    val account = Session.getAccount()
                    account?.let {
                        lifecycleScope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    conversationListViewModel.mute(
                                        duration.toLong(),
                                        senderId = it.userId,
                                        recipientId = conversationItem.ownerId
                                    )
                                },
                                successBlock = { response ->
                                    conversationListViewModel.updateMuteUntil(
                                        conversationItem.ownerId,
                                        response.data!!.muteUntil
                                    )
                                    toast(getString(R.string.contact_mute_title) + "  ${conversationItem.name}  " + choices[whichItem])
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
        if (conversationItem.isGroupConversation()) {
            lifecycleScope.launch {
                handleMixinResponse(
                    invokeNetwork = {
                        conversationListViewModel.mute(0, conversationId = conversationItem.conversationId)
                    },
                    successBlock = { response ->
                        conversationListViewModel.updateGroupMuteUntil(
                            conversationItem.conversationId,
                            response.data!!.muteUntil
                        )
                        toast(getString(R.string.Unmute) + " ${conversationItem.groupName}")
                    }
                )
            }
        } else {
            Session.getAccount()?.let {
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            conversationListViewModel.mute(
                                0,
                                senderId = it.userId,
                                recipientId = conversationItem.ownerId
                            )
                        },
                        successBlock = { response ->
                            conversationListViewModel.updateMuteUntil(
                                conversationItem.ownerId,
                                response.data!!.muteUntil
                            )
                            toast(getString(R.string.Unmute) + " ${conversationItem.name}")
                        }
                    )
                }
            }
        }
    }
}

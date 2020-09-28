package one.mixin.android.ui.forward

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bugsnag.android.Bugsnag
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions2.RxPermissions
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_forward.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ForwardEvent
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment.Companion.CATEGORY
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment.Companion.CONTENT
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_FROM_CONVERSATION
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_MESSAGES
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_SHARE
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.*
import one.mixin.android.vo.*
import one.mixin.android.webrtc.SelectItem
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ForwardFragment : BaseFragment() {
    companion object {
        const val TAG = "ForwardFragment"

        fun newInstance(
            messages: ArrayList<ForwardMessage>? = null,
            isShare: Boolean = false,
            fromConversation: Boolean = false,
            category: String? = null,
            content: String? = null,
            conversationId: String? = null
        ): ForwardFragment {
            val fragment = ForwardFragment()
            val b = bundleOf(
                ARGS_MESSAGES to messages,
                ARGS_SHARE to isShare,
                ARGS_FROM_CONVERSATION to fromConversation,
                CATEGORY to category,
                CONTENT to content,
                ARGS_CONVERSATION_ID to conversationId
            )
            fragment.arguments = b
            return fragment
        }
    }

    private val chatViewModel by viewModels<ConversationViewModel>()

    private val adapter by lazy {
        ForwardAdapter()
    }

    private val messages: ArrayList<ForwardMessage>? by lazy {
        requireArguments().getParcelableArrayList(ARGS_MESSAGES)
    }
    private val isShare: Boolean by lazy {
        requireArguments().getBoolean(ARGS_SHARE)
    }
    private val fromConversation: Boolean by lazy {
        requireArguments().getBoolean(ARGS_FROM_CONVERSATION)
    }
    private val conversationId: String? by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_forward, container, false)

    private fun setForwardText() {
        if (adapter.selectItem.size > 0) {
            forward_group.visibility = View.VISIBLE
        } else {
            forward_group.visibility = View.GONE
        }
        val str = StringBuffer()
        for (i in adapter.selectItem.size - 1 downTo 0) {
            adapter.selectItem[i].let {
                if (it is ConversationItem) {
                    if (it.isGroup()) {
                        str.append(it.groupName)
                    } else {
                        str.append(it.name)
                    }
                    if (i != 0) {
                        str.append(getString(R.string.divide))
                    }
                } else if (it is User) {
                    str.append(it.fullName)
                    if (i != 0) {
                        str.append(getString(R.string.divide))
                    }
                }
            }
        }
        forward_tv.text = str
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (conversationId != null) {
            sendDirectMessages(conversationId!!)
            return
        }
        if (isShare) {
            title_view.title_tv.text = getString(R.string.share)
        } else if (messages == null) {
            title_view.title_tv.text = getString(R.string.send)
        }
        title_view.setOnClickListener {
            search_et?.hideKeyboard()
            activity?.onBackPressed()
        }
        forward_rv.adapter = adapter
        forward_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        forward_bn.setOnClickListener {
            search_et?.hideKeyboard()
            updateDynamicShortcuts(adapter.selectItem)
            if (messages == null) {
                val resultData = adapter.selectItem.mapNotNull {
                    when (it) {
                        is ConversationItem -> {
                            SelectItem(it.conversationId, null)
                        }
                        is User -> {
                            SelectItem(null, it.userId)
                        }
                        else -> {
                            null
                        }
                    }
                }

                if (arguments?.getString(CONTENT) != null) {
                    sendMessage(resultData)
                    return@setOnClickListener
                }
                val result = Intent().apply {
                    putParcelableArrayListExtra(ForwardActivity.ARGS_RESULT, ArrayList(resultData))
                }
                requireActivity().setResult(Activity.RESULT_OK, result)
                requireActivity().finish()
            } else {
                sendMessages(adapter.selectItem.size == 1)
                toast(R.string.message_sent)
            }
        }
        adapter.setForwardListener(
            object : ForwardAdapter.ForwardListener {
                override fun onConversationItemClick(item: ConversationItem) {
                    if (adapter.selectItem.contains(item)) {
                        adapter.selectItem.remove(item)
                    } else {
                        adapter.selectItem.add(item)
                    }
                    setForwardText()
                }

                override fun onUserItemClick(user: User) {
                    if (adapter.selectItem.contains(user)) {
                        adapter.selectItem.remove(user)
                    } else {
                        adapter.selectItem.add(user)
                    }
                    setForwardText()
                }
            }
        )
        search_et.addTextChangedListener(mWatcher)

        loadData()
    }

    private fun loadData() = lifecycleScope.launch {
        val conversations = chatViewModel.successConversationList()
        adapter.sourceConversations = conversations
        val set = ArraySet<String>()
        conversations.forEach { item ->
            if (item.isContact()) {
                set.add(item.ownerId)
            }
        }

        val friends = mutableListOf<User>()
        val bots = mutableListOf<User>()
        chatViewModel.getFriends().filter { item ->
            !set.contains(item.userId)
        }.forEach {
            if (it.isBot()) {
                bots.add(it)
            } else {
                friends.add(it)
            }
        }
        adapter.sourceFriends = friends
        adapter.sourceBots = bots

        adapter.changeData()
    }

    private fun sendMessage(selectItem: List<SelectItem>) {
        lifecycleScope.launch {
            val sender = Session.getAccount()?.toUser() ?: return@launch
            val content = requireNotNull(arguments?.getString(CONTENT)) { "Error data" }
            val category = requireNotNull(arguments?.getString(CATEGORY)) { "Error data" }
            selectItem.forEach { item ->
                chatViewModel.checkData(item) { conversationId: String, isPlain: Boolean ->
                    when (category) {
                        Constants.ShareCategory.TEXT -> {
                            chatViewModel.sendTextMessage(conversationId, sender, content, isPlain)
                        }
                        Constants.ShareCategory.IMAGE -> {
                            withContext(Dispatchers.IO) {
                                val shareImageData = GsonHelper.customGson.fromJson(content, ShareImageData::class.java)
                                val file: File = Glide.with(requireContext()).asFile().load(shareImageData.url).submit().get()
                                chatViewModel.sendImageMessage(conversationId, sender, file.toUri(), isPlain)
                            }?.autoDispose(stopScope)?.subscribe(
                                {
                                    when (it) {
                                        0 -> {
                                        }
                                        -1 -> context?.toast(R.string.error_image)
                                        -2 -> context?.toast(R.string.error_format)
                                    }
                                },
                                {
                                    context?.toast(R.string.error_image)
                                }
                            )
                        }
                        Constants.ShareCategory.CONTACT -> {
                            val contactData = GsonHelper.customGson.fromJson(content, ContactMessagePayload::class.java)
                            chatViewModel.sendContactMessage(conversationId, sender, contactData.userId, isPlain)
                        }
                        Constants.ShareCategory.POST -> {
                            chatViewModel.sendPostMessage(conversationId, sender, content, isPlain)
                        }
                        Constants.ShareCategory.APP_CARD -> {
                            chatViewModel.sendAppCardMessage(conversationId, sender, content)
                        }
                        Constants.ShareCategory.LIVE -> {
                            val liveData = GsonHelper.customGson.fromJson(content, LiveMessagePayload::class.java)
                            chatViewModel.sendLiveMessage(conversationId, sender, liveData, isPlain)
                        }
                    }
                }
            }
            requireActivity().finish()
        }
    }

    private fun sendDirectMessages(cid: String) {
        chatViewModel.sendForwardMessages(cid, messages, isPlainMessage = false, showSuccess = false)
        MainActivity.reopen(requireContext())
        activity?.finish()
        ConversationActivity.show(requireContext(), conversationId)
    }

    private fun sendMessages(single: Boolean) {
        if (messages?.find { it.type == ForwardCategory.VIDEO.name || it.type == ForwardCategory.IMAGE.name || it.type == ForwardCategory.DATA.name } != null) {
            RxPermissions(requireActivity())
                .request(
                    WRITE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE
                )
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            sharePreOperation(single)
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    },
                    {
                        Bugsnag.notify(it)
                    }
                )
        } else {
            sharePreOperation(single)
        }
    }

    private fun sharePreOperation(single: Boolean) {
        chatViewModel.sendForwardMessages(adapter.selectItem, messages, !isShare && !fromConversation)
        val forwardEvent = adapter.selectItem[0].let {
            if (it is User) {
                ForwardEvent(null, it.userId)
            } else {
                it as ConversationItem
                ForwardEvent(it.conversationId, null)
            }
        }
        if (isShare) {
            MainActivity.reopen(requireContext())
            activity?.finish()
            ConversationActivity.show(requireContext(), forwardEvent.conversationId, forwardEvent.userId)
        } else {
            activity?.finish()
            if (fromConversation && single) {
                RxBus.publish(forwardEvent)
            }
        }
    }

    private fun updateDynamicShortcuts(selectItems: ArrayList<Any>) = lifecycleScope.launch {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()
        for (i in 0 until selectItems.size) {
            val s = selectItems[i]
            if (shortcuts.size >= maxDynamicShortcutCount) {
                break
            }

            val shortcutInfo = if (s is ConversationItem) {
                val bitmap = loadBitmap(s.iconUrl()) ?: continue
                val intent = ConversationActivity.getShortcutIntent(
                    requireContext(),
                    s.conversationId,
                    null
                )
                ShortcutInfo(s.conversationId, s.getConversationName(), bitmap, intent)
            } else {
                s as User
                val bitmap = loadBitmap(s.avatarUrl) ?: continue
                val cid = generateConversationId(
                    Session.getAccountId()!!,
                    s.userId
                )
                val intent = ConversationActivity.getShortcutIntent(
                    requireContext(),
                    cid,
                    s.userId
                )
                ShortcutInfo(cid, s.fullName ?: "", bitmap, intent)
            }
            shortcuts.add(generateDynamicShortcut(requireContext(), shortcutInfo))
        }
        ShortcutManagerCompat.addDynamicShortcuts(requireContext(), shortcuts)
    }

    private suspend fun loadBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null

        return try {
            withContext(Dispatchers.IO) {
                Glide.with(requireContext())
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get(2, TimeUnit.SECONDS)
            }
        } catch (t: Throwable) {
            null
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            adapter.keyword = s
            adapter.changeData()
        }
    }
}

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
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions2.RxPermissions
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.databinding.FragmentForwardBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.within24Hours
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_ACTION
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_COMBINE_MESSAGES
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_MESSAGES
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.ShortcutInfo
import one.mixin.android.util.generateDynamicShortcut
import one.mixin.android.util.maxDynamicShortcutCount
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AttachmentExtra
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.isContactConversation
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.SelectItem
import one.mixin.android.websocket.AttachmentMessagePayload
import one.mixin.android.websocket.AudioMessagePayload
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.DataMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.websocket.VideoMessagePayload
import java.io.File
import java.lang.Exception
import java.util.UUID
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ForwardFragment : BaseFragment(R.layout.fragment_forward) {
    companion object {
        const val TAG = "ForwardFragment"

        fun newInstance(
            messages: ArrayList<ForwardMessage>,
            action: ForwardAction
        ): ForwardFragment {
            val fragment = ForwardFragment()
            val b = bundleOf(
                ARGS_MESSAGES to messages,
                ARGS_ACTION to action
            )
            fragment.arguments = b
            return fragment
        }

        fun newCombineInstance(
            messages: ArrayList<TranscriptMessage>,
            action: ForwardAction
        ): ForwardFragment {
            val fragment = ForwardFragment()
            val b = bundleOf(
                ARGS_COMBINE_MESSAGES to messages,
                ARGS_ACTION to action
            )
            fragment.arguments = b
            return fragment
        }
    }

    private val chatViewModel by viewModels<ConversationViewModel>()

    private val adapter by lazy {
        ForwardAdapter()
    }

    private val messages: ArrayList<ForwardMessage> by lazy {
        requireNotNull(requireArguments().getParcelableArrayList(ARGS_MESSAGES))
    }

    private val combineMessages: ArrayList<TranscriptMessage> by lazy {
        requireNotNull(requireArguments().getParcelableArrayList(ARGS_COMBINE_MESSAGES))
    }

    private val action: ForwardAction by lazy {
        requireNotNull(requireArguments().getParcelable(ARGS_ACTION))
    }

    private val sender = Session.getAccount()?.toUser()

    private val binding by viewBinding(FragmentForwardBinding::bind)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_forward, container, false)

    private fun setForwardText() {
        if (adapter.selectItem.size > 0) {
            binding.forwardGroup.visibility = View.VISIBLE
        } else {
            binding.forwardGroup.visibility = View.GONE
        }
        val str = StringBuffer()
        for (i in adapter.selectItem.size - 1 downTo 0) {
            adapter.selectItem[i].let {
                if (it is ConversationMinimal) {
                    if (it.isGroupConversation()) {
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
        binding.forwardTv.text = str
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cid = action.conversationId
        if (cid != null) {
            when (action) {
                is ForwardAction.System -> {
                    sendDirectMessages(cid)
                }
                else -> {
                    sendMessage(listOf(SelectItem(cid, null)))
                }
            }
            requireActivity().finish()
            return
        }

        if (!action.name.isNullOrBlank()) {
            binding.titleView.titleTv.text = action.name
        }
        binding.titleView.setOnClickListener {
            binding.searchEt.hideKeyboard()
            activity?.onBackPressed()
        }
        binding.forwardRv.adapter = adapter
        binding.forwardRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        binding.forwardBn.setOnClickListener {
            binding.searchEt.hideKeyboard()
            checkPermission {
                updateDynamicShortcuts(adapter.selectItem)

                val selectItem = adapter.selectItem.mapNotNull {
                    when (it) {
                        is ConversationMinimal -> {
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
                if (action is ForwardAction.Combine) {
                    sendCombineMessage(selectItem)
                } else {
                    sendMessage(selectItem)
                }
            }
        }
        adapter.setForwardListener(
            object : ForwardAdapter.ForwardListener {
                override fun onConversationClick(item: ConversationMinimal) {
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
        binding.searchEt.addTextChangedListener(mWatcher)

        loadData()
    }

    private fun loadData() = lifecycleScope.launch {
        val conversations = chatViewModel.successConversationList()
        adapter.sourceConversations = conversations
        val set = ArraySet<String>()
        conversations.forEach { item ->
            if (item.isContactConversation()) {
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

    private fun sendMessage(selectItems: List<SelectItem>) = lifecycleScope.launch {
        val errList = arrayListOf<String>()
        selectItems.forEach { item ->
            val err = sendMessageInternal(item)
            if (err != null) {
                errList.addAll(err)
            }
        }
        if (errList.isEmpty()) {
            toast(R.string.message_sent)
        }
        if (action is ForwardAction.System) {
            MainActivity.reopen(requireContext())
            requireActivity().finish()
            val firstItem = selectItems[0]
            if (firstItem.conversationId != null) {
                ConversationActivity.show(requireContext(), conversationId = firstItem.conversationId)
            } else {
                ConversationActivity.show(requireContext(), recipientId = firstItem.userId)
            }
        } else {
            if (action is ForwardAction.App.Resultful) {
                val result = Intent().apply {
                    putParcelableArrayListExtra(ForwardActivity.ARGS_RESULT, ArrayList(selectItems))
                }
                requireActivity().setResult(Activity.RESULT_OK, result)
            }
            requireActivity().finish()
        }
    }

    private fun sendCombineMessage(selectItems: List<SelectItem>) = lifecycleScope.launch {
        if (sender == null) return@launch
        selectItems.forEach { item ->
            chatViewModel.checkData(item) { conversationId: String, isPlain: Boolean ->
                val transcripts = chatViewModel.processTranscript(conversationId, combineMessages)
                val messageId = transcripts[0].transcriptId
                chatViewModel.sendTranscriptMessage(conversationId, messageId, sender, transcripts, isPlain)
            }
        }
        val result = Intent().apply {
            putParcelableArrayListExtra(ForwardActivity.ARGS_RESULT, ArrayList(selectItems))
        }
        requireActivity().setResult(Activity.RESULT_OK, result)
        requireActivity().finish()
    }

    private fun checkPermission(afterGranted: () -> Job) {
        RxPermissions(requireActivity())
            .request(
                WRITE_EXTERNAL_STORAGE,
                READ_EXTERNAL_STORAGE
            )
            .autoDispose(stopScope)
            .subscribe(
                { granted ->
                    if (granted) {
                        afterGranted.invoke()
                    } else {
                        requireContext().openPermissionSetting()
                    }
                },
                {}
            )
    }

    private suspend fun sendMessageInternal(item: SelectItem): ArrayList<String>? {
        if (sender == null) return null

        val errList = arrayListOf<String>()
        messages.forEach { m ->
            chatViewModel.checkData(item) { conversationId: String, isPlain: Boolean ->
                val content = m.content
                var errorString: String? = null
                when (m.category) {
                    ShareCategory.Text -> {
                        chatViewModel.sendTextMessage(conversationId, sender, content, isPlain)
                    }
                    ShareCategory.Post -> {
                        chatViewModel.sendPostMessage(conversationId, sender, content, isPlain)
                    }
                    ShareCategory.Image -> {
                        val shareImageData = GsonHelper.customGson.fromJson(content, ShareImageData::class.java)
                        sendAttachmentMessage(
                            conversationId, sender, shareImageData.url, shareImageData.attachmentExtra,
                            {
                                if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
                            },
                            {
                                val code = if (shareImageData.url.startsWith("http", true)) {
                                    val file: File? = try {
                                        Glide.with(requireContext()).asFile().load(shareImageData.url).submit().get()
                                    } catch (t: Throwable) {
                                        null
                                    }
                                    if (file == null) {
                                        chatViewModel.sendImageMessage(conversationId, sender, shareImageData.url.toUri(), isPlain)
                                    } else {
                                        chatViewModel.sendImageMessage(conversationId, sender, file.toUri(), isPlain)
                                    }
                                } else {
                                    chatViewModel.sendImageMessage(conversationId, sender, shareImageData.url.toUri(), isPlain)
                                }
                                val errorRes = ShareCategory.Image.getErrorStringOrNull(code)
                                if (errorRes != null) {
                                    withContext(Dispatchers.Main) {
                                        errorString = requireContext().getString(errorRes)
                                        toast(errorString!!)
                                    }
                                }
                            }
                        )
                    }
                    ShareCategory.Contact -> {
                        val contactData = GsonHelper.customGson.fromJson(content, ContactMessagePayload::class.java)
                        chatViewModel.sendContactMessage(conversationId, sender, contactData.userId, isPlain)
                    }
                    ShareCategory.Contact -> {
                        chatViewModel.sendPostMessage(conversationId, sender, content, isPlain)
                    }
                    ShareCategory.AppCard -> {
                        chatViewModel.sendAppCardMessage(conversationId, sender, content)
                    }
                    ShareCategory.Live -> {
                        val liveData = GsonHelper.customGson.fromJson(content, LiveMessagePayload::class.java)
                        chatViewModel.sendLiveMessage(conversationId, sender, liveData, isPlain)
                    }
                    ForwardCategory.Video -> {
                        val videoData = GsonHelper.customGson.fromJson(content, VideoMessagePayload::class.java)
                        sendAttachmentMessage(
                            conversationId, sender, videoData.url, videoData.attachmentExtra,
                            {
                                if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
                            },
                            {
                                chatViewModel.sendVideoMessage(conversationId, sender.userId, videoData, isPlain)
                            }
                        )
                    }
                    ForwardCategory.Data -> {
                        val dataMessagePayload = GsonHelper.customGson.fromJson(content, DataMessagePayload::class.java)
                        sendAttachmentMessage(
                            conversationId, sender, dataMessagePayload.url, dataMessagePayload.attachmentExtra,
                            {
                                if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
                            },
                            {
                                chatViewModel.sendAttachmentMessage(conversationId, sender, dataMessagePayload.toAttachment(), isPlain)
                            }
                        )
                    }
                    ForwardCategory.Audio -> {
                        val audioData = GsonHelper.customGson.fromJson(content, AudioMessagePayload::class.java)
                        sendAttachmentMessage(
                            conversationId, sender, audioData.url, audioData.attachmentExtra,
                            {
                                if (isPlain) MessageCategory.PLAIN_AUDIO.name else MessageCategory.SIGNAL_AUDIO.name
                            },
                            {
                                chatViewModel.sendAudioMessage(conversationId, sender, audioData, isPlain)
                            }
                        )
                    }
                    ForwardCategory.Sticker -> {
                        val stickerData = GsonHelper.customGson.fromJson(content, StickerMessagePayload::class.java)
                        chatViewModel.sendStickerMessage(conversationId, sender, stickerData, isPlain)
                    }
                    ForwardCategory.Location -> {
                        val locationPayload = GsonHelper.customGson.fromJson(content, LocationPayload::class.java)
                        chatViewModel.sendLocationMessage(conversationId, sender.userId, locationPayload, isPlain)
                    }
                    ForwardCategory.Transcript -> {
                        val transcript = GsonHelper.customGson.fromJson(content, Array<TranscriptMessage>::class.java)
                        val list = chatViewModel.processTranscript(conversationId, transcript.toList())
                        val messageId = transcript[0].messageId
                        chatViewModel.sendTranscriptMessage(conversationId, messageId, sender, list, isPlain)
                    }
                    else -> {
                        throw IllegalArgumentException("Unknown category")
                    }
                }
                if (errorString != null) {
                    errList.add(errorString!!)
                }
            }
        }
        return errList
    }

    private suspend fun sendAttachmentMessage(
        conversationId: String,
        sender: User,
        mediaUrl: String?,
        attachmentExtraString: String?,
        getCategory: () -> String,
        fallbackAction: suspend () -> Unit
    ) = withContext(Dispatchers.IO) {
        if (attachmentExtraString != null) {
            val attachmentExtra: AttachmentExtra = try {
                GsonHelper.customGson.fromJson(attachmentExtraString, AttachmentExtra::class.java)
            } catch (e: Exception) {
                parseAsAttachmentMessagePayload(attachmentExtraString, sender, conversationId, getCategory.invoke(), mediaUrl, fallbackAction)
                return@withContext
            }
            val createdAt = attachmentExtra.createdAt
            val messageId = attachmentExtra.messageId
            if (createdAt == null || messageId == null || !createdAt.within24Hours()) {
                fallbackAction.invoke()
                return@withContext
            }
            val category = getCategory.invoke()
            val message = chatViewModel.findMessageById(messageId)
            if (message == null || category != message.category) {
                fallbackAction.invoke()
                return@withContext
            }
            val newMessage = buildAttachmentMessage(conversationId, sender, category, attachmentExtra.attachmentId, message)
            chatViewModel.sendMessage(newMessage)
        } else {
            fallbackAction.invoke()
        }
    }

    private suspend fun parseAsAttachmentMessagePayload(
        attachmentExtraString: String?,
        sender: User,
        conversationId: String,
        category: String,
        mediaUrl: String?,
        fallbackAction: suspend () -> Unit
    ) {
        val payload: AttachmentMessagePayload = try {
            GsonHelper.customGson.fromJson(String(Base64.decode(attachmentExtraString)), AttachmentMessagePayload::class.java)
        } catch (e: Exception) {
            fallbackAction.invoke()
            return
        }
        // Should be removed when PLAIN message's attachment is encrypted
        if ((category.startsWith("SIGNAL_") && (payload.key == null || payload.digest == null)) ||
            (category.startsWith("PLAIN_") && (payload.key != null && payload.digest != null))
        ) {
            fallbackAction.invoke()
            return
        }
        val createdAt = payload.createdAt
        if (createdAt == null || !createdAt.within24Hours()) {
            fallbackAction.invoke()
            return
        }
        val message = Message(
            UUID.randomUUID().toString(), conversationId, sender.userId, category, GsonHelper.customGson.toJson(payload).base64Encode(), mediaUrl,
            payload.mimeType, payload.size, payload.duration?.toString(), payload.width, payload.height, null, payload.thumbnail, null,
            payload.key, payload.digest, MediaStatus.DONE.name, MessageStatus.SENDING.name, nowInUtc(), name = payload.name, mediaWaveform = payload.waveform,
            caption = payload.caption,
        )
        chatViewModel.sendMessage(message)
    }

    private fun buildAttachmentMessage(conversationId: String, sender: User, category: String, attachmentId: String, message: Message): Message {
        val messageId = UUID.randomUUID().toString()
        val attachmentMessagePayload = AttachmentMessagePayload(
            message.mediaKey, message.mediaDigest, attachmentId, message.mediaMimeType!!, message.mediaSize ?: 0, message.name, message.mediaWidth,
            message.mediaHeight, message.thumbImage, message.mediaDuration?.toLongOrNull(), message.mediaWaveform,
        )
        return Message(
            messageId, conversationId, sender.userId, category,
            GsonHelper.customGson.toJson(attachmentMessagePayload).base64Encode(), message.mediaUrl, message.mediaMimeType,
            message.mediaSize ?: 0L, message.mediaDuration, message.mediaWidth,
            message.mediaHeight, message.mediaHash, message.thumbImage, message.thumbUrl,
            message.mediaKey, message.mediaDigest, MediaStatus.DONE.name, MessageStatus.SENDING.name,
            nowInUtc(), name = message.name, mediaWaveform = message.mediaWaveform,
        )
    }

    private fun sendDirectMessages(cid: String) = lifecycleScope.launch {
        val err = sendMessageInternal(SelectItem(cid, null))
        if (err.isNullOrEmpty()) {
            toast(R.string.message_sent)
        }
        MainActivity.reopen(requireContext())
        activity?.finish()
        ConversationActivity.show(requireContext(), cid)
    }

    private fun updateDynamicShortcuts(selectItems: ArrayList<Any>) = lifecycleScope.launch {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()
        for (i in 0 until selectItems.size) {
            val s = selectItems[i]
            if (shortcuts.size >= maxDynamicShortcutCount) {
                break
            }

            val shortcutInfo = if (s is ConversationMinimal) {
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
        val exists = ShortcutManagerCompat.getDynamicShortcuts(requireContext())
        val keepSize = maxDynamicShortcutCount - shortcuts.size
        if (keepSize >= 0 && exists.size > keepSize) {
            val removeIds = mutableListOf<String>()
            exists.take(exists.size - keepSize)
                .mapTo(removeIds) { it.id }
            ShortcutManagerCompat.removeDynamicShortcuts(requireContext(), removeIds)
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

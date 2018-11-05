package one.mixin.android.job

import androidx.collection.ArrayMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.Session
import one.mixin.android.vo.CallState
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createCallMessage
import one.mixin.android.webrtc.CallService
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.LIST_PENDING_MESSAGES
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executors

class DecryptCallMessage(private val callState: CallState) : Injector() {
    companion object {
        const val LIST_PENDING_CALL_DELAY = 2000L

        var listPendingOfferHandled = false
    }

    private val listPendingDispatcher by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    private val listPendingJobMap = ArrayMap<String, Pair<Job, BlazeMessageData>>()

    fun onRun(data: BlazeMessageData) {
        if (data.category.startsWith("WEBRTC_") && !isExistMessage(data.messageId)) {
            try {
                syncConversation(data)
                processWebRTC(data)
            } catch (e: Exception) {
                Timber.e("DecryptCallMessage failure, $e")
                updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            }
        }
    }

    private fun processWebRTC(data: BlazeMessageData) {
        if (data.source == LIST_PENDING_MESSAGES && data.category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
            val isExpired = try {
                val offset = System.currentTimeMillis() - data.createdAt.createAtToLong()
                offset > CallService.DEFAULT_TIMEOUT_MINUTES * 60 * 1000
            } catch (e: NumberFormatException) {
                true
            }
            if (!isExpired && !listPendingOfferHandled) {
                listPendingJobMap[data.messageId] = Pair(GlobalScope.launch(listPendingDispatcher) {
                    delay(LIST_PENDING_CALL_DELAY)
                    listPendingOfferHandled = true
                    listPendingJobMap.forEach {
                        val pair = it.value
                        val job = pair.first
                        val curData = pair.second
                        if (it.key != data.messageId && !job.isCancelled) {
                            job.cancel()
                            val m = createCallMessage(UUID.randomUUID().toString(), curData.conversationId, Session.getAccountId()!!,
                                MessageCategory.WEBRTC_AUDIO_BUSY.name, null, nowInUtc(), MessageStatus.SENDING, curData.messageId)
                            jobManager.addJobInBackground(SendMessageJob(m))

                            val savedMessage = createCallMessage(curData.messageId, m.conversationId, curData.userId, m.category, m.content,
                                m.createdAt, MessageStatus.DELIVERED, m.quoteMessageId)
                            messageDao.insert(savedMessage)
                        }
                    }
                    processCall(data)
                    listPendingJobMap.clear()

                }, data)
            } else if (isExpired) {
                val message = createCallMessage(data.messageId, data.conversationId, data.userId, MessageCategory.WEBRTC_AUDIO_CANCEL.name,
                    null, data.createdAt, MessageStatus.DELIVERED)
                messageDao.insert(message)
            }
            notifyServer(data)
            return
        }

        processCall(data)
    }

    private fun processCall(data: BlazeMessageData) {
        val ctx = MixinApplication.appContext
        if (data.category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
            val user = userDao.findUser(data.userId)!!
            CallService.startService(ctx, CallService.ACTION_CALL_INCOMING) {
                it.putExtra(Constants.ARGS_USER, user)
                it.putExtra(CallService.EXTRA_BLAZE, data)
            }
        } else {
            val hasUnHandledOffer = listPendingJobMap.containsKey(data.quoteMessageId)
            if (hasUnHandledOffer) {
                listPendingJobMap[data.quoteMessageId]?.let { pair ->
                    pair.first.let {
                        if (!it.isCancelled) {
                            it.cancel()
                        }
                    }
                    listPendingJobMap.remove(data.quoteMessageId)

                    val message = createCallMessage(data.quoteMessageId!!, data.conversationId, data.userId,
                        MessageCategory.WEBRTC_AUDIO_CANCEL.name, null, data.createdAt, MessageStatus.DELIVERED)
                    messageDao.insert(message)
                }
                notifyServer(data)
                return
            } else if (data.quoteMessageId == null || messageDao.findMessageById(data.quoteMessageId) != null) {
                notifyServer(data)
                return
            }

            when (data.category) {
                MessageCategory.WEBRTC_AUDIO_ANSWER.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE ||
                        data.quoteMessageId != callState.callInfo.messageId) {
                        notifyServer(data)
                        return
                    }

                    CallService.startService(ctx, CallService.ACTION_CALL_ANSWER) {
                        it.putExtra(CallService.EXTRA_BLAZE, data)
                    }
                }
                MessageCategory.WEBRTC_ICE_CANDIDATE.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE ||
                        data.quoteMessageId != callState.callInfo.messageId) {
                        notifyServer(data)
                        return
                    }

                    CallService.startService(ctx, CallService.ACTION_CANDIDATE) {
                        it.putExtra(CallService.EXTRA_BLAZE, data)
                    }
                }
                MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        notifyServer(data)
                        return
                    }
                    saveCallMessage(data)
                    if (data.quoteMessageId != callState.callInfo.messageId) {
                        return
                    }
                    CallService.startService(ctx, CallService.ACTION_CALL_CANCEL)
                }
                MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        notifyServer(data)
                        return
                    }

                    val uId = if (callState.callInfo.isInitiator) {
                        Session.getAccountId()!!
                    } else {
                        callState.callInfo.user!!.userId
                    }
                    saveCallMessage(data, userId = uId)
                    if (data.quoteMessageId != callState.callInfo.messageId) {
                        return
                    }
                    CallService.startService(ctx, CallService.ACTION_CALL_DECLINE)
                }
                MessageCategory.WEBRTC_AUDIO_BUSY.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE ||
                        data.quoteMessageId != callState.callInfo.messageId ||
                        callState.callInfo.user == null) {
                        notifyServer(data)
                        return
                    }

                    saveCallMessage(data, userId = Session.getAccountId()!!)
                    CallService.startService(ctx, CallService.ACTION_CALL_BUSY)
                }
                MessageCategory.WEBRTC_AUDIO_END.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        notifyServer(data)
                        return
                    }

                    val duration = System.currentTimeMillis() - callState.callInfo.connectedTime!!
                    val uId = if (callState.callInfo.isInitiator) {
                        Session.getAccountId()!!
                    } else {
                        callState.callInfo.user!!.userId
                    }
                    saveCallMessage(data, duration = duration.toString(), userId = uId, status = MessageStatus.READ)
                    CallService.startService(ctx, CallService.ACTION_CALL_REMOTE_END)
                }
                MessageCategory.WEBRTC_AUDIO_FAILED.name -> {
                    if (callState.callInfo.callState == CallService.CallState.STATE_IDLE) {
                        notifyServer(data)
                        return
                    }

                    val uId = if (callState.callInfo.isInitiator) {
                        Session.getAccountId()!!
                    } else {
                        callState.callInfo.user!!.userId
                    }
                    saveCallMessage(data, userId = uId)
                    CallService.startService(ctx, CallService.ACTION_CALL_REMOTE_FAILED)
                }
            }
        }
        notifyServer(data)
    }

    private fun notifyServer(data: BlazeMessageData) {
        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
        messageHistoryDao.insert(MessageHistory(data.messageId))
    }

    private fun updateRemoteMessageStatus(messageId: String, status: MessageStatus = MessageStatus.DELIVERED) {
        jobDao.insert(createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(messageId, status.name)))
    }

    private fun saveCallMessage(
        data: BlazeMessageData,
        category: String? = null,
        duration: String? = null,
        userId: String = data.userId,
        status: MessageStatus = MessageStatus.DELIVERED) {
        if (data.userId == Session.getAccountId()!! ||
            data.quoteMessageId == null) {
            return
        }
        val realCategory = category ?: data.category
        val message = createCallMessage(data.quoteMessageId, data.conversationId, userId, realCategory,
            null, data.createdAt, status, mediaDuration = duration)
        messageDao.insert(message)
    }

    private fun isExistMessage(messageId: String): Boolean {
        val id = messageDao.findMessageIdById(messageId)
        val messageHistory = messageHistoryDao.findMessageHistoryById(messageId)
        return id != null || messageHistory != null
    }
}
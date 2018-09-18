package one.mixin.android.websocket

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import one.mixin.android.Constants.API.WS_URL
import one.mixin.android.MixinApplication
import one.mixin.android.api.ClientErrorException
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.OffsetDao
import one.mixin.android.extension.gzip
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.ungzip
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshOffsetJob
import one.mixin.android.util.ErrorHandler.Companion.AUTHENTICATION
import one.mixin.android.util.GzipException
import one.mixin.android.util.Session
import one.mixin.android.vo.FloodMessage
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Offset
import one.mixin.android.vo.STATUS_OFFSET
import org.jetbrains.anko.runOnUiThread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChatWebSocket(
    private val okHttpClient: OkHttpClient,
    val app: Application,
    val conversationDao: ConversationDao,
    val messageDao: MessageDao,
    private val offsetDao: OffsetDao,
    private val floodMessageDao: FloodMessageDao,
    val jobManager: MixinJobManager,
    private val linkState: LinkState
) : WebSocketListener() {

    private val failCode = 1000
    private val quitCode = 1001
    var connected: Boolean = false
    private var client: WebSocket? = null
    private val transactions = ConcurrentHashMap<String, WebSocketTransaction>()
    private val gson = Gson()
    private val accountId = Session.getAccountId()

    companion object {
        val TAG = ChatWebSocket::class.java.simpleName
    }

    init {
        connected = false
    }

    @Synchronized
    fun connect() {
        if (client == null) {
            connected = false
            client = okHttpClient.newWebSocket(Request.Builder().url(WS_URL).build(), this)
        }
    }

    @Synchronized
    fun disconnect() {
        if (client != null) {
            closeInternal(quitCode)
            transactions.clear()
            client = null
            connected = false
        }
    }

    @Synchronized
    fun sendMessage(blazeMessage: BlazeMessage): BlazeMessage? {
        var bm: BlazeMessage? = null
        val latch = CountDownLatch(1)
        val transaction = WebSocketTransaction(blazeMessage.id,
            object : TransactionCallbackSuccess {
                override fun success(data: BlazeMessage) {
                    bm = data
                    latch.countDown()
                }
            },
            object : TransactionCallbackError {
                override fun error(data: BlazeMessage?) {
                    bm = data
                    latch.countDown()
                }
            })
        if (client != null && connected) {
            transactions[blazeMessage.id] = transaction
            val result = client!!.send(gson.toJson(blazeMessage).gzip())
            if (result) {
                latch.await(5, TimeUnit.SECONDS)
            }
        } else {
            Log.e(TAG, "WebSocket not connect")
        }
        return bm
    }

    private fun sendPendingMessage() {
        val blazeMessage = createListPendingMessage()
        val transaction = WebSocketTransaction(blazeMessage.id,
            object : TransactionCallbackSuccess {
                override fun success(data: BlazeMessage) {
                }
            },
            object : TransactionCallbackError {
                override fun error(data: BlazeMessage?) {
                    sendPendingMessage()
                }
            })
        transactions[blazeMessage.id] = transaction
        client?.send(gson.toJson(blazeMessage).gzip())
    }

    @Synchronized
    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        if (client != null) {
            connected = true
            client = webSocket
            webSocketObserver?.onSocketOpen()
            MixinApplication.appContext.runOnUiThread {
                linkState.state = LinkState.ONLINE
            }
            connectTimer?.dispose()
            jobManager.start()
            jobManager.addJobInBackground(RefreshOffsetJob())
            sendPendingMessage()
        }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        try {
            val json = bytes?.ungzip()
            val blazeMessage = gson.fromJson(json, BlazeMessage::class.java)
            if (blazeMessage.error == null) {
                if (transactions[blazeMessage.id] != null) {
                    transactions[blazeMessage.id]!!.success.success(blazeMessage)
                    transactions.remove(blazeMessage.id)
                }
                if (blazeMessage.data != null && blazeMessage.isReceiveMessageAction()) {
                    handleReceiveMessage(blazeMessage)
                }
            } else {
                if (transactions[blazeMessage.id] != null) {
                    transactions[blazeMessage.id]!!.error.error(blazeMessage)
                    transactions.remove(blazeMessage.id)
                }
                if (blazeMessage.action == ERROR_ACTION && blazeMessage.error.code == AUTHENTICATION) {
                    connected = false
                    closeInternal(quitCode)
                    (app as MixinApplication).closeAndClear()
                }
            }
        } catch (e: GzipException) {
            Bugsnag.notify(e)
        }
    }

    @SuppressLint("CheckResult")
    @Synchronized
    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        connected = false
        if (code == failCode) {
            closeInternal(code)
            jobManager.stop()
            if (connectTimer == null || connectTimer?.isDisposed == true) {
                connectTimer = Observable.interval(2000, TimeUnit.MILLISECONDS).subscribe({
                    if (MixinApplication.appContext.networkConnected()) {
                        connect()
                    }
                }, {
                })
            }
        } else {
            webSocket?.cancel()
        }
    }

    private var connectTimer: Disposable? = null

    @Synchronized
    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        t?.let {
            Log.e(TAG, "WebSocket onFailure ", it)
        }
        if (client != null) {
            if (t != null && (t is ClientErrorException && t.code == AUTHENTICATION)) {
                closeInternal(quitCode)
            } else {
                onClosed(webSocket, failCode, "OK")
            }
        }
    }

    private fun closeInternal(code: Int) {
        try {
            connected = false
            if (client != null) {
                client!!.close(code, "OK")
            }
        } catch (e: Exception) {
            Bugsnag.notify(e)
        } finally {
            client = null
            webSocketObserver?.onSocketClose()
            MixinApplication.appContext.runOnUiThread {
                linkState.state = LinkState.OFFLINE
            }
        }
    }

    private fun handleReceiveMessage(blazeMessage: BlazeMessage) {
        val data = Gson().fromJson(blazeMessage.data, BlazeMessageData::class.java)
        if (blazeMessage.action == ACKNOWLEDGE_MESSAGE_RECEIPT) {
            makeMessageStatus(data.status, data.messageId)
            offsetDao.insert(Offset(STATUS_OFFSET, data.updatedAt))
        } else if (blazeMessage.action == CREATE_MESSAGE) {
            if (data.userId == accountId && data.category.isEmpty()) {
                makeMessageStatus(data.status, data.messageId)
            } else {
                floodMessageDao.insert(FloodMessage(data.messageId, gson.toJson(data), data.createdAt))
            }
        }
    }

    private fun makeMessageStatus(status: String, messageId: String) {
        val curStatus = messageDao.findMessageStatusById(messageId)
        if (curStatus != null && curStatus != MessageStatus.READ.name) {
            messageDao.updateMessageStatus(status, messageId)
        }
    }

    fun setWebSocketObserver(webSocketObserver: WebSocketObserver) {
        this.webSocketObserver = webSocketObserver
    }

    private var webSocketObserver: WebSocketObserver? = null

    interface WebSocketObserver {
        fun onSocketClose()
        fun onSocketOpen()
    }
}

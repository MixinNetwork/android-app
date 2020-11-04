package one.mixin.android.util.cronet

import android.os.ConditionVariable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.EventListener
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.Locale

class CronetCallback(
    var originRequest: Request,
    val call: Call,
    private val eventListener: EventListener? = null,
    private val responseCallback: Callback? = null
) : UrlRequest.Callback() {
    private lateinit var response: Response

    private val responseCondition = ConditionVariable()

    private val bytesReceived = ByteArrayOutputStream()
    private val receiveChannel = Channels.newChannel(bytesReceived)

    private var followCount = 0

    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        Timber.d("$TAG onResponseStarted $info")

        response = responseFromResponse(info)

        eventListener?.responseHeadersEnd(call, response)
        eventListener?.responseBodyStart(call)

        request.read(ByteBuffer.allocateDirect(32 * 1024))
    }

    override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
        byteBuffer.flip()

        try {
            receiveChannel.write(byteBuffer)
        } catch (e: IOException) {
            Timber.i(" $TAG IOException during ByteBuffer read. Details: $e")
            throw e
        }

        byteBuffer.clear()

        request.read(byteBuffer)
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        eventListener?.responseBodyEnd(call, info.receivedByteCount)

        val contentTypeString = response.header("content-type")
        val contentType = contentTypeString?.toMediaTypeOrNull() ?: "text/plain; charset=\"utf-8\"".toMediaType()

        val responseBody = bytesReceived.toByteArray().toResponseBody(contentType)
        originRequest = originRequest.newBuilder().url(info.url).build()
        response = response.newBuilder().body(responseBody).build()

        responseCondition.open()

        eventListener?.callEnd(call)
        responseCallback?.onResponse(call, response)
    }

    override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
        if (followCount > MAX_FOLLOW_COUNT) {
            request.cancel()
            return
        }
        followCount++
        // TODO match OkHttp followRedirects and followSslRedirects
        request.followRedirect()
    }

    override fun onFailed(request: UrlRequest, info: UrlResponseInfo, error: CronetException) {
        responseCondition.open()

        eventListener?.callFailed(call, error)
        responseCallback?.onFailure(call, error)
    }

    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        responseCondition.open()

        eventListener?.callEnd(call)
    }

    internal fun waitDone(): Response {
        responseCondition.block()
        return response
    }

    private fun protocolFromNegotiatedProtocol(responseInfo: UrlResponseInfo): Protocol {
        val negotiatedProtocol = responseInfo.negotiatedProtocol.toLowerCase(Locale.getDefault())
        return when {
            negotiatedProtocol.contains("quic") -> {
                Protocol.QUIC
            }
            negotiatedProtocol.contains("h2") -> {
                Protocol.HTTP_2
            }
            negotiatedProtocol.contains("1.1") -> {
                Protocol.HTTP_1_1
            }
            else -> {
                Protocol.HTTP_1_0
            }
        }
    }

    private fun headersFromResponse(responseInfo: UrlResponseInfo): Headers {
        val headers = responseInfo.allHeadersAsList
        val headerBuilder = Headers.Builder()
        for ((key, value) in headers) {
            try {
                if (key.equals("content-encoding", ignoreCase = true)) {
                    // Strip all content encoding headers as decoding is done handled by cronet
                    continue
                }
                headerBuilder.add(key, value)
            } catch (e: Exception) {
                Timber.w("$TAG Invalid HTTP header/value: $key$value")
                // Ignore that header
            }
        }
        return headerBuilder.build()
    }

    private fun responseFromResponse(responseInfo: UrlResponseInfo): Response {
        val protocol = protocolFromNegotiatedProtocol(responseInfo)
        val headers: Headers = headersFromResponse(responseInfo)
        return Response.Builder()
            .receivedResponseAtMillis(System.currentTimeMillis())
            .request(call.request())
            .protocol(protocol)
            .code(responseInfo.httpStatusCode)
            .message(responseInfo.httpStatusText)
            .headers(headers)
            .build()
    }

    companion object {
        const val TAG = "CronetCallback"

        const val MAX_FOLLOW_COUNT = 20
    }
}

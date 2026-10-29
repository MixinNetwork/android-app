package one.mixin.android.api.service

import android.app.Application
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.util.GsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [33])
class SuspendApiContractTest {
    @Test
    fun accountRequestsPreserveBodiesAndDecodeResponses() = runBlocking {
        Transport().use { transport ->
            val service = transport.retrofit.create(AccountService::class.java)
            transport.json = """{"data":{"type":"verification","id":"verification-id","has_emergency_contact":true}}"""
            val verification = service.verification(VerificationRequest(phone = "+123456789", purpose = "SESSION", packageName = "example.test"))
            transport.assertRequest("POST", "/api/verifications", """{"phone":"+123456789","purpose":"SESSION","package_name":"example.test","platform":"Android"}""")
            assertEquals("verification-id", verification.data!!.id)
            assertTrue(verification.data!!.hasEmergencyContact)

            val deactivate = service.deactiveVerification("verification-id", DeactivateVerificationRequest("DEACTIVATED", "123456"))
            transport.assertRequest("POST", "/api/verifications/verification-id", """{"purpose":"DEACTIVATED","code":"123456"}""")
            assertEquals("verification-id", deactivate.data!!.id)

            transport.json = """{"data":{"user_id":"user-id","full_name":"A & B","receive_message_source":"CONTACTS","transfer_confirmation_threshold":12.5}}"""
            val update = service.update(AccountUpdateRequest(fullName = "A & B", receiveMessageSource = "CONTACTS", transferConfirmationThreshold = 12.5))
            transport.assertRequest("POST", "/api/me", """{"full_name":"A & B","receive_message_source":"CONTACTS","transfer_confirmation_threshold":12.5}""")
            assertEquals("user-id", update.data!!.userId)
            assertEquals("A & B", update.data!!.fullName)
            assertEquals("CONTACTS", update.data!!.receiveMessageSource)
            assertEquals(12.5, update.data!!.transferConfirmationThreshold, 0.0)
        }
    }

    @Test
    fun authorizationRequestsDecodeListsAndAllowUnitPayloads() = runBlocking {
        Transport().use { transport ->
            val service = transport.retrofit.create(AuthorizationService::class.java)
            transport.json = """{"data":[{"authorization_id":"authorization-id","scopes":["PROFILE:READ"]}]}"""
            val authorizations = service.authorizations()
            transport.assertRequest("GET", "/api/authorizations")
            assertEquals("authorization-id", authorizations.data!!.single().authorizationId)
            assertEquals(listOf("PROFILE:READ"), authorizations.data!!.single().scopes)

            transport.json = """{"data":null}"""
            val cancelled = service.deAuthorize(DeauthorRequest("app-id"))
            transport.assertRequest("POST", "/api/oauth/cancel", """{"client_id":"app-id"}""")
            assertTrue(cancelled.isSuccess)
            assertNull(cancelled.data)
        }
    }

    @Test
    fun conversationRequestsPreservePathsAndAttachmentMetadata() = runBlocking {
        Transport().use { transport ->
            val service = transport.retrofit.create(ConversationService::class.java)
            transport.json = """{"data":{"conversation_id":"conversation-id","category":"GROUP","participants":[]}}"""
            val found = service.findConversationSuspend("conversation-id")
            transport.assertRequest("GET", "/api/conversations/conversation-id")
            assertEquals("conversation-id", found.data!!.conversationId)
            assertEquals("GROUP", found.data!!.category)

            val joined = service.join("invite-code")
            transport.assertRequest("POST", "/api/conversations/invite-code/join")
            assertEquals("conversation-id", joined.data!!.conversationId)

            transport.json = """{"data":{"attachment_id":"attachment-id","upload_url":"https://example.invalid/upload","view_url":"https://example.invalid/view","created_at":"2026-01-01T00:00:00Z"}}"""
            val attachment = service.requestAttachment().data!!
            transport.assertRequest("POST", "/api/attachments")
            assertEquals("attachment-id", attachment.attachment_id)
            assertEquals("https://example.invalid/upload", attachment.upload_url)
            assertEquals("https://example.invalid/view", attachment.view_url)
        }
    }

    @Test
    fun userRequestsEncodeSearchSegmentsAndDecodeBlockedUsers() = runBlocking {
        Transport().use { transport ->
            val service = transport.retrofit.create(UserService::class.java)
            transport.json = """{"data":{"user_id":"user-id","identity_number":"123456","full_name":"Example"}}"""
            val user = service.searchSuspend("person/name").data!!
            transport.assertRequest("GET", "/api/search/person%2Fname")
            assertEquals("person/name", transport.request.url.pathSegments.last())
            assertEquals("user-id", user.userId)
            assertEquals("123456", user.identityNumber)

            transport.json = """{"data":[{"user_id":"blocked-id","full_name":"Blocked"}]}"""
            val blocked = service.blockingUsers().data!!
            transport.assertRequest("GET", "/api/blocking_users")
            assertEquals("blocked-id", blocked.single().userId)
        }
    }

    @Test
    fun giphyRequestsPreservePaginationAndEncodeQueries() = runBlocking {
        Transport(Gson()).use { transport ->
            val service = transport.retrofit.create(GiphyService::class.java)
            transport.json = """{"data":[{"id":"gif-id","type":"gif","images":{}}],"meta":{"status":200,"msg":"OK","response_id":"response-id"},"pagination":{"total_count":80,"count":1,"offset":24}}"""
            val trending = service.trendingGifs(24, 24, "test-key")
            transport.assertRequest("GET", "/api/gifs/trending", query = mapOf("limit" to "24", "offset" to "24", "api_key" to "test-key"))
            assertEquals("gif-id", trending.data.single().id)
            assertEquals(24, trending.pagination.offset)
            assertEquals(200, trending.meta.status)

            val search = service.searchGifs("cats & dogs/猫", 12, 36, "test-key")
            transport.assertRequest("GET", "/api/gifs/search", query = mapOf("q" to "cats & dogs/猫", "limit" to "12", "offset" to "36", "api_key" to "test-key"))
            assertEquals("gif-id", search.data.single().id)
            assertEquals(80, search.pagination.total_count)
        }
    }

    @Test
    fun mixinApiErrorsRemainFailuresForEveryMigratedEndpoint() = runBlocking {
        Transport().use { transport ->
            transport.json = """{"error":{"status":403,"code":403001,"description":"Forbidden"}}"""
            for ((name, invoke) in mixinCalls(transport.retrofit)) {
                val response = invoke()
                assertFalse(name, response.isSuccess)
                assertEquals(name, 403001, response.errorCode)
                assertEquals(name, "Forbidden", response.errorDescription)
                assertNull(name, response.data)
            }
        }
    }

    @Test
    fun httpAndNetworkFailuresPropagateForEveryMigratedEndpoint() = runBlocking {
        Transport().use { transport ->
            for ((name, invoke) in allCalls(transport.retrofit)) {
                for (status in listOf(401, 429, 500)) {
                    transport.status = status
                    val error = runCatching { invoke() }.exceptionOrNull()
                    assertTrue("$name HTTP $status: $error", error is HttpException)
                    assertEquals(status, (error as HttpException).code())
                }
                transport.status = 200
                transport.failure = IOException("offline")
                val error = runCatching { invoke() }.exceptionOrNull()
                assertTrue("$name: $error", error is IOException)
                assertEquals("offline", error!!.message)
                transport.failure = null
            }
        }
    }

    @Test
    fun emptyHttpBodiesCannotBecomeSuccessfulNonNullResponses() = runBlocking {
        Transport().use { transport ->
            transport.status = 204
            transport.json = ""
            for ((name, invoke) in allCalls(transport.retrofit)) {
                val error = runCatching { invoke() }.exceptionOrNull()
                assertTrue("$name: $error", error is KotlinNullPointerException)
            }
        }
    }

    @Test
    fun cancellingSuspendedRequestsCancelsTheirOkHttpCalls() = runBlocking {
        Transport().use { transport ->
            for ((name, invoke) in allCalls(transport.retrofit)) {
                val started = CompletableDeferred<Call>()
                val release = CountDownLatch(1)
                transport.beforeResponse = { chain ->
                    started.complete(chain.call())
                    check(release.await(10, TimeUnit.SECONDS))
                }
                val job = launch { invoke() }
                try {
                    val call = withTimeout(5_000) { started.await() }
                    withTimeout(5_000) { job.cancelAndJoin() }
                    assertTrue(name, call.isCanceled())
                } finally {
                    release.countDown()
                    job.cancelAndJoin()
                }
            }
        }
    }

    private fun mixinCalls(retrofit: Retrofit): List<Pair<String, suspend () -> MixinResponse<*>>> {
        val account = retrofit.create(AccountService::class.java)
        val authorization = retrofit.create(AuthorizationService::class.java)
        val conversation = retrofit.create(ConversationService::class.java)
        val user = retrofit.create(UserService::class.java)
        return listOf<Pair<String, suspend () -> MixinResponse<*>>>(
            "verification" to { account.verification(VerificationRequest(purpose = "SESSION", packageName = "example.test")) },
            "deactiveVerification" to { account.deactiveVerification("verification-id", DeactivateVerificationRequest("DEACTIVATED", "123456")) },
            "update" to { account.update(AccountUpdateRequest(fullName = "Example")) },
            "authorizations" to { authorization.authorizations() },
            "deAuthorize" to { authorization.deAuthorize(DeauthorRequest("app-id")) },
            "findConversationSuspend" to { conversation.findConversationSuspend("conversation-id") },
            "requestAttachment" to { conversation.requestAttachment() },
            "join" to { conversation.join("invite-code") },
            "searchSuspend" to { user.searchSuspend("123456") },
            "blockingUsers" to { user.blockingUsers() },
        )
    }

    private fun allCalls(retrofit: Retrofit): List<Pair<String, suspend () -> Any>> {
        val giphy = retrofit.create(GiphyService::class.java)
        return mixinCalls(retrofit) + listOf<Pair<String, suspend () -> Any>>(
            "trendingGifs" to { giphy.trendingGifs(24, 0, "test-key") },
            "searchGifs" to { giphy.searchGifs("cats", 24, 0, "test-key") },
        )
    }

    private class Transport(gson: Gson = GsonHelper.customGson) : AutoCloseable {
        var status = 200
        var json = "{}"
        var failure: IOException? = null
        var beforeResponse: ((Interceptor.Chain) -> Unit)? = null
        lateinit var request: Request

        private val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                request = chain.request()
                beforeResponse?.invoke(chain)
                failure?.let { throw it }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("Test response")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://example.invalid/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        fun assertRequest(method: String, path: String, body: String? = null, query: Map<String, String> = emptyMap()) {
            assertEquals(method, request.method)
            assertEquals(path, request.url.encodedPath)
            assertEquals(query.keys, request.url.queryParameterNames)
            query.forEach { (key, value) -> assertEquals(value, request.url.queryParameter(key)) }
            val actualBody = request.body?.let { Buffer().apply { it.writeTo(this) }.readUtf8() }.orEmpty()
            if (body == null) {
                assertEquals("", actualBody)
            } else {
                assertEquals(JsonParser.parseString(body), JsonParser.parseString(actualBody))
            }
        }

        override fun close() {
            client.dispatcher.executorService.shutdown()
            assertTrue(client.dispatcher.executorService.awaitTermination(10, TimeUnit.SECONDS))
            client.connectionPool.evictAll()
        }
    }
}

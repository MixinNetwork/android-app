package one.mixin.android.job

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AttachmentExtra
import one.mixin.android.vo.ICategory
import one.mixin.android.vo.MessageBuilder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.websocket.AttachmentMessagePayload

class SendMessageJobTest {
    @Test
    fun appCardDescriptionIsUsedForOutgoingMentions() {
        val category =
            object : ICategory {
                override val type = MessageCategory.APP_CARD.name
            }

        assertEquals(
            "➡️ @7000105334 ⬅️",
            outgoingMentionContent(
                category,
                """{"description":"➡️ @7000105334 ⬅️"}""",
            ),
        )
    }

    @Test
    fun invalidAppCardHasNoOutgoingMentionContent() {
        val category =
            object : ICategory {
                override val type = MessageCategory.APP_CARD.name
            }

        assertNull(outgoingMentionContent(category, "invalid"))
    }

    @Test
    fun resentAttachmentContentCanBeDecodedByTheReceiver() {
        val extra =
            GsonHelper.customGson.toJson(
                AttachmentExtra(attachmentId = "attachment", messageId = "message", createdAt = "2026-10-31T12:00:00Z"),
            )
        val message =
            MessageBuilder("message", "conversation", "sender", MessageCategory.SIGNAL_IMAGE.name, "read", "2026-10-31T12:00:00Z")
                .setContent(extra)
                .setMediaMimeType("image/png")
                .setMediaSize(1024)
                .setMediaWidth(20)
                .setMediaHeight(10)
                .setMediaKey(byteArrayOf(1, 2))
                .setMediaDigest(byteArrayOf(3, 4))
                .setThumbImage("thumb")
                .setCaption("caption")
                .build()

        // The stored content is not a payload: the receiver cannot read it.
        assertFailsWith<Exception> { String(extra.decodeBase64()) }

        val payload =
            GsonHelper.customGson.fromJson(
                String(resendAttachmentContent(message).decodeBase64()),
                AttachmentMessagePayload::class.java,
            )
        assertEquals("attachment", payload.attachmentId)
        assertContentEquals(byteArrayOf(1, 2), payload.key)
        assertContentEquals(byteArrayOf(3, 4), payload.digest)
        assertEquals("image/png", payload.mimeType)
        assertEquals(1024L, payload.size)
        assertEquals(20, payload.width)
        assertEquals(10, payload.height)
        assertEquals("thumb", payload.thumbnail)
        assertEquals("caption", payload.caption)
        assertEquals("2026-10-31T12:00:00Z", payload.createdAt)
    }

    @Test
    fun resentAttachmentKeepsContentThatIsAlreadyAPayload() {
        val payload = """{"attachment_id":"attachment"}""".base64Encode()
        val message =
            MessageBuilder("message", "conversation", "sender", MessageCategory.SIGNAL_IMAGE.name, "read", "2026-10-31T12:00:00Z")
                .setContent(payload)
                .build()

        assertEquals(payload, resendAttachmentContent(message))
    }
}

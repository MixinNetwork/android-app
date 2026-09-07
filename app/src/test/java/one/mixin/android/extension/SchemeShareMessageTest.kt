package one.mixin.android.extension

import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class SchemeShareMessageTest {
    @Test
    fun rejectsLocalImageSourcesBeforeRecipientBranching() {
        listOf(
            "file:///data/user/0/app/files/private.jpg",
            "content://one.mixin.messenger.provider/root/private.jpg",
            "content://0@one.mixin.messenger.provider/root/private.jpg",
            "/data/user/0/app/files/private.jpg",
            "android.resource://one.mixin.messenger/1",
            "https:/missing-host.jpg",
            "https://user@example.com/image.jpg",
        ).forEach {
            assertNull(parseSchemeShareMessage(ShareCategory.Image, encodedImage(it)))
        }
    }

    @Test
    fun acceptsRemoteImagesAndDropsInternalAttachmentCapabilities() {
        val result = parseSchemeShareMessage(ShareCategory.Image, encodedImage("https://example.com/image.jpg", "internal-attachment"))!!
        val image = GsonHelper.customGson.fromJson(result.content, ShareImageData::class.java)
        assertEquals("https://example.com/image.jpg", image.url)
        assertNull(image.attachmentExtra)
        assertNull(parseSchemeShareMessage(ShareCategory.Image, "not base64"))
    }

    private fun encodedImage(url: String, attachmentExtra: String? = null) =
        Base64.getEncoder().encodeToString(GsonHelper.customGson.toJson(ShareImageData(url, attachmentExtra)).toByteArray())
}

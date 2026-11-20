package one.mixin.android.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class ShareUriValidatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val packageName = "one.mixin.messenger"

    @Test
    fun `shares require external content authorities`() {
        assertTrue(isAllowedShareUri("content", "com.android.providers.media.documents", packageName, "com.android.providers.media"))
        assertTrue(isAllowedShareUri("content", "external.provider", packageName, null))
        for (scheme in listOf(null, "file", "https", "CONTENT")) {
            assertFalse(isAllowedShareUri(scheme, "external.provider", packageName, null))
        }
        for (authority in listOf(null, "", " ", "$packageName.provider", "0@$packageName.provider")) {
            assertFalse(isAllowedShareUri("content", authority, packageName, null))
        }
        assertFalse(isAllowedShareUri("content", "another.authority", packageName, packageName))
        assertFalse(isAllowedShareUri("content", "10@another.authority", packageName, packageName))
    }

    @Test
    fun `private paths include traversal and symlinks but not sibling prefixes`() {
        val dataDir = temporaryFolder.newFolder("data")
        val outside = temporaryFolder.newFolder("data-other")
        assertTrue(isPrivateAttachmentPath(dataDir, dataDir))
        assertTrue(isPrivateAttachmentPath(File(dataDir, "files/secret"), dataDir))
        assertTrue(isPrivateAttachmentPath(File(outside, "../data/databases/secret"), dataDir))
        assertFalse(isPrivateAttachmentPath(File(outside, "file"), dataDir))
        val link = File(outside, "link")
        Files.createSymbolicLink(link.toPath(), dataDir.toPath())
        assertTrue(isPrivateAttachmentPath(File(link, "secret"), dataDir))
    }
}

package one.mixin.android.util

import one.mixin.android.crypto.EncryptedProtocol
import kotlin.test.Test

class EncryptedProtocolTest {

    @Test
    fun testEncryptAndDecrypt() {
        // encryptedMessageData: [2, 87, -21, 35, 69, -66, 63, 18, 35, -63, -90, 91, 66, 22, 22, 86, -10, 63, 69, -38, 5, -24, 15, -77, -69, 16, -32, 7, 28]
        // senderSessionId: [42, -126, 23, -7, -42, -66, 71, -60, -107, 40, 41, 28, 38, 125, -112, -77]
        // messageKey: [-99, -57, 103, 10, 1, -111, -102, 0, -31, 74, -125, -116, 64, -114, 31, 58, -6, -87, 123, 13, -49, 3, 23, 102, -57, -109, 42, 101, -76, 19, -10, 127, 122, -83, -15, 108, -26, -86, 87, 74, 54, 55, -72, -96, 80, 120, -107, 91]
        // senderPublicKey: [42, -112, -11, -78, 59, 88, -4, -48, -71, 2, -33, 10, -122, 9, -35, 1, 12, -76, -111, 18, 0, -30, 35, 85, -74, -57, -34, -13, 17, 25, -102, 75]

        val content = "L".toByteArray()
        // val seedBase64 = "xkogvQWXVP/Ed8G7l/dYthuovd9mBSEc87vBLDgLC4A="
        val seedByteArray = byteArrayOf(-58, 74, 32, -67, 5, -105, 84, -1, -60, 119, -63, -69, -105, -9, 88, -74, 27, -88, -67, -33, 102, 5, 33, 28, -13, -69, -63, 44, 56, 11, 11, -128)
        // val encodedContentBase64 = "AQEAKpD1sjtY/NC5At8KhgndAQy0kRIA4iNVtsfe8xEZmksqghf51r5HxJUoKRwmfZCzX+LG9wCqlkQS0aHV3liib3QzlwseOKDEB2F9b2Q/mYC/1sOLCPaxLaaCmrUr1tkzY1VWjEkutI7Hd92926NdcuEc9yZbLptTyL+/P0E="
        // val encodedByteArray = byteArrayOf(1, 1, 0, 42, -112, -11, -78, 59, 88, -4, -48, -71, 2, -33, 10, -122, 9, -35, 1, 12, -76, -111, 18, 0, -30, 35, 85, -74, -57, -34, -13, 17, 25, -102, 75, 42, -126, 23, -7, -42, -66, 71, -60, -107, 40, 41, 28, 38, 125, -112, -77, 95, -30, -58, -9, 0, -86, -106, 68, 18, -47, -95, -43, -34, 88, -94, 111, 116, 51, -105, 11, 30, 56, -96, -60, 7, 97, 125, 111, 100, 63, -103, -128, -65, -42, -61, -117, 8, -10, -79, 45, -90, -126, -102, -75, 43, -42, -39, 51, 99, 85, 86, -116, 73, 46, -76, -114, -57, 119, -35, -67, -37, -93, 93, 114, -31, 28, -9, 38, 91, 46, -101, 83, -56, -65, -65, 63, 65)
        val otherPublic = byteArrayOf(110, -16, -31, 38, -22, 7, 101, -16, 20, -31, 72, -52, -64, -95, -28, -87, 69, 113, 127, 73, -1, -6, 124, 12, -39, -128, -112, 127, -40, 78, 96, 0)
        val otherSessionId = "2a8217f9-d6be-47c4-9528-291c267d90b3"

        val encryptedProtocol = EncryptedProtocol()

        val encodedContent = encryptedProtocol.encryptMessage(seedByteArray, content, otherPublic, otherSessionId)

        val otherSeedByteArray = byteArrayOf()
        val decryptedContent = encryptedProtocol.decryptMessage(otherSeedByteArray, encodedContent)

        assert(decryptedContent.contentEquals(content))
    }
}
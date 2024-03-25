package one.mixin.android

import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.junit.Test
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Keys
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Bip44Test {
    @Test
    fun testBip44() {
        val seed = "f01a27c0cafc921b3a1e1e4bd5c8cc9e1fe8e7cf2edcd9a846233d1e55462768"
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed.hexStringToByteArray())
        val key = generateBip44Key(masterKeyPair, Bip44Path.Ethereum)
        val address = Keys.toChecksumAddress(Keys.getAddress(key.publicKey))

        assertContentEquals(key.publicKey.toByteArray(), "6defa9e4188f3ca577c40895358391ec58c54f7455d5d83b8c883f018a8e01fe7ab418019f5e9bd7de0a43dd08ee68f60a8ed1d5df62aed19d99f4a187d85f4e".hexStringToByteArray())
        assertEquals(address, "0x28fB45dbcb4d244Fed7e824F1fb1f19DCd283D06")
    }
}

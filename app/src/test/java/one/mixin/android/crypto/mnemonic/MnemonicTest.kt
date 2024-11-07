package one.mixin.android.crypto.mnemonic

import one.mixin.android.crypto.toSeed
import one.mixin.android.crypto.mnemonicChecksumIndex
import one.mixin.android.extension.hexString
import org.junit.Test
import org.web3j.crypto.Bip32ECKeyPair
import kotlin.test.assertEquals

class MnemonicTest {

    @Test
    fun mnemonicTest() {
        val mn = "legal winner thank year wave sausage worth useful legal winner thank yellow"
        val seed = toSeed(mn.split(" "), "")
        println(seed.hexString())
        assertEquals(seed.hexString() ,"878386efb78845b3355bd15ea4d39ef97d179cb712b77d5c12b6be415fffeffe5f377ba02bf3f8544ab800b955e51fbff09828f682052a20faa6addbbddfb096")
        val key = Bip32ECKeyPair.generateKeyPair(seed)
        println(key.privateKey.toByteArray().hexString())
        println(key.publicKey.toByteArray().hexString())
        assertEquals(key.privateKey.toByteArray().hexString() ,"7e56ecf5943d79e1f5f87e11c768253d7f3fcf30ae71335611e366c578b4564e")

        var mnemonic = "ought darted yawning apricot hold odds goblet logic loyal drying tucks atom".split(" ")
        var index = mnemonicChecksumIndex(mnemonic, 3)
        assertEquals("goblet", mnemonic[index])
        println(mnemonic[index])
        mnemonic = "vogue juggled dyslexic hounded revamp zapped ambush hunter hire duets potato noted".split(" ")
        index = mnemonicChecksumIndex(mnemonic, 3)
        println(mnemonic[index])
        assertEquals("vogue", mnemonic[index])
    }
}
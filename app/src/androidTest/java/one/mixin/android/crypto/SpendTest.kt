package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lambdapioneer.argon2kt.Argon2Kt
import junit.framework.TestCase.assertEquals
import one.mixin.android.extension.hexString
import one.mixin.android.extension.hexStringToByteArray
import org.junit.Test
import org.junit.runner.RunWith
import org.web3j.crypto.Bip32ECKeyPair

@RunWith(AndroidJUnit4::class)
class SpendTest {
    @Test
    fun legacySpendTest() {
        val entropy = "b303a9017e698fe77d3ab448f059e49091525fe61e8d7c588664a102cfd6da4d".hexStringToByteArray()
        val legacyMn = toMnemonic(entropy)
        assertEquals(legacyMn, "reason bubble doctor wolf ocean victory visual final employ lizard junior cancel benefit copper observe spider labor service odor dragon coconut twin hard sail")
        val legacySeed = toSeed(legacyMn.split(" "), "")
        val legacyKey = newMasterPrivateKeyFromMnemonic(legacyMn)
        assertEquals(legacyKey.privateKey.toByteArray().hexString(), "140d51ebe0eecba895236984b59144c0c98f60cd21d470ff55910985611031f6")
        val tipSeed = "35c7b06243a170bf9cfe68df3bb3082d50a35dc16ddb6f5ce1eb37cf2caeecc3".hexStringToByteArray()
        val argon2Kt = Argon2Kt()
        val spendSeed = argon2Kt.argon2IHash(legacySeed, tipSeed).rawHashAsByteArray()
        val spendKeyPair = newKeyPairFromSeed(spendSeed)
        assertEquals(spendKeyPair.privateKey.hexString(), "56cf1c919c17986ada2d9d7ef1485806a5b4874140dfaefaf68666e31a1f6430")
    }

    @Test
    fun spendTest() {
        val entropy = "15f5ef750481c46c6803ce2d364b6fc1".hexStringToByteArray()
        val mn = toMnemonic(entropy)
        assertEquals(mn, "bicycle quarter tail animal brisk curtain parade keep coffee rather swim lonely")
        val seed = toSeed(mn.split(" "), "")
        val key = newMasterPrivateKeyFromMnemonic(mn)
        assertEquals(key.privateKeyAsHex, "9d9a820a6dd9c1bba705d57b91d4c20cc00d4bba3815479153ca2c13403907bf")
        val tipSeed = "35c7b06243a170bf9cfe68df3bb3082d50a35dc16ddb6f5ce1eb37cf2caeecc3".hexStringToByteArray()
        val argon2Kt = Argon2Kt()
        val spendSeed = argon2Kt.argon2IHash(seed, tipSeed).rawHashAsByteArray()
        val spendKeyPair = newKeyPairFromSeed(spendSeed)
        assertEquals(spendKeyPair.privateKey.hexString(), "a8883d100dbf34f00337a7fbe6f600c917a85b173739bab9de871e77bc7776eb")
    }
}
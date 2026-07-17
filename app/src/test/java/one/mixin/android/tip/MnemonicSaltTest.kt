package one.mixin.android.tip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MnemonicSaltTest {
    @Test
    fun `removes local mnemonic only when Safe entropy matches`() {
        val entropy = byteArrayOf(1, 2, 3)

        assertTrue(shouldRemoveLocalMnemonic(entropy, entropy.copyOf()))
        assertFalse(shouldRemoveLocalMnemonic(entropy, byteArrayOf(3, 2, 1)))
        assertFalse(shouldRemoveLocalMnemonic(null, entropy))
    }

    @Test
    fun `uses verified Safe entropy for phone account pending import`() {
        val safeEntropy = byteArrayOf(1, 2, 3)

        assertContentEquals(
            safeEntropy,
            resolvePhonePendingImportEntropy(byteArrayOf(4, 5, 6), safeEntropy.copyOf(), safeEntropy),
        )
        assertContentEquals(
            safeEntropy,
            resolvePhonePendingImportEntropy(null, null, safeEntropy),
        )
    }

    @Test
    fun `keeps pending import entropy when Safe does not match local`() {
        val pendingEntropy = byteArrayOf(1, 2, 3)
        val localEntropy = byteArrayOf(1, 2, 3)
        val safeEntropy = byteArrayOf(3, 2, 1)

        assertContentEquals(
            pendingEntropy,
            resolvePhonePendingImportEntropy(pendingEntropy, localEntropy, safeEntropy),
        )
        assertContentEquals(
            localEntropy,
            resolvePhonePendingImportEntropy(null, localEntropy, safeEntropy),
        )
    }

    @Test
    fun `marks pending mnemonic login as exported after Safe verification`() {
        val localEntropy = byteArrayOf(1, 2, 3)
        val safeEntropy = localEntropy.copyOf()

        assertTrue(shouldMarkPendingMnemonicAsExported(false, localEntropy, null))
        assertTrue(shouldMarkPendingMnemonicAsExported(true, null, safeEntropy))
        assertTrue(shouldMarkPendingMnemonicAsExported(true, localEntropy, safeEntropy))
        assertFalse(shouldMarkPendingMnemonicAsExported(true, localEntropy, byteArrayOf(3, 2, 1)))
    }
}

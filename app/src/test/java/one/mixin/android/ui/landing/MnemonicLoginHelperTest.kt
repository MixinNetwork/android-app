package one.mixin.android.ui.landing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MnemonicLoginHelperTest {
    @Test
    fun `completes 12 word phrase before login processing`() {
        val words = (1..12).map { "word$it" }

        val result = completeMnemonicForLogin(words) { input ->
            input + "checksum"
        }

        assertEquals(words + "checksum", result)
    }

    @Test
    fun `completes 24 word phrase before login processing`() {
        val words = (1..24).map { "word$it" }

        val result = completeMnemonicForLogin(words) { input ->
            input + "checksum"
        }

        assertEquals(words + "checksum", result)
    }

    @Test
    fun `keeps 13 and 25 word phrases unchanged`() {
        val thirteenWords = (1..13).map { "word$it" }
        val twentyFiveWords = (1..25).map { "word$it" }

        assertEquals(thirteenWords, completeMnemonicForLogin(thirteenWords) { error("unused") })
        assertEquals(twentyFiveWords, completeMnemonicForLogin(twentyFiveWords) { error("unused") })
    }

    @Test
    fun `keeps 12 and 24 word phrases as pending import mnemonic`() {
        val twelveWords = (1..12).map { "word$it" }
        val twentyFourWords = (1..24).map { "word$it" }

        assertEquals(twelveWords, pendingImportMnemonicForLogin(twelveWords))
        assertEquals(twentyFourWords, pendingImportMnemonicForLogin(twentyFourWords))
    }

    @Test
    fun `does not keep 13 and 25 word phrases as pending import mnemonic`() {
        assertEquals(null, pendingImportMnemonicForLogin((1..13).map { "word$it" }))
        assertEquals(null, pendingImportMnemonicForLogin((1..25).map { "word$it" }))
    }

    @Test
    fun `prepares short mnemonic as completed login words plus pending import words`() {
        val words = (1..12).map { "word$it" }

        val result = prepareMnemonicForLogin(words) { input ->
            input + "checksum"
        }

        assertEquals(words + "checksum", result.completedWords)
        assertEquals(words, result.pendingImportWords)
    }

    @Test
    fun `prepares recovery kit mnemonic as completed login words without pending import`() {
        val words = (1..13).map { "word$it" }

        val result = prepareMnemonicForLogin(words) { error("unused") }

        assertEquals(words, result.completedWords)
        assertEquals(null, result.pendingImportWords)
    }

    @Test
    fun `rejects unsupported word counts`() {
        assertFailsWith<IllegalArgumentException> {
            completeMnemonicForLogin((1..11).map { "word$it" }) { error("unused") }
        }
    }

    @Test
    fun `stores login mnemonic until Safe registration`() {
        assertEquals(true, shouldStoreLoginMnemonicForSafe(hasSafe = false, hasPhone = true, hasPendingWalletImport = false))
        assertEquals(true, shouldStoreLoginMnemonicForSafe(hasSafe = false, hasPhone = false, hasPendingWalletImport = false))
    }

    @Test
    fun `keeps local mnemonic for accounts without phone recovery`() {
        assertEquals(true, shouldStoreLoginMnemonicForSafe(hasSafe = true, hasPhone = false, hasPendingWalletImport = true))
    }

    @Test
    fun `stores pending wallet mnemonic until Safe salt is verified for phone accounts`() {
        assertEquals(true, shouldStoreLoginMnemonicForSafe(hasSafe = false, hasPhone = true, hasPendingWalletImport = true))
        assertEquals(true, shouldStoreLoginMnemonicForSafe(hasSafe = true, hasPhone = true, hasPendingWalletImport = true))
    }
}

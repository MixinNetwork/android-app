package one.mixin.android.ui.wallet

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialClassicWalletProvisioningTest {
    @Test
    fun firstWalletInClassicCategoryUsesIndexZero() {
        assertEquals(0, classicWalletIndexForCreation(hasClassicWallet = false, maxClassicIndex = 8))
    }

    @Test
    fun additionalClassicWalletUsesNextCategoryIndex() {
        assertEquals(9, classicWalletIndexForCreation(hasClassicWallet = true, maxClassicIndex = 8))
    }

    @Test
    fun createsClassicWalletAtIndexZeroWhenStillMissingAfterSync() = runBlocking {
        var syncCount = 0
        var createdIndex: Int? = null

        val result = ensureInitialClassicWallet(
            syncWallets = {
                syncCount++
                if (syncCount == 1) emptyList() else listOf("classic")
            },
            isClassicWallet = { it == "classic" },
            createClassicWallet = { index -> createdIndex = index },
        )

        assertEquals(0, createdIndex)
        assertEquals(2, syncCount)
        assertEquals(listOf("classic"), result)
    }

    @Test
    fun doesNotCreateClassicWalletWhenAuthoritativeSyncContainsIt() = runBlocking {
        var syncCount = 0
        var created = false

        val result = ensureInitialClassicWallet(
            syncWallets = {
                syncCount++
                listOf("classic")
            },
            isClassicWallet = { it == "classic" },
            createClassicWallet = { created = true },
        )

        assertEquals(listOf("classic"), result)
        assertEquals(1, syncCount)
        assertFalse(created)
    }

    @Test
    fun failedInitialSyncStopsBeforeCreation() = runBlocking {
        var syncCount = 0
        var created = false

        val result = ensureInitialClassicWallet<String>(
            syncWallets = {
                syncCount++
                null
            },
            isClassicWallet = { it == "classic" },
            createClassicWallet = { created = true },
        )

        assertEquals(null, result)
        assertEquals(1, syncCount)
        assertFalse(created)
    }

    @Test
    fun postCreationSyncMustContainClassicWallet() = runBlocking {
        var syncCount = 0
        var created = false

        val result = ensureInitialClassicWallet<String>(
            syncWallets = {
                syncCount++
                if (syncCount == 1) emptyList() else null
            },
            isClassicWallet = { it == "classic" },
            createClassicWallet = {
                created = true
            },
        )

        assertEquals(null, result)
        assertEquals(2, syncCount)
        assertTrue(created)
    }
}

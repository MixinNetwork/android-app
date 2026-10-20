package one.mixin.android.ui.wallet

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletHomePreferencesTest {
    @Test
    fun dynamicBannerClosedKeyIsGlobal() {
        assertEquals(
            PREF_WALLET_HOME_DYNAMIC_BANNER_CLOSED,
            walletHomeDynamicBannerClosedKey(),
        )
    }

    @Test
    fun concurrentDismissalsPreserveExistingAndNewIds() = runBlocking {
        var stored = setOf("existing")
        val firstWriteStarted = CompletableDeferred<Unit>()
        val releaseFirstWrite = CompletableDeferred<Unit>()
        val first = launch {
            updateWalletHomeDynamicBannerClosedIds(setOf("a"), read = { stored }, write = {
                firstWriteStarted.complete(Unit)
                releaseFirstWrite.await()
                stored = it
            })
        }
        firstWriteStarted.await()
        val second = launch {
            updateWalletHomeDynamicBannerClosedIds(setOf("b"), read = { stored }, write = { stored = it })
        }
        yield()
        releaseFirstWrite.complete(Unit)
        first.join()
        second.join()
        assertEquals(setOf("existing", "a", "b"), stored)
    }

    @Test
    fun parsesClosedIdsWithoutTreatingCorruptionAsEmpty() {
        assertEquals(emptySet<String>(), parseWalletHomeDynamicBannerClosedIds(""))
        assertEquals(emptySet<String>(), parseWalletHomeDynamicBannerClosedIds("[]"))
        assertEquals(setOf("a", "b"), parseWalletHomeDynamicBannerClosedIds("[\"a\",\"b\",\"a\",\"\"]"))
        for (value in listOf("[", "null", "{}")) {
            assertTrue(runCatching { parseWalletHomeDynamicBannerClosedIds(value) }.isFailure)
        }
    }

    @Test
    fun corruptStoredIdsAbortWriteAndReleaseLock() = runBlocking {
        var wrote = false
        val result = runCatching {
            updateWalletHomeDynamicBannerClosedIds(
                setOf("new"),
                read = { parseWalletHomeDynamicBannerClosedIds("[") },
                write = { wrote = true },
            )
        }
        assertTrue(result.isFailure)
        assertFalse(wrote)
        updateWalletHomeDynamicBannerClosedIds(setOf("new"), read = { setOf("existing") }, write = {
            assertEquals(setOf("existing", "new"), it)
            wrote = true
        })
        assertTrue(wrote)
    }

    @Test
    fun queuedDismissalSurvivesCallerCancellation() = runBlocking {
        var stored = emptySet<String>()
        val writing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = launch {
            updateWalletHomeDynamicBannerClosedIds(setOf("a"), read = { stored }, write = {
                writing.complete(Unit)
                release.await()
                stored = it
            })
        }
        writing.await()
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            updateWalletHomeDynamicBannerClosedIds(setOf("b"), read = { stored }, write = { stored = it })
        }
        second.cancel()
        first.cancel()
        release.complete(Unit)
        first.join()
        second.join()
        assertEquals(setOf("a", "b"), stored)
    }
}

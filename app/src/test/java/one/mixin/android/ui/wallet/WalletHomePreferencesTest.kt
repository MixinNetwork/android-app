package one.mixin.android.ui.wallet

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
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
}

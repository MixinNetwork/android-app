package one.mixin.android.db

import android.content.Context
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.ChainId
import one.mixin.android.vo.Address
import one.mixin.android.vo.Chain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AddressBookDaoTest {
    @Test
    fun allAddressesIncludesUncachedAssetsAcrossChainsAndReflectsDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()
        try {
            val dao = database.addressDao()
            val ethereum = Address(
                addressId = "ethereum",
                type = "address",
                assetId = "uncached-ethereum-asset",
                chainId = ChainId.ETHEREUM_CHAIN_ID,
                destination = "shared-destination",
                label = "Ethereum",
                updatedAt = "2026-10-27T12:00:00Z",
                tag = null,
                dust = null,
            )
            val bnb = ethereum.copy(
                addressId = "bnb",
                assetId = "uncached-bnb-asset",
                chainId = ChainId.BinanceSmartChain,
                label = "BNB Smart Chain",
                updatedAt = "2026-10-28T12:00:00Z",
            )
            dao.insertList(listOf(ethereum, bnb))

            assertEquals(listOf(bnb, ethereum), dao.allAddressesFlow().first())

            dao.deleteById(bnb.addressId)
            assertEquals(listOf(ethereum), dao.allAddressesFlow().first())

            database.chainDao().insertSuspend(
                Chain(
                    chainId = ChainId.ETHEREUM_CHAIN_ID,
                    name = "Ethereum",
                    symbol = "ETH",
                    iconUrl = "https://example.com/eth.png",
                    threshold = 1,
                ),
            )
            val items = dao.allAddresses().read()
            assertEquals(1, items.size)
            assertEquals(ChainId.ETHEREUM_CHAIN_ID, items.single().chainId)
            assertEquals("https://example.com/eth.png", items.single().iconUrl)
            assertEquals("https://example.com/eth.png", items.single().chainUrl)
        } finally {
            database.close()
        }
    }

    private fun <T> LiveData<T>.read(): T {
        val observer = Observer<T> {}
        observeForever(observer)
        try {
            shadowOf(Looper.getMainLooper()).idle()
            return requireNotNull(value)
        } finally {
            removeObserver(observer)
        }
    }
}

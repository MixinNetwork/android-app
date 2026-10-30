package one.mixin.android.db

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.ChainId
import one.mixin.android.vo.Address
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        } finally {
            database.close()
        }
    }
}

package one.mixin.android.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import one.mixin.android.session.Session
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.route.serializer.SafeBoxSerializer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafeBoxStoreManager
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
    ) {
        private data class StoreEntry(
            val store: DataStore<SafeBox>,
            val scope: CoroutineScope,
        )

        private val stores = ConcurrentHashMap<String, StoreEntry>()

        fun current(): DataStore<SafeBox> {
            val accountId = Session.getAccountId() ?: "temp"
            return stores.getOrPut(accountId) {
                val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                val store = DataStoreFactory.create(
                    serializer = SafeBoxSerializer,
                    produceFile = { appContext.dataStoreFile("safe_box_$accountId.store") },
                    scope = scope,
                )
                StoreEntry(store, scope)
            }.store
        }

        /**
         * Clear the DataStore for the current account.
         * This cancels the associated coroutine scope and removes the store from the cache.
         */
        fun clearCurrent() {
            val accountId = Session.getAccountId() ?: return
            stores.remove(accountId)?.let { entry ->
                entry.scope.cancel()
            }
        }

        /**
         * Clear all DataStores and cancel all associated coroutine scopes.
         * This should be called on logout or when switching accounts.
         */
        fun clearAll() {
            stores.values.forEach { entry ->
                entry.scope.cancel()
            }
            stores.clear()
        }
    }

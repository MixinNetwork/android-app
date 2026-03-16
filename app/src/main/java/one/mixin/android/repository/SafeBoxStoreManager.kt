package one.mixin.android.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        private val stores = ConcurrentHashMap<String, DataStore<SafeBox>>()

        fun current(): DataStore<SafeBox> {
            val accountId = Session.getAccountId() ?: "temp"
            return stores.getOrPut(accountId) {
                DataStoreFactory.create(
                    serializer = SafeBoxSerializer,
                    produceFile = { appContext.dataStoreFile("safe_box_$accountId.store") },
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                )
            }
        }
    }

package one.mixin.android.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.db.SessionDao
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.job.RefreshOneTimePreKeysJob

class LoadingViewModel @Inject internal
constructor(
    private val signalKeyService: SignalKeyService,
    private val accountService: AccountService,
    private val userService: UserService
) : ViewModel() {
    private val sessionDao: SessionDao = SignalDatabase.getDatabase(MixinApplication.appContext).sessionDao()

    suspend fun pushAsyncSignalKeys(): MixinResponse<Void> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val signalKeys = RefreshOneTimePreKeysJob.generateKeys()
        val response = signalKeyService.pushSignalKeys(signalKeys).await()
        val time = System.currentTimeMillis() - start
        if (time < 2000) {
            delay(time)
        }
        return@withContext response
    }

    suspend fun updateSignalSession() {
        val sessions = sessionDao.syncGetSessionAddress()
        sessions?.let {
            val sessionChunk = it.chunked(500)
            for (item in sessionChunk) {
                userService.fetchSessions(item)
            }
        }
    }

    fun pingServer(callback: () -> Unit, elseCallBack: (e: Exception?) -> Unit): Job {
        return viewModelScope.launch {
            try {
                val response = withContext(coroutineContext + Dispatchers.IO) { accountService.ping().execute() }
                response.headers()["X-Server-Time"]?.toLong()?.let { serverTime ->
                    if (abs(serverTime / 1000000 - System.currentTimeMillis()) < 600000L) { // 10 minutes
                        callback.invoke()
                    } else {
                        elseCallBack.invoke(null)
                    }
                }
            } catch (e: Exception) {
                elseCallBack.invoke(e)
            }
        }
    }
}

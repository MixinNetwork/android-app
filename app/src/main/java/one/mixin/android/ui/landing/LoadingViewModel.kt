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
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.job.RefreshOneTimePreKeysJob

class LoadingViewModel @Inject internal
constructor(
    private val signalKeyService: SignalKeyService,
    private val accountService: AccountService
) : ViewModel() {

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

package one.mixin.android.ui.landing

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.job.RefreshOneTimePreKeysJob
import javax.inject.Inject

class LoadingViewModel @Inject internal
constructor(
    private val signalKeyService: SignalKeyService,
    private val accountService: AccountService,
    private val app: Application
) : ViewModel() {

    fun pushAsyncSignalKeys(): Observable<MixinResponse<Void>?> {
        val start = System.currentTimeMillis()
        var signalKeys: SignalKeyRequest? = null
        return Observable.just(app).observeOn(Schedulers.io()).subscribeOn(Schedulers.io()).flatMap { _ ->
            if (signalKeys == null) {
                signalKeys = RefreshOneTimePreKeysJob.generateKeys()
            }
            val response = signalKeyService.pushSignalKeys(signalKeys!!).execute().body()
            if (response != null && response.isSuccess) {
            }
            val time = System.currentTimeMillis() - start
            if (time < 2000) {
                Thread.sleep(time)
            }
            Observable.just(response)
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
    }

    fun pingServer(callback: () -> Unit, elseCallBack: (e: Exception?) -> Unit): Job {
        return viewModelScope.launch {
            try {
                val response = accountService.ping().execute()

                response.headers()["X-Server-Time"]?.toLong()?.let { serverTime ->
                    if (Math.abs(serverTime / 1000000 - System.currentTimeMillis()) < 600000L) { // 10 minutes
                        withContext(Dispatchers.Main) {
                            callback.invoke()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            elseCallBack.invoke(null)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    elseCallBack.invoke(e)
                }
            }
        }
    }
}
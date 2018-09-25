package one.mixin.android.ui.landing

import android.app.Application
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.job.RefreshOneTimePreKeysJob
import javax.inject.Inject

class LoadingViewModel @Inject internal
constructor(private val signalKeyService: SignalKeyService, private val app: Application) : ViewModel() {

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
}
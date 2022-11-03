package one.mixin.android.ui.tip

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.service.TipNodeService
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class TipViewModel
@Inject
internal constructor(
    private val tipNodeService: TipNodeService,
    private val tipConfig: TipConfig
) : ViewModel() {

    suspend fun checkTipNodeConnect(): Pair<Boolean, String> {
        val signers = tipConfig.signers
        val nodeFailedInfo = StringBuffer()
        val successSum = AtomicInteger(0)
        coroutineScope {
            signers.map { signer ->
                async(Dispatchers.IO) {
                    kotlin.runCatching {
                        tipNodeService.get(signer.api)
                        successSum.incrementAndGet()
                    }.onFailure {
                        if (it is HttpException) {
                            nodeFailedInfo.append("[${signer.index}, ${it.code()}] ")
                        }
                    }
                }
            }.awaitAll()
        }
        return Pair(successSum.get() == signers.size, nodeFailedInfo.toString())
    }
}

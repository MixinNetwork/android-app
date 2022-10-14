package one.mixin.android.ui.tip

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.service.TipNodeService
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class TipViewModel
@Inject
internal constructor(
    private val tipNodeService: TipNodeService,
    private val tipConfig: TipConfig
) : ViewModel() {

    suspend fun checkTipNodeConnect(): Boolean {
        val signers = tipConfig.signers
        val successSum = AtomicInteger(0)
        coroutineScope {
            signers.map { signer ->
                async(Dispatchers.IO) {
                    kotlin.runCatching {
                        tipNodeService.get(signer.api)
                        successSum.incrementAndGet()
                    }
                }
            }.awaitAll()
        }
        return successSum.get() == signers.size
    }
}

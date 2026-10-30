package one.mixin.android.ui.wallet.network

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.db.ChainDao
import javax.inject.Inject

@HiltViewModel
class NetworkIconViewModel @Inject constructor(
    private val chainDao: ChainDao,
) : ViewModel() {
    suspend fun iconUrls(chainIds: Set<String>): Map<String, String> {
        if (chainIds.isEmpty()) return emptyMap()
        val icons = HashMap<String, String>(chainIds.size)
        chainDao.getChains().forEach { chain ->
            if (chain.chainId in chainIds && chain.iconUrl.isNotBlank()) {
                icons[chain.chainId] = chain.iconUrl
            }
        }
        return icons
    }
}

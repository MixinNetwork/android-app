package one.mixin.android.ui.wallet.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.vo.safe.UnifiedAssetItem
import java.math.BigDecimal
import javax.inject.Inject


@HiltViewModel
class AssetDistributionViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val web3Repository: Web3Repository
) : ViewModel() {

    private val _wallets = MutableStateFlow<List<Web3Wallet>>(emptyList())
    val wallets: StateFlow<List<Web3Wallet>> = _wallets

    fun loadWallets() {
        viewModelScope.launch(Dispatchers.IO) {
            _wallets.value = web3Repository.getAllWallets()
        }
    }

    suspend fun getTokenDistribution(excludeWeb3: Boolean = false): List<AssetDistribution> = withContext(Dispatchers.IO) {
        val walletIds = _wallets.value.filter { it.hasLocalPrivateKey }.map { it.id }
        val web3Tokens = if (excludeWeb3) emptyList() else web3Repository.allWeb3Tokens(walletIds)
        val tokens = tokenRepository.findUnifiedAssetItem()

        val unifiedAssets = mutableListOf<UnifiedAssetItem>()
        unifiedAssets.addAll(tokens)
        unifiedAssets.addAll(web3Tokens)

        val tokensWithValue = unifiedAssets
            .filter { calculateValue(it) > BigDecimal.ZERO }
            .sortedByDescending { calculateValue(it) }

        val totalTokenValue = tokensWithValue.sumOf { calculateValue(it) }

        if (totalTokenValue == BigDecimal.ZERO || tokensWithValue.isEmpty()) {
            return@withContext emptyList()
        }

        when {
            tokensWithValue.size == 1 -> {
                tokensWithValue.map { token ->
                    AssetDistribution(
                        symbol = token.symbol,
                        percentage = 1f,
                        icons = listOfNotNull(token.iconUrl),
                        count = 1
                    )
                }
            }

            tokensWithValue.size == 2 -> {
                val token1 = tokensWithValue[0]
                val token2 = tokensWithValue[1]
                val value1 = calculateValue(token1)

                val p1 = value1.divide(totalTokenValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                val p2 = (1f - p1).coerceIn(0f, 1f)

                listOf(
                    AssetDistribution(
                        symbol = token1.symbol,
                        percentage = p1,
                        icons = listOfNotNull(token1.iconUrl),
                        count = 1
                    ),
                    AssetDistribution(
                        symbol = token2.symbol,
                        percentage = p2,
                        icons = listOfNotNull(token2.iconUrl),
                        count = 1
                    )
                )
            }

            else -> {
                val top2 = tokensWithValue.take(2)
                val others = tokensWithValue.drop(2)

                val top2Distributions = top2.map { token ->
                    val value = calculateValue(token)
                    val percentage = value.divide(totalTokenValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                    AssetDistribution(
                        symbol = token.symbol,
                        percentage = percentage,
                        icons = listOfNotNull(token.iconUrl),
                        count = 1
                    )
                }

                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.take(3).mapNotNull { it.iconUrl }

                top2Distributions + AssetDistribution(
                    symbol = MixinApplication.appContext.getString(R.string.Other),
                    percentage = othersPercentage,
                    icons = othersIcons,
                    count = others.size,
                    isOthers = true
                )
            }
        }
    }

    suspend fun getTokenTotalBalance(excludeWeb3: Boolean = false): BigDecimal = withContext(Dispatchers.IO) {
        val walletIds = _wallets.value.filter { it.hasLocalPrivateKey }.map { it.id }
        val web3Tokens = if (excludeWeb3) emptyList() else web3Repository.allWeb3Tokens(walletIds)
        val tokens = tokenRepository.findUnifiedAssetItem()

        val unifiedAssets = mutableListOf<UnifiedAssetItem>()
        unifiedAssets.addAll(tokens)
        unifiedAssets.addAll(web3Tokens)

        unifiedAssets.sumOf { calculateValue(it) }
    }

    private fun calculateValue(item: UnifiedAssetItem): BigDecimal {
        return try {
            BigDecimal(item.balance).multiply(BigDecimal(item.priceUsd))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    suspend fun getWeb3TokenTotalBalance(walletId: String): BigDecimal = withContext(Dispatchers.IO) {
        val tokens = web3Repository.findUnifiedAssetItem(walletId)
        tokens.sumOf { calculateValue(it) }
    }

    suspend fun getWeb3TokenDistribution(walletId: String): List<AssetDistribution> = withContext(Dispatchers.IO) {
        val tokens = web3Repository.findUnifiedAssetItem(walletId)
        val tokensWithValue = tokens
            .filter { calculateValue(it) > BigDecimal.ZERO }
            .sortedByDescending { calculateValue(it) }
        val totalWeb3Value = tokensWithValue.sumOf { calculateValue(it) }
        if (totalWeb3Value == BigDecimal.ZERO || tokensWithValue.isEmpty()) return@withContext emptyList()
        when {
            tokensWithValue.size == 1 -> tokensWithValue.map { token ->
                AssetDistribution(token.symbol, 1f, listOfNotNull(token.iconUrl), count = 1)
            }
            tokensWithValue.size == 2 -> {
                val (t1, t2) = tokensWithValue
                val p1 = calculateValue(t1).divide(totalWeb3Value, 2, BigDecimal.ROUND_DOWN).toFloat()
                val p2 = (1f - p1).coerceIn(0f, 1f)
                listOf(
                    AssetDistribution(t1.symbol, p1, listOfNotNull(t1.iconUrl), count = 1),
                    AssetDistribution(t2.symbol, p2, listOfNotNull(t2.iconUrl), count = 1)
                )
            }
            else -> {
                val top2 = tokensWithValue.take(2)
                val others = tokensWithValue.drop(2)
                val top2Dist = top2.map { token ->
                    val percentage = calculateValue(token).divide(totalWeb3Value, 2, BigDecimal.ROUND_DOWN).toFloat()
                    AssetDistribution(token.symbol, percentage, listOfNotNull(token.iconUrl), count = 1)
                }
                val othersPercentage = 1f - top2Dist.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.take(3).mapNotNull { it.iconUrl }
                top2Dist + AssetDistribution(
                    symbol = MixinApplication.appContext.getString(R.string.Other),
                    percentage = othersPercentage,
                    icons = othersIcons,
                    count = others.size,
                    isOthers = true
                )
            }
        }
    }
}




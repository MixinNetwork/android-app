package one.mixin.android.ui.wallet.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.repository.TokenRepository
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class AssetDistributionViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val web3TokenDao: Web3TokenDao
) : ViewModel() {

    private val _totalBalance = MutableStateFlow(BigDecimal.ZERO)
    val totalBalance: StateFlow<BigDecimal> = _totalBalance

    private val _tokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val tokenTotalBalance: StateFlow<BigDecimal> = _tokenTotalBalance

    private val _web3TokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val web3TokenTotalBalance: StateFlow<BigDecimal> = _web3TokenTotalBalance

    private val tokenFlow = tokenRepository.assetFlow()

    private val web3TokenFlow = web3TokenDao.web3TokensFlow()

    val wallets: Flow<List<Web3Wallet>> = tokenRepository.getWallets()

    val tokenDistribution: Flow<List<AssetDistribution>> = tokenFlow
        .map { tokens ->
            val tokensWithValue = tokens
                .filter { calculateTokenValue(it) > BigDecimal.ZERO }
                .sortedByDescending { calculateTokenValue(it) }

            val totalTokenValue = tokensWithValue.sumOf { calculateTokenValue(it) }
            _tokenTotalBalance.value = totalTokenValue

            if (totalTokenValue == BigDecimal.ZERO || tokensWithValue.isEmpty()) {
                return@map emptyList()
            }

            when {
                tokensWithValue.size == 1 -> {
                    tokensWithValue.map { token ->
                        AssetDistribution(
                            symbol = token.symbol,
                            percentage = 1f,
                            icons = listOf(token.iconUrl),
                            count = 1
                        )
                    }
                }

                tokensWithValue.size == 2 -> {
                    tokensWithValue.map { token ->
                        val value = calculateTokenValue(token)
                        val percentage = value.divide(totalTokenValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                        AssetDistribution(
                            symbol = token.symbol,
                            percentage = percentage,
                            icons = listOf(token.iconUrl),
                            count = 1
                        )
                    }
                }

                else -> {
                    val top2 = tokensWithValue.take(2)
                    val others = tokensWithValue.drop(2)

                    val top2Distributions = top2.map { token ->
                        val value = calculateTokenValue(token)
                        val percentage = value.divide(totalTokenValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                        AssetDistribution(
                            symbol = token.symbol,
                            percentage = percentage,
                            icons = listOf(token.iconUrl),
                            count = 1
                        )
                    }

                    val othersPercentage = 1f - top2Distributions[0].percentage - top2Distributions[1].percentage
                    val othersIcons = others.take(3).map { it.iconUrl ?: "" }

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val web3TokenDistribution: Flow<List<AssetDistribution>> = web3TokenFlow
        .map { tokens ->
            val tokensWithValue = tokens
                .filter { calculateWeb3TokenValue(it) > BigDecimal.ZERO }
                .sortedByDescending { calculateWeb3TokenValue(it) }

            val totalWeb3Value = tokensWithValue.sumOf { calculateWeb3TokenValue(it) }
            _web3TokenTotalBalance.value = totalWeb3Value

            if (totalWeb3Value == BigDecimal.ZERO || tokensWithValue.isEmpty()) {
                return@map emptyList()
            }

            when {
                tokensWithValue.size == 1 -> {
                    tokensWithValue.map { token ->
                        AssetDistribution(
                            symbol = token.symbol,
                            percentage = 1f,
                            icons = listOf(token.iconUrl),
                            count = 1
                        )
                    }
                }

                tokensWithValue.size == 2 -> {
                    tokensWithValue.map { token ->
                        val value = calculateWeb3TokenValue(token)
                        val percentage = value.divide(totalWeb3Value, 2, BigDecimal.ROUND_DOWN).toFloat()
                        AssetDistribution(
                            symbol = token.symbol,
                            percentage = percentage,
                            icons = listOf(token.iconUrl),
                            count = 1
                        )
                    }
                }

                else -> {
                    val top2 = tokensWithValue.take(2)
                    val others = tokensWithValue.drop(2)

                    val top2Distributions = top2.map { token ->
                        val value = calculateWeb3TokenValue(token)
                        val percentage =
                            value.divide(totalWeb3Value, 2, BigDecimal.ROUND_DOWN).toFloat()
                        AssetDistribution(
                            symbol = token.symbol,
                            percentage = percentage,
                            icons = listOf(token.iconUrl),
                            count = 1
                        )
                    }

                    val othersPercentage = 1f - top2Distributions[0].percentage - top2Distributions[1].percentage
                    val othersIcons = others.take(3).map { it.iconUrl }

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val combinedAssetDistribution: Flow<List<AssetDistribution>> = tokenFlow.combine(web3TokenFlow) { tokens, web3Tokens ->
        val tokenValues = tokens
            .filter { calculateTokenValue(it) > BigDecimal.ZERO }
            .map { 
                AssetValuePair(it.symbol, calculateTokenValue(it), listOf(it.iconUrl ?: ""))
            }
        
        val web3TokenValues = web3Tokens
            .filter { calculateWeb3TokenValue(it) > BigDecimal.ZERO }
            .map { 
                AssetValuePair(it.symbol, calculateWeb3TokenValue(it), listOf(it.iconUrl ?: ""))
            }
        
        val allAssets = (tokenValues + web3TokenValues)
            .sortedByDescending { it.value }
        

        val totalValue = allAssets.sumOf { it.value }
        _totalBalance.value = totalValue

        if (totalValue == BigDecimal.ZERO || allAssets.isEmpty()) {
            return@combine emptyList()
        }

        when {
            allAssets.size == 1 -> {
                allAssets.map { (symbol, value, icons) ->
                    AssetDistribution(symbol, 1f, icons)
                }
            }

            allAssets.size == 2 -> {
                allAssets.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
            }

            else -> {
                val top2 = allAssets.take(2)
                val others = allAssets.drop(2)

                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }

                val othersValue = others.sumOf { it.value }
                val othersPercentage = 1f - top2Distributions[0].percentage - top2Distributions[1].percentage
                val othersIcons = others.take(3).flatMap { it.icons }

                top2Distributions + AssetDistribution(
                    MixinApplication.appContext.getString(R.string.Other),
                    othersPercentage,
                    othersIcons,
                    others.size,
                    isOthers = true
                )
            }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class AssetValuePair(val symbol: String, val value: BigDecimal, val icons: List<String>)

    private fun calculateTokenValue(token: TokenItem): BigDecimal {
        return try {
            BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun calculateWeb3TokenValue(token: Web3Token): BigDecimal {
        return try {
            BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}

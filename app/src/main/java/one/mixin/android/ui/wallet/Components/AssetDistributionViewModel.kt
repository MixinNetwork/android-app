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
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.repository.TokenRepository
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class AssetDistributionViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _totalBalance = MutableStateFlow(BigDecimal.ZERO)
    val totalBalance: StateFlow<BigDecimal> = _totalBalance
    
    private val _tokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val tokenTotalBalance: StateFlow<BigDecimal> = _tokenTotalBalance
    
    private val _web3TokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val web3TokenTotalBalance: StateFlow<BigDecimal> = _web3TokenTotalBalance

    private val tokenFlow = tokenRepository.assetFlow()
    
    private val web3TokenFlow = tokenRepository.web3TokensFlow()
    
    val wallets: Flow<List<Web3Wallet>> = tokenRepository.getWallets()
    
    val top3TokenDistribution: Flow<List<AssetDistribution>> = tokenFlow
        .map { tokens ->
            val totalTokenValue = tokens.sumOf { token ->
                calculateTokenValue(token)
            }
            _tokenTotalBalance.value = totalTokenValue
            
            calculateAssetDistributions(tokens) { token ->
                BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val top3Web3TokenDistribution: Flow<List<AssetDistribution>> = web3TokenFlow
        .map { tokens ->
            val totalWeb3Value = tokens.sumOf { token ->
                calculateWeb3TokenValue(token)
            }
            _web3TokenTotalBalance.value = totalWeb3Value
            
            calculateAssetDistributions(tokens) { token ->
                runCatching { BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd)) }.getOrDefault(BigDecimal.ZERO)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val combinedAssetDistribution: Flow<List<AssetDistribution>> = tokenFlow.combine(web3TokenFlow) { tokens, web3Tokens ->
        val tokenValues = tokens.map { 
            AssetValuePair(it.symbol, calculateTokenValue(it), listOf(it.iconUrl ?: ""))
        }
        
        val web3TokenValues = web3Tokens.map { 
            AssetValuePair(it.symbol, calculateWeb3TokenValue(it), listOf(it.iconUrl ?: ""))
        }
        
        val allAssets = (tokenValues + web3TokenValues)
            .filter { it.value > BigDecimal.ZERO }
            .sortedByDescending { it.value }
            .take(6)
        
        val totalValue = allAssets.sumOf { it.value }
        _totalBalance.value = totalValue
        
        if (totalValue == BigDecimal.ZERO || allAssets.isEmpty()) {
            return@combine emptyList()
        }
        
        when {
            allAssets.size <= 3 -> {
                val distributions = allAssets.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val totalPercentage = distributions.sumOf { it.percentage.toDouble() }.toFloat()
                if (totalPercentage != 1.0f && distributions.isNotEmpty()) {
                    val lastIndex = distributions.lastIndex
                    val adjustedPercentage = distributions[lastIndex].percentage + (1.0f - totalPercentage)
                    val result = distributions.toMutableList()
                    result[lastIndex] = result[lastIndex].copy(percentage = adjustedPercentage)
                    result
                } else {
                    distributions
                }
            }
            allAssets.size == 4 -> {
                val top2 = allAssets.take(2)
                val others = allAssets.drop(2)
                
                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.flatMap { it.icons }
                
                top2Distributions + AssetDistribution(MixinApplication.appContext.getString(R.string.Other), othersPercentage, othersIcons, others.size)
            }
            allAssets.size == 5 -> {
                val top2 = allAssets.take(2)
                val others = allAssets.drop(2)
                
                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.flatMap { it.icons }
                
                top2Distributions + AssetDistribution(MixinApplication.appContext.getString(R.string.Other), othersPercentage, othersIcons, others.size)
            }
            else -> {
                val top2 = allAssets.take(2)
                val others = allAssets.drop(2)
                
                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.take(3).flatMap { it.icons }
                
                top2Distributions + AssetDistribution(MixinApplication.appContext.getString(R.string.Other), othersPercentage, othersIcons, others.size)
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
    
    private fun <T> calculateAssetDistributions(
        tokens: List<T>, 
        valueCalculator: (T) -> BigDecimal
    ): List<AssetDistribution> {
        val tokenValues = tokens.map { token ->
            val symbol = when (token) {
                is TokenItem -> token.symbol
                is Web3Token -> token.symbol
                else -> ""
            }
            val icon = when (token) {
                is TokenItem -> token.iconUrl
                is Web3Token -> token.iconUrl
                else -> ""
            }
            AssetValuePair(symbol, valueCalculator(token), listOf(icon ?: ""))
        }
        .filter { it.value > BigDecimal.ZERO }
        .sortedByDescending { it.value }
        .take(6)
        
        val totalValue = tokenValues.sumOf { it.value }
        
        if (totalValue == BigDecimal.ZERO || tokenValues.isEmpty()) {
            return emptyList()
        }
        
        return when {
            tokenValues.size <= 3 -> {
                var distributions = tokenValues.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val totalPercentage = distributions.sumOf { it.percentage.toDouble() }.toFloat()
                if (totalPercentage != 1.0f && distributions.isNotEmpty()) {
                    val lastIndex = distributions.lastIndex
                    val adjustedPercentage = distributions[lastIndex].percentage + (1.0f - totalPercentage)
                    val result = distributions.toMutableList()
                    result[lastIndex] = result[lastIndex].copy(percentage = adjustedPercentage)
                    result
                } else {
                    distributions
                }
            }
            tokenValues.size == 4 -> {
                val top2 = tokenValues.take(2)
                val others = tokenValues.drop(2)
                
                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.flatMap { it.icons }
                
                top2Distributions + AssetDistribution(MixinApplication.appContext.getString(R.string.Other), othersPercentage, othersIcons, others.size)
            }
            tokenValues.size == 5 -> {
                val top2 = tokenValues.take(2)
                val others = tokenValues.drop(2)
                
                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.flatMap { it.icons }
                
                top2Distributions + AssetDistribution(MixinApplication.appContext.getString(R.string.Other), othersPercentage, othersIcons, others.size)
            }
            else -> {
                val top2 = tokenValues.take(2)
                val others = tokenValues.drop(2)
                
                val top2Distributions = top2.map { (symbol, value, icons) ->
                    val percentage = value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
                    AssetDistribution(symbol, percentage, icons)
                }
                
                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.take(3).flatMap { it.icons }
                
                top2Distributions + AssetDistribution(MixinApplication.appContext.getString(R.string.Other), othersPercentage, othersIcons, others.size)
            }
        }
    }
}

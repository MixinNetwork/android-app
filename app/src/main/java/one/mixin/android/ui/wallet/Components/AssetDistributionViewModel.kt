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
    private val web3WalletDao: Web3WalletDao
) : ViewModel() {

    private val _totalBalance = MutableStateFlow(BigDecimal.ZERO)
    val totalBalance: StateFlow<BigDecimal> = _totalBalance
    
    private val _tokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val tokenTotalBalance: StateFlow<BigDecimal> = _tokenTotalBalance
    
    private val _web3TokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val web3TokenTotalBalance: StateFlow<BigDecimal> = _web3TokenTotalBalance

    private val tokenFlow = tokenRepository.assetFlow()
    
    private val web3TokenFlow = tokenRepository.web3TokensFlow()
    
    val wallets: Flow<List<Web3Wallet>> = web3WalletDao.getWallets()
    
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
            AssetValuePair(it.symbol, calculateTokenValue(it), it.iconUrl)
        }
        
        val web3TokenValues = web3Tokens.map { 
            AssetValuePair(it.symbol, calculateWeb3TokenValue(it), it.iconUrl)
        }
        
        val allAssets = (tokenValues + web3TokenValues).sortedByDescending { it.value }
        
        val totalValue = allAssets.sumOf { it.value }
        _totalBalance.value = totalValue
        
        if (totalValue == BigDecimal.ZERO) {
            return@combine emptyList()
        }
        
        val distributions = allAssets.map { (symbol, value, icon) ->
            val percentage = if (totalValue > BigDecimal.ZERO) {
                value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
            } else {
                0f
            }
            AssetDistribution(symbol, percentage, icon)
        }
        
        if (distributions.size <= 3) {
            distributions
        } else {
            val top2 = distributions.take(2)
            val otherPercentage = 1f - top2.sumOf { it.percentage.toDouble() }.toFloat()
            // Todo
            top2 + AssetDistribution("Other", otherPercentage, null)
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private data class AssetValuePair(val symbol: String, val value: BigDecimal, val icon: String?)
    
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
            AssetValuePair(symbol, valueCalculator(token), icon)
        }.sortedByDescending { it.value }
        
        val totalValue = tokenValues.sumOf { it.value }
        
        if (totalValue == BigDecimal.ZERO) {
            return emptyList()
        }
        
        return tokenValues.map { (symbol, value, icon) ->
            val percentage = if (totalValue > BigDecimal.ZERO) {
                value.divide(totalValue, 4, BigDecimal.ROUND_HALF_UP).toFloat()
            } else {
                0f
            }
            AssetDistribution(symbol, percentage, icon)
        }.take(3)
    }
}

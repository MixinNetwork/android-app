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
import one.mixin.android.api.response.Web3Token
import one.mixin.android.repository.TokenRepository
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _totalBalance = MutableStateFlow(BigDecimal.ZERO)
    val totalBalance: StateFlow<BigDecimal> = _totalBalance
    
    // 常规代币总余额
    private val _tokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val tokenTotalBalance: StateFlow<BigDecimal> = _tokenTotalBalance
    
    // Web3 代币总余额
    private val _web3TokenTotalBalance = MutableStateFlow(BigDecimal.ZERO)
    val web3TokenTotalBalance: StateFlow<BigDecimal> = _web3TokenTotalBalance

    // 获取常规代币资产流
    private val tokenFlow = tokenRepository.assetFlow()
    
    // 获取 Web3 代币资产流
    private val web3TokenFlow = tokenRepository.web3TokensFlow()
    
    // 计算常规代币的前三值钱资产
    val top3TokenDistribution: Flow<List<AssetDistribution>> = tokenFlow
        .map { tokens ->
            // 计算常规代币总余额
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
    
    // 计算 Web3 代币的前三值钱资产
    val top3Web3TokenDistribution: Flow<List<AssetDistribution>> = web3TokenFlow
        .map { tokens ->
            // 计算 Web3 代币总余额
            val totalWeb3Value = tokens.sumOf { token ->
                calculateWeb3TokenValue(token)
            }
            _web3TokenTotalBalance.value = totalWeb3Value
            
            calculateAssetDistributions(tokens) { token ->
                BigDecimal(token.balance).multiply(BigDecimal(token.price))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 合并两种资产，计算总价值和总体分布
    val combinedAssetDistribution: Flow<List<AssetDistribution>> = tokenFlow.combine(web3TokenFlow) { tokens, web3Tokens ->
        val tokenValues = tokens.map { 
            AssetValuePair(it.symbol, calculateTokenValue(it))
        }
        
        val web3TokenValues = web3Tokens.map { 
            AssetValuePair(it.symbol, calculateWeb3TokenValue(it))
        }
        
        // 合并所有资产并按价值排序
        val allAssets = (tokenValues + web3TokenValues).sortedByDescending { it.value }
        
        // 计算总价值
        val totalValue = allAssets.sumOf { it.value }
        _totalBalance.value = totalValue
        
        if (totalValue == BigDecimal.ZERO) {
            return@combine emptyList()
        }
        
        // 计算每个资产的百分比
        val distributions = allAssets.map { (symbol, value) ->
            val percentage = if (totalValue > BigDecimal.ZERO) {
                value.divide(totalValue, 8, BigDecimal.ROUND_HALF_UP).toFloat()
            } else {
                0f
            }
            AssetDistribution(symbol, percentage)
        }
        
        // 处理少于等于3个资产的情况
        if (distributions.size <= 3) {
            distributions
        } else {
            // 取前两个资产，计算剩余资产的总百分比
            val top2 = distributions.take(2)
            val otherPercentage = 1f - top2.sumOf { it.percentage.toDouble() }.toFloat()
            top2 + AssetDistribution("Other", otherPercentage)
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // 辅助数据类，用于存储资产符号和价值
    private data class AssetValuePair(val symbol: String, val value: BigDecimal)
    
    // 计算常规代币的价值
    private fun calculateTokenValue(token: TokenItem): BigDecimal {
        return try {
            BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
    
    // 计算 Web3 代币的价值
    private fun calculateWeb3TokenValue(token: Web3Token): BigDecimal {
        return try {
            BigDecimal(token.balance).multiply(BigDecimal(token.price))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
    
    // 通用方法，计算资产分布
    private fun <T> calculateAssetDistributions(
        tokens: List<T>, 
        valueCalculator: (T) -> BigDecimal
    ): List<AssetDistribution> {
        // 计算每个代币的价值
        val tokenValues = tokens.map { token ->
            val symbol = when (token) {
                is TokenItem -> token.symbol
                is Web3Token -> token.symbol
                else -> ""
            }
            AssetValuePair(symbol, valueCalculator(token))
        }.sortedByDescending { it.value }
        
        // 计算总价值
        val totalValue = tokenValues.sumOf { it.value }
        
        if (totalValue == BigDecimal.ZERO) {
            return emptyList()
        }
        
        // 计算每个资产的百分比
        return tokenValues.map { (symbol, value) ->
            val percentage = if (totalValue > BigDecimal.ZERO) {
                value.divide(totalValue, 4, BigDecimal.ROUND_HALF_UP).toFloat()
            } else {
                0f
            }
            AssetDistribution(symbol, percentage)
        }.take(3) // 只取前三个
    }
}

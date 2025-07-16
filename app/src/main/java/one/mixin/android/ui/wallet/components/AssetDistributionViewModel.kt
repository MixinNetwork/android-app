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
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.repository.TokenRepository
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import javax.inject.Inject

import one.mixin.android.repository.Web3Repository
import one.mixin.android.db.web3.vo.toWeb3TokenItem

@HiltViewModel
class AssetDistributionViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val web3Repository: Web3Repository,
    private val web3TokenDao: Web3TokenDao,
    private val web3WalletDao: Web3WalletDao,
) : ViewModel() {

    private val _wallets = MutableStateFlow<List<Web3Wallet>>(emptyList())
    val wallets: StateFlow<List<Web3Wallet>> = _wallets

    init {
        loadWallets()
    }

    private fun loadWallets() {
        viewModelScope.launch(Dispatchers.IO) {
            web3Repository.getWallets().collect {
                _wallets.value = it
            }
        }
    }

    suspend fun getAddressesByWalletId(walletId: String) = withContext(Dispatchers.IO) {
        web3Repository.getAddressesByWalletId(walletId)
    }

    suspend fun getTokenDistribution(excludeWeb3: Boolean = false): List<AssetDistribution> = withContext(Dispatchers.IO) {
        val walletIds = _wallets.value.filter { it.hasLocalPrivateKey }.map { it.id }
        val web3Tokens = if (excludeWeb3) emptyList() else web3Repository.allWeb3Tokens(walletIds)
        val tokens = tokenRepository.findAssetItemsWithBalance()

        val unifiedTokens = mutableListOf<UnifiedToken>()

        tokens.forEach { token ->
            val value = calculateTokenValue(token)
            if (value > BigDecimal.ZERO) {
                unifiedTokens.add(UnifiedToken(token.symbol, token.iconUrl, value))
            }
        }

        web3Tokens.forEach { web3Token ->
            val web3TokenItem = web3Token.toWeb3TokenItem() // Convert to Web3TokenItem
            val value = calculateWeb3TokenValue(web3TokenItem) // Now it's Web3TokenItem
            if (value > BigDecimal.ZERO) {
                unifiedTokens.add(UnifiedToken(web3TokenItem.symbol, web3TokenItem.iconUrl, value))
            }
        }

        val tokensWithValue = unifiedTokens.sortedByDescending { it.value }

        val totalTokenValue = tokensWithValue.sumOf { it.value }

        if (totalTokenValue == BigDecimal.ZERO || tokensWithValue.isEmpty()) {
            return@withContext emptyList()
        }

        when {
            tokensWithValue.size == 1 -> {
                tokensWithValue.map { token ->
                    AssetDistribution(
                        symbol = token.symbol,
                        percentage = 1f,
                        icons = listOfNotNull(token.iconUrl), // Handle nullability
                        count = 1
                    )
                }
            }

            tokensWithValue.size == 2 -> {
                val token1 = tokensWithValue[0]
                val token2 = tokensWithValue[1]
                val value1 = token1.value

                val p1 = value1.divide(totalTokenValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                val p2 = (1f - p1).coerceIn(0f, 1f)

                listOf(
                    AssetDistribution(
                        symbol = token1.symbol,
                        percentage = p1,
                        icons = listOfNotNull(token1.iconUrl), // Handle nullability
                        count = 1
                    ),
                    AssetDistribution(
                        symbol = token2.symbol,
                        percentage = p2,
                        icons = listOfNotNull(token2.iconUrl), // Handle nullability
                        count = 1
                    )
                )
            }

            else -> {
                val top2 = tokensWithValue.take(2)
                val others = tokensWithValue.drop(2)

                val top2Distributions = top2.map { token ->
                    val value = token.value
                    val percentage = value.divide(totalTokenValue, 2, BigDecimal.ROUND_DOWN).toFloat()
                    AssetDistribution(
                        symbol = token.symbol,
                        percentage = percentage,
                        icons = listOfNotNull(token.iconUrl), // Handle nullability
                        count = 1
                    )
                }

                val othersPercentage = 1f - top2Distributions.sumOf { it.percentage.toDouble() }.toFloat()
                val othersIcons = others.take(3).mapNotNull { it.iconUrl } // Handle nullability

                top2Distributions + AssetDistribution(
                    symbol = MixinApplication.appContext.getString(R.string.Other),
                    percentage = othersPercentage,
                    icons = othersIcons, // Already filtered for nulls
                    count = others.size,
                    isOthers = true
                )
            }
        }
    }

    suspend fun getTokenTotalBalance(excludeWeb3: Boolean = false): BigDecimal = withContext(Dispatchers.IO) {
        val walletIds = _wallets.value.filter { it.hasLocalPrivateKey }.map { it.id }
        val web3Tokens = if (excludeWeb3) emptyList() else web3Repository.allWeb3Tokens(walletIds)
        val tokens = tokenRepository.findAssetItemsWithBalance()

        val totalMixinTokenValue = tokens.sumOf { calculateTokenValue(it) }
        val totalWeb3TokenValue = web3Tokens.sumOf { calculateWeb3TokenValue(it.toWeb3TokenItem()) } // Convert and calculate

        totalMixinTokenValue.add(totalWeb3TokenValue)
    }

    private fun calculateTokenValue(token: TokenItem): BigDecimal {
        return try {
            BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun calculateWeb3TokenValue(token: Web3TokenItem): BigDecimal {
        return try {
            BigDecimal(token.balance).multiply(BigDecimal(token.priceUsd))
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    suspend fun getWeb3TokenTotalBalance(walletId: String): BigDecimal = withContext(Dispatchers.IO) {
        val tokens = web3TokenDao.findWeb3TokenItems(walletId)
        tokens.sumOf { calculateWeb3TokenValue(it) }
    }

    suspend fun getWeb3TokenDistribution(walletId: String): List<AssetDistribution> = withContext(Dispatchers.IO) {
        val tokens = web3TokenDao.findWeb3TokenItems(walletId)
        val tokensWithValue = tokens
            .filter { calculateWeb3TokenValue(it) > BigDecimal.ZERO }
            .sortedByDescending { calculateWeb3TokenValue(it) }
        val totalWeb3Value = tokensWithValue.sumOf { calculateWeb3TokenValue(it) }
        if (totalWeb3Value == BigDecimal.ZERO || tokensWithValue.isEmpty()) return@withContext emptyList()
        when {
            tokensWithValue.size == 1 -> tokensWithValue.map { token ->
                AssetDistribution(token.symbol, 1f, listOfNotNull(token.iconUrl), count = 1)
            }
            tokensWithValue.size == 2 -> {
                val (t1, t2) = tokensWithValue
                val p1 = calculateWeb3TokenValue(t1).divide(totalWeb3Value, 2, BigDecimal.ROUND_DOWN).toFloat()
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
                    val percentage = calculateWeb3TokenValue(token).divide(totalWeb3Value, 2, BigDecimal.ROUND_DOWN).toFloat()
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

data class UnifiedToken(
    val symbol: String,
    val iconUrl: String?,
    val value: BigDecimal
)

package one.mixin.android.ui.home.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.market.HistoryPrice
import one.mixin.android.vo.market.Price
import timber.log.Timber

sealed class Result<out T> {
    data object Loading : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}

@Composable
fun Market(assetId: String, isPositive: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = hiltViewModel<WalletViewModel>()
    val liveData = viewModel.historyPriceById(assetId)
    val historyPrices =
        remember {
            mutableStateOf<HistoryPrice?>(null)
        }
    DisposableEffect(assetId, lifecycleOwner) {
        val observer =
            Observer<HistoryPrice?> {
                historyPrices.value = it
            }
        liveData.observe(lifecycleOwner, observer)
        onDispose { liveData.removeObserver(observer) }
    }
    if (historyPrices.value != null) {
        val data = historyPrices.value!!.data.map {
            it.price.toFloat()
        }
        LineChart(data, isPositive)
    }
}

@Composable
fun Market(type: String, assetId: String, isPositive: Boolean, onHighlightChange: (String?, Float?) -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<WalletViewModel>()
    var responseState by remember { mutableStateOf<Result<List<Price>>>(Result.Loading) }

    LaunchedEffect(type, assetId) {
        responseState = Result.Loading
        responseState = try {
            val data = viewModel.priceHistory(assetId, type)
            if (data.isSuccess) {
                Result.Success(data.data!!.data)
            } else {
                Result.Error(Exception(context.getMixinErrorStringByCode(data.errorCode, data.errorDescription)))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val response = responseState) {
            is Result.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MixinAppTheme.colors.accent,
                )
            }

            is Result.Success -> {
                if (response.data.isEmpty()) {
                    MixinAppTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFD9D9D9), Color(0x33D9D9D9)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.Price_data_unavailable), color = MixinAppTheme.colors.textRemarks)
                        }
                    }
                } else {
                    val prices = response.data.map { it.price.toFloat() }
                    val time = response.data.map { it.unix }
                    LineChart(prices, isPositive, time, type) { index ->
                        Timber.e("on $index")
                        if (index < 0 || index >= prices.size) {
                            onHighlightChange.invoke(null, null)
                            return@LineChart
                        }
                        val currentPrice = prices[index]
                        val basePrice = prices.first()
                        if (basePrice != 0f) {
                            val percentageChange = ((currentPrice - basePrice) / basePrice) * 100
                            onHighlightChange.invoke(currentPrice.toString(), percentageChange)
                        } else {
                            onHighlightChange.invoke(currentPrice.toString(), null)
                        }
                    }
                }
            }

            is Result.Error -> {
                MixinAppTheme {
                    Box(modifier = Modifier
                        .wrapContentSize()
                        .padding(20.dp)) {
                        Text(text = response.exception.message ?: stringResource(R.string.Unknown), color = MixinAppTheme.colors.textPrimary)
                    }
                }
            }
        }
    }
}


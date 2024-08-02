package one.mixin.android.ui.home.market

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.WalletViewModel

sealed class Result<out T> {
    data object Loading : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}

@Composable
fun Market(type: String, assetId: String, isPositive: Boolean) {
    val viewModel = hiltViewModel<WalletViewModel>()
    var responseState by remember { mutableStateOf<Result<List<Float>>>(Result.Loading) }

    LaunchedEffect(type, assetId) {
        responseState = Result.Loading
        responseState = try {
            val data = viewModel.priceHistory(assetId, type)
            if (data.isSuccess) {
                Result.Success(data.data!!.map { it.price.toFloat() })
            } else {
                Result.Error(Exception(data.errorDescription))
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
                LineChart(response.data, isPositive, true)
            }

            is Result.Error -> {
                Text(text = "Error: ${response.exception.message}")
            }
        }
    }
}


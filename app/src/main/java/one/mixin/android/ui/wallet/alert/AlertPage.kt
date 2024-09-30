package one.mixin.android.ui.wallet.alert

import PageScaffold
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.AlertGroupItem
import one.mixin.android.ui.wallet.alert.components.AssetFilter
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertGroup
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertType
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest
import one.mixin.android.ui.wallet.alert.vo.CoinItem
import java.util.Locale

@Composable
fun AlertPage(coin: CoinItem, pop: () -> Unit, onAdd: () -> Unit, toAll: () -> Unit, onEdit: (Alert) -> Unit) {
    val viewModel = hiltViewModel<AlertViewModel>()
    var alertGroup by remember { mutableStateOf<AlertGroup?>(null) }

    LaunchedEffect(coin) {
        viewModel.alertGroup(coin.coinId).collect { groups ->
            alertGroup = groups
        }
    }

    PageScaffold(
        title = stringResource(id = R.string.Alert),
        verticalScrollable = false,
        pop = pop,
        actions = {
            TextButton(
                onClick = toAll,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            ) {
                Text(
                    stringResource(id = R.string.All),
                    color = MixinAppTheme.colors.accent,
                )
            }
        }
    ) {
        if (alertGroup == null) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Image(painter = painterResource(R.drawable.ic_empty_file), contentDescription = null)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = stringResource(R.string.NO_ALERTS), color = MixinAppTheme.colors.textRemarks)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = Modifier
                            .height(48.dp)
                            .align(alignment = Alignment.CenterHorizontally),
                        onClick = onAdd,
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MixinAppTheme.colors.accent
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.Add_Alert),
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

            }
        } else {
            Column (
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(2.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
                    AlertGroupItem(alertGroup!!, true, onEdit = onEdit)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier
                        .height(48.dp)
                        .align(alignment = Alignment.CenterHorizontally),
                    onClick = onAdd,
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation = ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.Add_Alert),
                            color = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}


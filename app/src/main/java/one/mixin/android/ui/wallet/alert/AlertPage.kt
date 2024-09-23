package one.mixin.android.ui.wallet.alert

import PageScaffold
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.AlertGroupItem
import one.mixin.android.ui.wallet.alert.components.AssetFilter
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertAction
import one.mixin.android.ui.wallet.alert.vo.AlertGroup
import one.mixin.android.vo.safe.TokenItem

@Composable
fun AlertPage(assets: List<TokenItem>?, openFilter: () -> Unit, pop: () -> Unit, to: () -> Unit, onAction: (AlertAction, Alert) -> Unit) {
    val viewModel = hiltViewModel<AlertViewModel>()
    var alertGroups by remember { mutableStateOf(emptyList<AlertGroup>()) }

    LaunchedEffect(assets) {
        if (assets.isNullOrEmpty()) {
            viewModel.alertGroups().collect { groups ->
                alertGroups = groups
            }
        } else {
            viewModel.alertGroups(assets.map { it.assetId }).collect { groups ->
                alertGroups = groups
            }
        }
    }

    PageScaffold(
        title = stringResource(id = R.string.Alert),
        verticalScrollable = false,
        pop = pop,
    ) {
        if (alertGroups.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetFilter(assets, openFilter)
                    TextButton(
                        onClick = to,
                        modifier = Modifier.wrapContentSize(),
                    ) {
                        Image(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_alert_add),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.Add_Alert), color = Color(0xFF3D75E3)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(painter = painterResource(R.drawable.ic_empty_file), contentDescription = null)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = stringResource(R.string.NO_ALERTS), color = MixinAppTheme.colors.textRemarks)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetFilter(assets, openFilter)
                        TextButton(
                            onClick = to,
                            modifier = Modifier.wrapContentSize(),
                        ) {
                            Image(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(R.drawable.ic_alert_add),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.Add_Alert), color = Color(0xFF3D75E3)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(alertGroups.size) { index ->
                    val group = alertGroups[index]
                    if (index!=0) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    AlertGroupItem(group, index == 0, onAction = onAction)
                }
            }
        }
    }
}


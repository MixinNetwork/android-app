package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.localTime
import one.mixin.android.ui.setting.PinLogsFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.vo.LogResponse
import timber.log.Timber

@Composable
fun PinLogsPage() {
    SettingPageScaffold(
        title = stringResource(id = R.string.Logs),
        verticalScrollable = false,
    ) {
        val viewModel = hiltViewModel<SettingViewModel>()

        var logs by remember {
            mutableStateOf(listOf<LogResponse>())
        }

        var isLoading by remember {
            mutableStateOf(false)
        }

        var hasMore by remember {
            mutableStateOf(true)
        }

        val scope = rememberCoroutineScope()

        suspend fun loadMore() {
            if (isLoading || !hasMore) return
            isLoading = true
            Timber.d("loadMore")
            handleMixinResponse(
                invokeNetwork = {
                    viewModel.getPinLogs(logs.lastOrNull()?.createdAt)
                },
                successBlock = {
                    val data = it.data ?: emptyList()
                    hasMore = data.isNotEmpty()
                    logs = logs + data
                },
                defaultErrorHandle = {
                    hasMore = false
                },
            )
            isLoading = false
        }

        LaunchedEffect(true) {
            loadMore()
        }

        if (logs.isEmpty()) {
            if (isLoading) {
                Loading()
            } else {
                EmptyLayout()
            }
        } else {
            LogsList(
                logs = logs,
                loadMore = {
                    scope.launch {
                        loadMore()
                    }
                },
            )
        }
    }
}

@Composable
private fun LogsList(
    logs: List<LogResponse>,
    loadMore: () -> Unit,
) {
    val state = rememberLazyListState()

    val isLastItemVisible =
        with(state.layoutInfo) {
            visibleItemsInfo.lastOrNull()?.index == totalItemsCount - 1
        }

    if (isLastItemVisible) {
        loadMore()
    }

    LazyColumn(state = state) {
        items(logs) { log ->
            LogItem(log = log)
        }
    }
}

@Composable
private fun LogItem(log: LogResponse) {
    val context = LocalContext.current

    val description =
        remember(log.code) {
            PinLogsFragment.getLogDescription(context, log.code)
        }

    Column(
        modifier =
            Modifier
                .background(MixinAppTheme.colors.background)
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = description.first,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = log.createdAt.localTime(),
                fontSize = 12.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp),
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
        Box(modifier = Modifier.height(6.dp))

        Text(
            text = description.second,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )

        Box(modifier = Modifier.height(6.dp))

        Text(
            text = log.ipAddress,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun Loading() {
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MixinAppTheme.colors.accent,
        )
    }
}

@Composable
private fun EmptyLayout() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.height(120.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_pin_empty),
            contentDescription = null,
        )
        Box(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.No_logs),
            color = MixinAppTheme.colors.textSubtitle,
        )
    }
}

@Composable
@Preview
fun PreviewEmptyLayout() {
    MixinAppTheme {
        SettingPageScaffold(
            title = stringResource(id = R.string.Logs),
            verticalScrollable = false,
        ) {
            EmptyLayout()
        }
    }
}

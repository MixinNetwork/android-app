package one.mixin.android.ui.logs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.compose.GetNavBarHeightValue
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.toast
import java.io.File

@Composable
fun LogViewerScreen(
    viewModel: LogsViewModel = viewModel(),
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.loadPreLoginLogs()
    }

    MixinAppTheme {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = GetNavBarHeightValue())
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.Logs),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W700,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    modifier = Modifier.clickable {
                        onNavigateUp.invoke()
                    },
                    painter = painterResource(id = R.drawable.ic_circle_close),
                    tint = Color.Unspecified,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
            Scaffold(
                bottomBar = {
                    val state = uiState
                    if (state is LogUiState.Success && state.content.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MixinAppTheme.colors.background)
                                .padding(vertical = 24.dp, horizontal = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(30.dp)
                        ) {
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor =  MixinAppTheme.colors.backgroundGrayLight
                                ),
                                onClick = {
                                    try {
                                        val logFile = File(context.cacheDir, "mixin_pre_login.log")
                                        logFile.writeText(state.content)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            BuildConfig.APPLICATION_ID + ".provider",
                                            logFile
                                        )
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            type = "text/plain"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, context.getString(R.string.Share))
                                        )
                                    } catch (e: Exception) {
                                        toast(e.message ?: "Error sharing log file")
                                    }
                                },
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(text = stringResource(R.string.Share), color = MixinAppTheme.colors.accent)
                            }
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor =  MixinAppTheme.colors.accent
                                ),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.content))
                                    toast(R.string.copied_to_clipboard)
                                },
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(text = stringResource(R.string.Copy), color = Color.White)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MixinAppTheme.colors.background)
                                .padding(vertical = 24.dp, horizontal = 32.dp)
                        ) {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                onClick = onNavigateUp,
                                shape = RoundedCornerShape(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor =  MixinAppTheme.colors.accent
                                ),
                            ) {
                                Text(text = stringResource(R.string.Done), color = Color.White)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MixinAppTheme.colors.backgroundWindow)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = uiState) {
                        is LogUiState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is LogUiState.Error -> {
                            Text(
                                text = state.message,
                                color = MixinAppTheme.colors.tipError,
                                style = MaterialTheme.typography.body1
                            )
                        }
                        is LogUiState.Success -> {
                            if (state.content.isBlank()) {
                                Text("Log file is empty.")
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    items(state.content.lines()) { line ->
                                        Text(
                                            text = line,
                                            color = MixinAppTheme.colors.textPrimary,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

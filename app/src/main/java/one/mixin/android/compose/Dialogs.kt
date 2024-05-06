package one.mixin.android.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.compose.theme.MixinAppTheme

@Suppress("DEPRECATION")
@Composable
fun IndeterminateProgressDialog(
    message: String = "",
    title: String = "",
    cancelable: Boolean? = null,
) {
    val context = LocalContext.current

    val activity = context.findFragmentActivityOrNull()

    val progressDialog =
        remember {
            activity?.indeterminateProgressDialog()
        }

    SideEffect {
        progressDialog?.setTitle(title)
        progressDialog?.setMessage(message)

        if (cancelable != null) {
            progressDialog?.setCancelable(cancelable)
        }
    }

    DisposableEffect(progressDialog) {
        progressDialog?.show()
        onDispose {
            progressDialog?.dismiss()
        }
    }
}

@Composable
fun MixinAlertDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit = onDismissRequest,
    confirmText: String,
    dismissText: String? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                1.5.dp,
            ),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MixinAppTheme.colors.accent,
                    ),
            ) {
                Text(
                    text = confirmText,
                    style = TextStyle.Default,
                )
            }
        },
        title = title,
        text = text,
        dismissButton =
            dismissText?.let {
                {
                    TextButton(
                        onClick = onDismissClick,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MixinAppTheme.colors.textPrimary,
                            ),
                    ) {
                        Text(
                            text = it,
                            style = TextStyle.Default,
                        )
                    }
                }
            },
        backgroundColor = MixinAppTheme.colors.background,
        contentColor = MixinAppTheme.colors.textPrimary,
    )
}

@Composable
@Preview
fun PreviewMixinAlertDialog() {
    MixinAppTheme {
        MixinAlertDialog(
            onDismissRequest = {
            },
            onConfirmClick = {
            },
            title = {
                Text("Title")
            },
            text = {
                Text("Text")
            },
            confirmText = "Confirm",
            dismissText = "Dismiss",
        )
    }
}

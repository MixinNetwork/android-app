package one.mixin.android.compose

import android.view.View
import android.view.Window
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_SECURE
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import one.mixin.android.MixinApplication
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.util.FlagSecureReason

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
    val hostView = LocalView.current
    AlertDialog(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                1.5.dp,
            ),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TrackAppLockDialogWindow(hostView)
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
private fun TrackAppLockDialogWindow(hostView: View) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val appAuth = MixinApplication.appAuthOrNull() ?: return
    DisposableEffect(window, hostView, appAuth) {
        val originalCallback = window.callback
        val secureCallback =
            originalCallback?.let { callback ->
                object : Window.Callback by callback {
                    override fun onWindowAttributesChanged(attrs: LayoutParams) {
                        // Compose reapplies the host's secure flag when the dialog recomposes.
                        val hostFlags = (hostView.rootView.layoutParams as? LayoutParams)?.flags ?: 0
                        if (FlagSecureReason.isSecured(window) || (hostFlags and FLAG_SECURE) != 0) {
                            attrs.flags = attrs.flags or FLAG_SECURE
                        }
                        callback.onWindowAttributesChanged(attrs)
                    }
                }
            }
        if (secureCallback != null) window.callback = secureCallback
        val reason = appAuth.attachSecureWindow(window)
        onDispose {
            if (window.callback === secureCallback) window.callback = originalCallback
            appAuth.detachSecureWindow(reason)
        }
    }
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

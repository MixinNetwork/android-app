package one.mixin.android.ui.setting.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.indeterminateProgressDialog

@Suppress("DEPRECATION")
@Composable
fun IndeterminateProgressDialog(
    message: String = "",
    title: String = ""
) {

    val context = LocalContext.current

    val activity = context.findFragmentActivityOrNull()

    val progressDialog = remember {
        activity?.indeterminateProgressDialog()
    }

    SideEffect {
        progressDialog?.setTitle(title)
        progressDialog?.setMessage(message)
    }

    DisposableEffect(progressDialog) {
        progressDialog?.show()
        onDispose {
            progressDialog?.dismiss()
        }
    }
}

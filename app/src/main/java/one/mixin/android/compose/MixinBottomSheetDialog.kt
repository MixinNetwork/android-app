package one.mixin.android.compose

import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import one.mixin.android.R
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

@Composable
fun MixinBottomSheetDialog(
    createDialog: () -> MixinBottomSheetDialogFragment,
    tag: String? = null,
) {
    val dialog =
        remember {
            createDialog().apply {
            }
        }

    val context = LocalContext.current

    DisposableEffect(dialog) {
        context.findFragmentActivityOrNull()?.let {
            dialog.showNow(it.supportFragmentManager, tag)
        }
        onDispose {
            dialog.dismiss()
        }
    }
}

@Composable
fun MixinBottomSheetDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    val composition = rememberCompositionContext()

    val currentContent by rememberUpdatedState(content)

    val dialog =
        remember(view) {
            MixinBottomSheetDialogWrapper(
                view,
                parent = composition,
                content = {
                    Box(Modifier.semantics { dialog() }) {
                        currentContent()
                    }
                },
                onDismissRequest = onDismissRequest,
            )
        }

    DisposableEffect(dialog) {
        dialog.show()
        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    SideEffect {
        dialog.updateParameters(onDismissRequest = onDismissRequest)
    }
}

private class MixinBottomSheetDialogWrapper(
    composeView: View,
    content: @Composable () -> Unit,
    parent: CompositionContext,
    private var onDismissRequest: () -> Unit,
) {
    private val dialog =
        BottomSheet.Builder(
            ContextThemeWrapper(composeView.context, R.style.AppTheme_Dialog),
            needFocus = true,
            softInputResize = true,
        ).create()

    private val dialogContentView =
        ComposeView(composeView.context).apply {
            setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
            this.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
            setViewTreeSavedStateRegistryOwner(
                composeView.findViewTreeSavedStateRegistryOwner(),
            )

            setParentCompositionContext(parent)
            setContent(content)
            createComposition()
        }

    init {
        dialog.setCustomView(dialogContentView)
        dialog.setOnDismissListener {
            onDismissRequest()
        }
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun disposeComposition() {
        dialogContentView.disposeComposition()
    }

    fun updateParameters(onDismissRequest: () -> Unit) {
        this.onDismissRequest = onDismissRequest
    }
}

package one.mixin.android.ui.setting.ui.compose

import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import one.mixin.android.R
import one.mixin.android.widget.BottomSheet

@Composable
fun MixinBottomSheetDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    val composition = rememberCompositionContext()

    val currentContent by rememberUpdatedState(content)

    val dialog = remember(view) {
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
            softInputResize = true
        ).create()

    private val dialogContentView = ComposeView(composeView.context).apply {

        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
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

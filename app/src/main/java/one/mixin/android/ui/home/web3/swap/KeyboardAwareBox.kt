package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Surface
import androidx.compose.ui.Alignment
import timber.log.Timber

@Composable
fun KeyboardAwareBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    floating: @Composable BoxScope.() -> Unit
) {
    var isKeyboardVisible by remember { mutableStateOf(false) }
    val view = LocalView.current

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            isKeyboardVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true

            Timber.e("isKeyboardVisible: $isKeyboardVisible")
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }


    Box(modifier = modifier) {
        content()

        if (isKeyboardVisible) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .imePadding(),
            ) {
                floating()
            }
        }
    }
}
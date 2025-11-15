package one.mixin.android.ui.home.web3.trade

import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import timber.log.Timber

@Composable
fun KeyboardAwareBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    floating: @Composable BoxScope.() -> Unit,
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
            val density = LocalDensity.current
            val imeBottom = with(density) {
                WindowInsets.ime.getBottom(density).toDp()
            }
            val systemBarsBottom = with(density) {
                WindowInsets.systemBars.getBottom(density).toDp()

            }
            Timber.e("imeBottom: $imeBottom, systemBarsBottom: $systemBarsBottom")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom =
                            if (imeBottom > systemBarsBottom) imeBottom - systemBarsBottom
                            else imeBottom
                    )
            ) {
                floating()
            }
        }
    }
}
package one.mixin.android.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.uber.autodispose.ScopeProvider
import io.reactivex.subjects.CompletableSubject

@Composable
fun rememberComposeScope(): ScopeProvider {
    val completable =
        remember {
            CompletableSubject.create()
        }
    val scope =
        remember {
            ScopeProvider {
                completable
            }
        }
    DisposableEffect(Unit) {
        onDispose {
            completable.onComplete()
        }
    }
    return scope
}

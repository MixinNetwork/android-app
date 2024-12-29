package androidx.compose.ui.platform

import androidx.compose.runtime.Composable

// Used by Stripe Android SDK
// Work around for https://github.com/stripe/stripe-android/issues/7184
// This existed as Experimental in Compose 1.5, but was moved to a val in Compose 1.6
@Suppress("unused")
object LocalSoftwareKeyboardController {
    val current
        @Composable
        get() = LocalSoftwareKeyboardController.current
}

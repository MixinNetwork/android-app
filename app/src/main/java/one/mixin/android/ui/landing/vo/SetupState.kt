package one.mixin.android.ui.landing.vo

sealed class SetupState {
    data object Loading : SetupState()
    data object Success : SetupState()
    data object Failure : SetupState()
}
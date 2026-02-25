package one.mixin.android.ui.landing.vo

sealed class MnemonicPhraseState {
    data object Creating : MnemonicPhraseState()
    data object Success : MnemonicPhraseState()
    data object Failure : MnemonicPhraseState()
}

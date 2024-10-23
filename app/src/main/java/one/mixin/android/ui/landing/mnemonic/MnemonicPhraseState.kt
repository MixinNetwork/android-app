package one.mixin.android.ui.landing.mnemonic

sealed class MnemonicPhraseState {
    object Initial : MnemonicPhraseState()
    object Creating : MnemonicPhraseState()
    object Success : MnemonicPhraseState()
    object Failure : MnemonicPhraseState()
}
package one.mixin.android.ui.conversation.link.parser

import one.mixin.android.ui.common.biometric.AssetBiometricItem

class BalanceError(val assetBiometricItem: AssetBiometricItem, override val message: String? = null) : Exception()

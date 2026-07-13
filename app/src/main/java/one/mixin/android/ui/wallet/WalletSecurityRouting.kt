package one.mixin.android.ui.wallet

import one.mixin.android.vo.WalletCategory

enum class WalletSecurityStartRoute {
    Notice,
    VerifyPin,
    ViewSecurity,
    ViewAddress,
    FetchPendingMnemonic,
}

fun walletSecurityStartRoute(
    mode: WalletSecurityActivity.Mode,
    hasVerifiedPin: Boolean = false,
): WalletSecurityStartRoute =
    when (mode) {
        WalletSecurityActivity.Mode.CREATE_WALLET,
        WalletSecurityActivity.Mode.IMPORT_MNEMONIC,
        WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY,
        WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS,
        -> WalletSecurityStartRoute.Notice
        WalletSecurityActivity.Mode.VIEW_MNEMONIC,
        WalletSecurityActivity.Mode.VIEW_PRIVATE_KEY,
        -> WalletSecurityStartRoute.ViewSecurity
        WalletSecurityActivity.Mode.RE_IMPORT_MNEMONIC,
        WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY,
        -> WalletSecurityStartRoute.VerifyPin
        WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC -> {
            if (hasVerifiedPin) {
                WalletSecurityStartRoute.FetchPendingMnemonic
            } else {
                WalletSecurityStartRoute.VerifyPin
            }
        }
        WalletSecurityActivity.Mode.VIEW_ADDRESS -> WalletSecurityStartRoute.ViewAddress
        WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC -> WalletSecurityStartRoute.FetchPendingMnemonic
    }

fun importWalletCategoryForMode(mode: WalletSecurityActivity.Mode): String =
    when (mode) {
        WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC,
        WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC,
        WalletSecurityActivity.Mode.IMPORT_MNEMONIC,
        -> WalletCategory.IMPORTED_MNEMONIC.value
        else -> WalletCategory.IMPORTED_MNEMONIC.value
    }

fun shouldBlockWalletSecurityBack(
    mode: WalletSecurityActivity.Mode,
    isImportStep: Boolean,
): Boolean =
    isImportStep && shouldHideWalletSecurityClose(mode)

fun shouldHideWalletSecurityClose(mode: WalletSecurityActivity.Mode): Boolean =
    when (mode) {
        WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC,
        WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC,
        -> true
        else -> false
    }

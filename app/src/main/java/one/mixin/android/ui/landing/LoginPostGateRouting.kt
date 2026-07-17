package one.mixin.android.ui.landing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class LoginAccountRoute {
    SetupName,
    UseLocalDatabase,
    Restore,
}

fun routeLoginAccount(
    hasFullName: Boolean,
    hasLocalAccountDatabase: Boolean,
): LoginAccountRoute =
    when {
        !hasFullName -> LoginAccountRoute.SetupName
        hasLocalAccountDatabase -> LoginAccountRoute.UseLocalDatabase
        else -> LoginAccountRoute.Restore
    }

enum class LoginPostGateRoute {
    VerifyPin,
    SetupPin,
    UpgradeTip,
}

enum class PendingMnemonicStartupRoute {
    Continue,
    ResumeAccountSetup,
    ImportMnemonic,
}

fun routePendingMnemonicStartup(
    hasPendingImport: Boolean,
    hasSafe: Boolean,
): PendingMnemonicStartupRoute =
    when {
        !hasPendingImport -> PendingMnemonicStartupRoute.Continue
        !hasSafe -> PendingMnemonicStartupRoute.ResumeAccountSetup
        else -> PendingMnemonicStartupRoute.ImportMnemonic
    }

fun routeLoginPostGate(
    hasSafe: Boolean,
    hasPin: Boolean,
): LoginPostGateRoute =
    when {
        hasSafe -> LoginPostGateRoute.VerifyPin
        hasPin -> LoginPostGateRoute.UpgradeTip
        else -> LoginPostGateRoute.SetupPin
    }

enum class LoginPinGateResult {
    Continue,
    Block,
}

fun routeLoginPinGate(
    pinVerified: Boolean,
): LoginPinGateResult =
    if (pinVerified) {
        LoginPinGateResult.Continue
    } else {
        LoginPinGateResult.Block
    }

fun loginPinGateDismissCallback(
    ownerScope: CoroutineScope,
    openNext: suspend (String?) -> Boolean,
    finish: () -> Unit,
): (Boolean, String?) -> Unit = { success, pin ->
    if (routeLoginPinGate(success) == LoginPinGateResult.Continue) {
        ownerScope.launch {
            if (openNext(pin)) {
                finish()
            }
        }
    }
}

fun <T> reuseOrCreateLoginPinGate(
    existing: T?,
    create: () -> T,
    bind: (T) -> Unit,
    show: (T) -> Unit,
): T {
    val dialog = existing ?: create()
    bind(dialog)
    if (existing == null) {
        show(dialog)
    }
    return dialog
}

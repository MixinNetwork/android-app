package one.mixin.android.ui.landing

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

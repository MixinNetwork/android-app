package one.mixin.android.ui.wallet.alert.vo

enum class InputError {
    EQUALS_CURRENT_PRICE,
    EXCEEDS_MAX_PRICE,
    BELOW_MIN_PRICE,
    MUST_BE_LESS_THAN_CURRENT_PRICE,
    MUST_BE_GREATER_THAN_CURRENT_PRICE,
    INCREASE_TOO_HIGH,
    INCREASE_TOO_LOW,
    DECREASE_TOO_HIGH,
    DECREASE_TOO_LOW
}

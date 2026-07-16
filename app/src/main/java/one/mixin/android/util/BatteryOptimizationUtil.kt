package one.mixin.android.util

internal fun isPowerManagerBatteryOptimizationRestricted(isIgnoringBatteryOptimizations: Boolean?): Boolean {
    return isIgnoringBatteryOptimizations != true
}

internal fun shouldPrioritizeBatteryOptimizationRequest(romType: RomPermissionUtil.RomType): Boolean {
    return when (romType) {
        RomPermissionUtil.RomType.NATIVE,
        RomPermissionUtil.RomType.HUAWEI,
        RomPermissionUtil.RomType.HONOR
        -> true
        else -> false
    }
}

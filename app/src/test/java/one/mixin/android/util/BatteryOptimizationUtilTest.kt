package one.mixin.android.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatteryOptimizationUtilTest {
    @Test
    fun powerManagerNullIsRestricted() {
        assertTrue(isPowerManagerBatteryOptimizationRestricted(null))
        assertTrue(isPowerManagerBatteryOptimizationRestricted(false))
        assertFalse(isPowerManagerBatteryOptimizationRestricted(true))
    }

    @Test
    fun requestIntentIsPrioritizedForNativeHuaweiAndHonor() {
        assertTrue(shouldPrioritizeBatteryOptimizationRequest(RomPermissionUtil.RomType.NATIVE))
        assertTrue(shouldPrioritizeBatteryOptimizationRequest(RomPermissionUtil.RomType.HUAWEI))
        assertTrue(shouldPrioritizeBatteryOptimizationRequest(RomPermissionUtil.RomType.HONOR))
        assertFalse(shouldPrioritizeBatteryOptimizationRequest(RomPermissionUtil.RomType.SAMSUNG))
        assertFalse(shouldPrioritizeBatteryOptimizationRequest(RomPermissionUtil.RomType.UNKNOWN))
    }
}

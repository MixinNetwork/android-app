package one.mixin.android.ui.home.reminder

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationPermissionActionTest {
    @Test
    fun deniedPermissionWithRationaleRequestsPermissionAgain() {
        val action =
            notificationPermissionAction(
                sdkInt = 33,
                permissionGranted = false,
                permissionRequested = true,
                shouldShowRationale = true,
            )

        assertEquals(NotificationPermissionAction.RequestPermission, action)
    }

    @Test
    fun firstRequestWithoutRationaleRequestsPermission() {
        val action =
            notificationPermissionAction(
                sdkInt = 33,
                permissionGranted = false,
                permissionRequested = false,
                shouldShowRationale = false,
            )

        assertEquals(NotificationPermissionAction.RequestPermission, action)
    }

    @Test
    fun permanentlyDeniedPermissionOpensSettings() {
        val action =
            notificationPermissionAction(
                sdkInt = 33,
                permissionGranted = false,
                permissionRequested = true,
                shouldShowRationale = false,
            )

        assertEquals(NotificationPermissionAction.OpenSettings, action)
    }

    @Test
    fun disabledNotificationsBeforeAndroid13OpenSettings() {
        val action =
            notificationPermissionAction(
                sdkInt = 32,
                permissionGranted = true,
                permissionRequested = false,
                shouldShowRationale = false,
            )

        assertEquals(NotificationPermissionAction.OpenSettings, action)
    }
}

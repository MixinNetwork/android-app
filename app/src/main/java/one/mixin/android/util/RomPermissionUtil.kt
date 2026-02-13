package one.mixin.android.util

import android.annotation.TargetApi
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import one.mixin.android.MixinApplication
import timber.log.Timber

object RomPermissionUtil {
    private const val OP_BACKGROUND_START_ACTIVITY_MIUI = 10021
    private const val OP_BACKGROUND_START_ACTIVITY_OPPO = 10021

    enum class RomType {
        MIUI,
        OPPO,
        VIVO,
        HUAWEI,
        HONOR,
        SAMSUNG,
        NATIVE,
        UNKNOWN
    }

    fun getCurrentRomType(): RomType {
        return when {
            RomUtil.isMiui -> RomType.MIUI
            RomUtil.isOppo -> RomType.OPPO
            RomUtil.isVivo -> RomType.VIVO
            RomUtil.isEmui -> {
                if (Build.MANUFACTURER.equals("HONOR", ignoreCase = true)) {
                    RomType.HONOR
                } else {
                    RomType.HUAWEI
                }
            }
            RomUtil.isOneUi -> RomType.SAMSUNG
            Build.MANUFACTURER.equals("Google", ignoreCase = true) -> RomType.NATIVE
            else -> RomType.UNKNOWN
        }
    }

    fun checkBackgroundStartPermission(context: Context): Boolean {
        return when (getCurrentRomType()) {
            RomType.MIUI -> checkMiuiBackgroundPermission()
            RomType.OPPO -> checkOppoBackgroundPermission()
            RomType.VIVO -> checkVivoBackgroundPermission()
            RomType.HUAWEI, RomType.HONOR -> checkHuaweiBackgroundPermission()
            else -> true
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun checkMiuiBackgroundPermission(): Boolean {
        return try {
            if (!XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
                return false
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Check MIUI background permission failed")
            true
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun checkOppoBackgroundPermission(): Boolean {
        return try {
            val mgr = MixinApplication.appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (mgr != null) {
                val method = AppOpsManager::class.java.getMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                val result = method.invoke(
                    mgr,
                    OP_BACKGROUND_START_ACTIVITY_OPPO,
                    android.os.Process.myUid(),
                    MixinApplication.appContext.packageName
                ) as Int
                result == AppOpsManager.MODE_ALLOWED
            } else {
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Check OPPO background permission failed")
            true
        }
    }

    private fun checkVivoBackgroundPermission(): Boolean {
        return true
    }

    private fun checkHuaweiBackgroundPermission(): Boolean {
        return true
    }

    fun getBackgroundPermissionIntent(context: Context): Intent? {
        return when (getCurrentRomType()) {
            RomType.MIUI -> getMiuiPermissionIntent()
            RomType.OPPO -> getOppoPermissionIntent(context)
            RomType.VIVO -> getVivoPermissionIntent(context)
            RomType.HUAWEI, RomType.HONOR -> getHuaweiPermissionIntent(context)
            else -> getDefaultPermissionIntent(context)
        }
    }

    private fun getMiuiPermissionIntent(): Intent? {
        return try {
            XiaomiUtilities.getPermissionManagerIntent()
        } catch (e: Exception) {
            Timber.e(e, "Get MIUI permission intent failed")
            null
        }
    }

    private fun getOppoPermissionIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Get OPPO permission intent failed")
            try {
                Intent().apply {
                    component = ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                }
            } catch (e2: Exception) {
                Timber.e(e2, "Get OPPO permission intent (fallback) failed")
                null
            }
        }
    }

    private fun getVivoPermissionIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Get vivo permission intent failed")
            try {
                Intent().apply {
                    component = ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                }
            } catch (e2: Exception) {
                Timber.e(e2, "Get vivo permission intent (fallback) failed")
                null
            }
        }
    }

    private fun getHuaweiPermissionIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Get Huawei permission intent failed")
            try {
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
            } catch (e2: Exception) {
                Timber.e(e2, "Get Huawei permission intent (fallback) failed")
                null
            }
        }
    }

    private fun getDefaultPermissionIntent(context: Context): Intent? {
        return try {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get default permission intent failed")
            null
        }
    }

    fun openBackgroundPermissionSetting(context: Context): Boolean {
        val intent = getBackgroundPermissionIntent(context) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Open background permission setting failed")
            try {
                val fallbackIntent = getDefaultPermissionIntent(context)
                if (fallbackIntent != null) {
                    context.startActivity(fallbackIntent)
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
                Timber.e(e2, "Open fallback permission setting failed")
                false
            }
        }
    }
}

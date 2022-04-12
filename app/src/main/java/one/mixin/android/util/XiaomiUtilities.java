package one.mixin.android.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import one.mixin.android.MixinApplication;
import timber.log.Timber;

import java.lang.reflect.Method;

// MIUI. Redefining Android.
// (not in the very best way I'd say)
public class XiaomiUtilities {

    // custom permissions
    public static final int OP_ACCESS_XIAOMI_ACCOUNT = 10015;
    public static final int OP_AUTO_START = 10008;
    public static final int OP_BACKGROUND_START_ACTIVITY = 10021;
    public static final int OP_BLUETOOTH_CHANGE = 10002;
    public static final int OP_BOOT_COMPLETED = 10007;
    public static final int OP_DATA_CONNECT_CHANGE = 10003;
    public static final int OP_DELETE_CALL_LOG = 10013;
    public static final int OP_DELETE_CONTACTS = 10012;
    public static final int OP_DELETE_MMS = 10011;
    public static final int OP_DELETE_SMS = 10010;
    public static final int OP_EXACT_ALARM = 10014;
    public static final int OP_GET_INSTALLED_APPS = 10022;
    public static final int OP_GET_TASKS = 10019;
    public static final int OP_INSTALL_SHORTCUT = 10017;
    public static final int OP_NFC = 10016;
    public static final int OP_NFC_CHANGE = 10009;
    public static final int OP_READ_MMS = 10005;
    public static final int OP_READ_NOTIFICATION_SMS = 10018;
    public static final int OP_SEND_MMS = 10004;
    public static final int OP_SERVICE_FOREGROUND = 10023;
    public static final int OP_SHOW_WHEN_LOCKED = 10020;
    public static final int OP_WIFI_CHANGE = 10001;
    public static final int OP_WRITE_MMS = 10006;

    @SuppressWarnings("JavaReflectionMemberAccess")
    @TargetApi(19)
    public static boolean isCustomPermissionGranted(int permission) {
        try {
            AppOpsManager mgr =
                    (AppOpsManager) MixinApplication.appContext.getSystemService(Context.APP_OPS_SERVICE);
            Method m = AppOpsManager.class.getMethod("checkOpNoThrow", int.class, int.class, String.class);
            int result =
                    (int) m.invoke(mgr, permission, android.os.Process.myUid(),
                            MixinApplication.appContext.getPackageName());
            return result == AppOpsManager.MODE_ALLOWED;
        } catch (Exception x) {
            Timber.e(x);
        }
        return true;
    }

    public static Intent getPermissionManagerIntent() {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        intent.putExtra("extra_package_uid", android.os.Process.myUid());
        intent.putExtra("extra_pkgname", MixinApplication.appContext.getPackageName());
        return intent;
    }
}

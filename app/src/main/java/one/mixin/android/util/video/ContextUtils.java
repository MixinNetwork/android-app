package one.mixin.android.util.video;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

public class ContextUtils {

    public static int getVersionCode(@NonNull Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }
}

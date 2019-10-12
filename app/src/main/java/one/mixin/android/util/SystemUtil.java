package one.mixin.android.util;

import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

public class SystemUtil {
    private static final String SYS_EMUI = "sys_emui";
    private static final String SYS_MIUI = "sys_miui";
    private static final String SYS_FLYME = "sys_flyme";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
    private static final String KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level";
    private static final String KEY_EMUI_VERSION = "ro.build.version.emui";
    private static final String KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion";

    public static boolean isHuaWei() {
        return getSystem() == SYS_EMUI;
    }

    public static String getSystem() {
        String SYS = null;
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            if (prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                    || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                    || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null) {
                SYS = SYS_MIUI;//小米
            } else if (prop.getProperty(KEY_EMUI_API_LEVEL, null) != null
                    || prop.getProperty(KEY_EMUI_VERSION, null) != null
                    || prop.getProperty(KEY_EMUI_CONFIG_HW_SYS_VERSION, null) != null) {
                SYS = SYS_EMUI;//华为
            } else if (getMeizuFlymeOSFlag().toLowerCase().contains("flyme")) {
                SYS = SYS_FLYME;//魅族
            }
        } catch (IOException e) {
            e.printStackTrace();
            return SYS;
        }
        return SYS;
    }

    public static String getMeizuFlymeOSFlag() {
        return getSystemProperty("ro.build.display.id", "");
    }

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            return (String) get.invoke(clz, key, defaultValue);
        } catch (Exception e) {
        }
        return defaultValue;
    }
}

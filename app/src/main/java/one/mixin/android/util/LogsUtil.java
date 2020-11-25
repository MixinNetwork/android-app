package one.mixin.android.util;

import android.util.ArrayMap;
import com.microsoft.appcenter.crashes.Crashes;

public class LogsUtil {
    public static void log(String log) {
        ArrayMap<String, String> logs = new ArrayMap<String, String>();
        logs.put("log", log);
        Crashes.trackError(
                new Exception(),
                logs
                ,
                null);
    }
}

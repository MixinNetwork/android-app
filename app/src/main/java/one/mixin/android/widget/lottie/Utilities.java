package one.mixin.android.widget.lottie;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.IntDef;

import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Locale;

import one.mixin.android.MixinApplication;
import one.mixin.android.extension.ContextExtensionKt;
import one.mixin.android.util.DispatchQueue;
import one.mixin.android.widget.AndroidUtilities;
import timber.log.Timber;

public class Utilities {
    public static volatile DispatchQueue globalQueue = new DispatchQueue("globalQueue");
    public static int clamp(int value, int maxValue, int minValue) {
        return Math.max(Math.min(value, maxValue), minValue);
    }

    public static float clamp(float value, float maxValue, float minValue) {
        if (Float.isNaN(value)) {
            return minValue;
        }
        if (Float.isInfinite(value)) {
            return maxValue;
        }
        return Math.max(Math.min(value, maxValue), minValue);
    }

    public static void recycleBitmaps(List<Bitmap> bitmapToRecycle) {
        if (Build.VERSION.SDK_INT <= 23) {
            // cause to crash:
            // /system/lib/libskia.so (SkPixelRef::unlockPixels()+3)
            // /system/lib/libskia.so (SkBitmap::freePixels()+14)
            // /system/lib/libskia.so (SkBitmap::setPixelRef(SkPixelRef*, int, int)+50)
            // /system/lib/libhwui.so (android::uirenderer::ResourceCache::recycleLocked(SkBitmap*)+30)
            // /system/lib/libhwui.so (android::uirenderer::ResourceCache::recycle(SkBitmap*)+20)
            // gc recycle it automatically
            return;
        }
        if (bitmapToRecycle != null && !bitmapToRecycle.isEmpty()) {
            ContextExtensionKt.runOnUiThread(() -> Utilities.globalQueue.postRunnable(() -> {
                for (int i = 0; i < bitmapToRecycle.size(); i++) {
                    Bitmap bitmap = bitmapToRecycle.get(i);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        try {
                            bitmap.recycle();
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            }), 36);
        }
    }

    public static Integer parseInt(CharSequence value) {
        if (value == null) {
            return 0;
        }
        int val = 0;
        try {
            int start = -1, end;
            for (end = 0; end < value.length(); ++end) {
                char character = value.charAt(end);
                boolean allowedChar = character == '-' || character >= '0' && character <= '9';
                if (allowedChar && start < 0) {
                    start = end;
                } else if (!allowedChar && start >= 0) {
                    end++;
                    break;
                }
            }
            if (start >= 0) {
                String str = value.subSequence(start, end).toString();
//                val = parseInt(str);
                val = Integer.parseInt(str);
            }
        } catch (Exception ignore) {}
        return val;
    }

    private static final int[] LOW_SOC = {
            -1775228513, // EXYNOS 850
            802464304,  // EXYNOS 7872
            802464333,  // EXYNOS 7880
            802464302,  // EXYNOS 7870
            2067362118, // MSM8953
            2067362060, // MSM8937
            2067362084, // MSM8940
            2067362241, // MSM8992
            2067362117, // MSM8952
            2067361998, // MSM8917
            -1853602818 // SDM439
    };

    @PerformanceClass
    private static int devicePerformanceClass;
    @PerformanceClass
    private static int overrideDevicePerformanceClass;

    public final static int PERFORMANCE_CLASS_LOW = 0;
    public final static int PERFORMANCE_CLASS_AVERAGE = 1;
    public final static int PERFORMANCE_CLASS_HIGH = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            PERFORMANCE_CLASS_LOW,
            PERFORMANCE_CLASS_AVERAGE,
            PERFORMANCE_CLASS_HIGH
    })
    public @interface PerformanceClass {}

    @PerformanceClass
    public static int getDevicePerformanceClass() {
        if (overrideDevicePerformanceClass != -1) {
            return overrideDevicePerformanceClass;
        }
        if (devicePerformanceClass == -1) {
            devicePerformanceClass = measureDevicePerformanceClass();
        }
        return devicePerformanceClass;
    }

    public static int measureDevicePerformanceClass() {
        int androidVersion = Build.VERSION.SDK_INT;
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int memoryClass = ((ActivityManager) MixinApplication.appContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != null) {
            int hash = Build.SOC_MODEL.toUpperCase().hashCode();
            for (int i = 0; i < LOW_SOC.length; ++i) {
                if (LOW_SOC[i] == hash) {
                    return PERFORMANCE_CLASS_LOW;
                }
            }
        }

        int totalCpuFreq = 0;
        int freqResolved = 0;
        for (int i = 0; i < cpuCount; i++) {
            try {
                RandomAccessFile reader = new RandomAccessFile(String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i), "r");
                String line = reader.readLine();
                if (line != null) {
                    totalCpuFreq += Utilities.parseInt(line) / 1000;
                    freqResolved++;
                }
                reader.close();
            } catch (Throwable ignore) {}
        }
        int maxCpuFreq = freqResolved == 0 ? -1 : (int) Math.ceil(totalCpuFreq / (float) freqResolved);

        long ram = -1;
        try {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ((ActivityManager) MixinApplication.appContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
            ram = memoryInfo.totalMem;
        } catch (Exception ignore) {}

        int performanceClass;
        if (
                androidVersion < 21 ||
                        cpuCount <= 2 ||
                        memoryClass <= 100 ||
                        cpuCount <= 4 && maxCpuFreq != -1 && maxCpuFreq <= 1250 ||
                        cpuCount <= 4 && maxCpuFreq <= 1600 && memoryClass <= 128 && androidVersion <= 21 ||
                        cpuCount <= 4 && maxCpuFreq <= 1300 && memoryClass <= 128 && androidVersion <= 24 ||
                        ram != -1 && ram < 2L * 1024L * 1024L * 1024L
        ) {
            performanceClass = PERFORMANCE_CLASS_LOW;
        } else if (
                cpuCount < 8 ||
                        memoryClass <= 160 ||
                        maxCpuFreq != -1 && maxCpuFreq <= 2055 ||
                        maxCpuFreq == -1 && cpuCount == 8 && androidVersion <= 23
        ) {
            performanceClass = PERFORMANCE_CLASS_AVERAGE;
        } else {
            performanceClass = PERFORMANCE_CLASS_HIGH;
        }
        Timber.d("device performance info selected_class = " + performanceClass + " (cpu_count = " + cpuCount + ", freq = " + maxCpuFreq + ", memoryClass = " + memoryClass + ", android version " + androidVersion + ", manufacture " + Build.MANUFACTURER + ", screenRefreshRate=" + AndroidUtilities.INSTANCE.getScreenRefreshRate() + ")");
        return performanceClass;
    }
}
package one.mixin.android.job;

import com.birbit.android.jobqueue.BuildConfig;
import com.birbit.android.jobqueue.log.JqLog;

import one.mixin.android.util.CrashExceptionReportKt;
import timber.log.Timber;


public class JobLogger extends JqLog.ErrorLogger {

    public static final String TAG = "JobLogger";

    @Override
    public boolean isDebugEnabled() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void v(String text, Object... args) {
    }

    @Override
    public void e(String text, Object... args) {
        Timber.tag(TAG).e("Job Error %s %s", text, args);
    }

    @Override
    public void e(Throwable t, String text, Object... args) {
        Timber.tag(TAG).e(t, "Job Error: %s %s", text, args);
        CrashExceptionReportKt.reportException(t);
    }

    @Override
    public void d(String text, Object... args) {
    }
}

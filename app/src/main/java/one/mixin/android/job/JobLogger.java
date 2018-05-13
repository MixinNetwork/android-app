package one.mixin.android.job;

import android.util.Log;

import com.birbit.android.jobqueue.BuildConfig;
import com.birbit.android.jobqueue.log.JqLog;
import com.bugsnag.android.Bugsnag;


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
        Log.e(TAG, "Job Error " + String.format(text, args));
    }

    @Override
    public void e(Throwable t, String text, Object... args) {
        Log.e(TAG, "Job Error: " + String.format(text, args), t);
        Bugsnag.notify(t);
    }

    @Override
    public void d(String text, Object... args) {
    }
}

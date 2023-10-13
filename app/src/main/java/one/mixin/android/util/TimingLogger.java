package one.mixin.android.util;

import android.os.SystemClock;
import timber.log.Timber;

import java.util.ArrayList;

public class TimingLogger {

    private String mTag;

    private String mLabel;

    ArrayList<Long> mSplits;

    ArrayList<String> mSplitLabels;

    public TimingLogger(String tag, String label) {
        reset(tag, label);
    }

    public void reset(String tag, String label) {
        mTag = tag;
        mLabel = label;
        reset();
    }

    public void reset() {
        if (mSplits == null) {
            mSplits = new ArrayList<>();
            mSplitLabels = new ArrayList<>();
        } else {
            mSplits.clear();
            mSplitLabels.clear();
        }
        addSplit(null);
    }

    public void addSplit(String splitLabel) {
        long now = SystemClock.elapsedRealtime();
        mSplits.add(now);
        mSplitLabels.add(splitLabel);
    }

    public void dumpToLog() {
        Timber.d("%s: begin", mLabel);
        final long first = mSplits.get(0);
        long now = first;
        for (int i = 1; i < mSplits.size(); i++) {
            now = mSplits.get(i);
            final String splitLabel = mSplitLabels.get(i);
            final long prev = mSplits.get(i - 1);

            Timber.d(mLabel + ":      " + (now - prev) + " ms, " + splitLabel);
        }
        Timber.d(mLabel + ": end, " + (now - first) + " ms");
    }
}

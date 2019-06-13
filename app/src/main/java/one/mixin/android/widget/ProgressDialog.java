package one.mixin.android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import one.mixin.android.R;

public class ProgressDialog extends AlertDialog {

    private ProgressBar mProgress;
    private TextView mMessageView;

    private int mMax;
    private int mProgressVal;
    private int mSecondaryProgressVal;
    private int mIncrementBy;
    private int mIncrementSecondaryBy;
    private Drawable mProgressDrawable;
    private Drawable mIndeterminateDrawable;
    private CharSequence mMessage;
    private boolean mIndeterminate;

    private boolean mHasStarted;


    public ProgressDialog(Context context) {
        super(context);
    }


    public ProgressDialog(Context context, int theme) {
        super(context, theme);
    }


    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message) {
        return show(context, title, message, false);
    }


    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null);
    }


    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null);
    }


    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate,
                                      boolean cancelable, OnCancelListener cancelListener) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.view_progress_dialog, null);
        mProgress = view.findViewById(R.id.progress);
        mMessageView = view.findViewById(R.id.message);
        setView(view);
        if (mMax > 0) {
            setMax(mMax);
        }
        if (mProgressVal > 0) {
            setProgress(mProgressVal);
        }
        if (mSecondaryProgressVal > 0) {
            setSecondaryProgress(mSecondaryProgressVal);
        }
        if (mIncrementBy > 0) {
            incrementProgressBy(mIncrementBy);
        }
        if (mIncrementSecondaryBy > 0) {
            incrementSecondaryProgressBy(mIncrementSecondaryBy);
        }
        if (mProgressDrawable != null) {
            setProgressDrawable(mProgressDrawable);
        }
        if (mIndeterminateDrawable != null) {
            setIndeterminateDrawable(mIndeterminateDrawable);
        }
        if (mMessage != null) {
            setMessage(mMessage);
        }
        setIndeterminate(mIndeterminate);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mHasStarted = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHasStarted = false;
    }

    public void setProgress(int value) {
        if (mHasStarted) {
            mProgress.setProgress(value);
        } else {
            mProgressVal = value;
        }
    }


    public void setSecondaryProgress(int secondaryProgress) {
        if (mProgress != null) {
            mProgress.setSecondaryProgress(secondaryProgress);
        } else {
            mSecondaryProgressVal = secondaryProgress;
        }
    }


    public int getProgress() {
        if (mProgress != null) {
            return mProgress.getProgress();
        }
        return mProgressVal;
    }


    public int getSecondaryProgress() {
        if (mProgress != null) {
            return mProgress.getSecondaryProgress();
        }
        return mSecondaryProgressVal;
    }


    public int getMax() {
        if (mProgress != null) {
            return mProgress.getMax();
        }
        return mMax;
    }


    public void setMax(int max) {
        if (mProgress != null) {
            mProgress.setMax(max);
        } else {
            mMax = max;
        }
    }


    public void incrementProgressBy(int diff) {
        if (mProgress != null) {
            mProgress.incrementProgressBy(diff);
        } else {
            mIncrementBy += diff;
        }
    }

    public void incrementSecondaryProgressBy(int diff) {
        if (mProgress != null) {
            mProgress.incrementSecondaryProgressBy(diff);
        } else {
            mIncrementSecondaryBy += diff;
        }
    }


    public void setProgressDrawable(Drawable d) {
        if (mProgress != null) {
            mProgress.setProgressDrawable(d);
        } else {
            mProgressDrawable = d;
        }
    }


    public void setIndeterminateDrawable(Drawable d) {
        if (mProgress != null) {
            mProgress.setIndeterminateDrawable(d);
        } else {
            mIndeterminateDrawable = d;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate);
        } else {
            mIndeterminate = indeterminate;
        }
    }

    /**
     * Whether this ProgressDialog is in indeterminate mode.
     *
     * @return true if the dialog is in indeterminate mode, false otherwise
     */
    public boolean isIndeterminate() {
        if (mProgress != null) {
            return mProgress.isIndeterminate();
        }
        return mIndeterminate;
    }

    @Override
    public void setMessage(CharSequence message) {
        if (mProgress != null) {

            mMessageView.setText(message);
        } else {
            mMessage = message;
        }
    }

}
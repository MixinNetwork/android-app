package one.mixin.android.widget.keyboard;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.preference.PreferenceManager;

import kotlin.math.MathKt;
import one.mixin.android.R;
import one.mixin.android.extension.ContextExtensionKt;
import timber.log.Timber;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class KeyboardAwareLinearLayout extends LinearLayoutCompat {
    private final Rect rect = new Rect();
    private final Set<OnKeyboardHiddenListener> hiddenListeners = new HashSet<>();
    private final Set<OnKeyboardShownListener> shownListeners = new HashSet<>();

    private final int minKeyboardSize;
    private final int minCustomKeyboardSize;
    private final int defaultCustomKeyboardSize;
    private final int minCustomKeyboardTopMarginPortrait;
    private final int statusBarHeight;

    private int viewInset;

    private boolean keyboardOpen = false;
    private boolean isFullscreen = false;
    private boolean isBubble = false;

    public KeyboardAwareLinearLayout(Context context) {
        this(context, null);
    }

    public KeyboardAwareLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardAwareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        minKeyboardSize = getResources().getDimensionPixelSize(R.dimen.min_keyboard_size);
        minCustomKeyboardSize = getResources().getDimensionPixelSize(R.dimen.min_custom_keyboard_size);
        defaultCustomKeyboardSize = getResources().getDimensionPixelSize(R.dimen.default_custom_keyboard_size);
        minCustomKeyboardTopMarginPortrait = getResources().getDimensionPixelSize(R.dimen.min_custom_keyboard_top_margin_portrait);
        statusBarHeight = ContextExtensionKt.statusBarHeight(context);
        viewInset = getViewInset();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updateKeyboardState();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setIsBubble(boolean isBubble) {
        this.isBubble = isBubble;
    }

    @SuppressLint("ObsoleteSdkInt")
    private void updateKeyboardState() {
        if (viewInset == 0 && Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
            viewInset = getViewInset();

        getWindowVisibleDisplayFrame(rect);

        final int availableHeight = getAvailableHeight();
        final int keyboardHeight = Math.max(availableHeight - rect.bottom, 0);

        Timber.i("updateKeyboardState keyboardOpen: " + keyboardOpen + ", keyboardHeight: " + keyboardHeight + ". minKeyboardSize: " + minKeyboardSize);
        if (keyboardHeight > minKeyboardSize) {
            if (getKeyboardHeight() != keyboardHeight) {
                setKeyboardHeight(keyboardHeight);
            }
            if (!keyboardOpen) {
                onKeyboardOpen(keyboardHeight);
            }
        } else if (keyboardHeight == 0 && keyboardOpen) {
            onKeyboardClose();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= 23 && getRootWindowInsets() != null) {
            int bottomInset;
            WindowInsets windowInsets = getRootWindowInsets();
            if (Build.VERSION.SDK_INT >= 30) {
                bottomInset = windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else {
                bottomInset = windowInsets.getStableInsetBottom();
            }

            if (bottomInset != 0 && (viewInset == 0 || viewInset == statusBarHeight)) {
                Timber.i("Updating view inset based on WindowInsets. viewInset: " + viewInset + " windowInset: " + bottomInset);
                viewInset = bottomInset;
            }
        }
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private int getViewInset() {
        try {
            @SuppressLint("SoonBlockedPrivateApi")
            Field attachInfoField = View.class.getDeclaredField("mAttachInfo");
            attachInfoField.setAccessible(true);
            Object attachInfo = attachInfoField.get(this);
            if (attachInfo != null) {
                Field stableInsetsField = attachInfo.getClass().getDeclaredField("mStableInsets");
                stableInsetsField.setAccessible(true);
                Rect insets = (Rect) stableInsetsField.get(attachInfo);
                if (insets != null) {
                    return insets.bottom;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Do nothing
        }
        return statusBarHeight;
    }

    private int getAvailableHeight() {
        return this.getRootView().getHeight() - viewInset;
    }

    protected void onKeyboardOpen(int keyboardHeight) {
        Timber.i("onKeyboardOpen(" + keyboardHeight + ")");
        keyboardOpen = true;

        notifyShownListeners();
    }

    protected void onKeyboardClose() {
        Timber.i("onKeyboardClose()");
        keyboardOpen = false;
        notifyHiddenListeners();
    }

    public boolean isKeyboardOpen() {
        return keyboardOpen;
    }

    private int getKeyboardHeight() {
        if (isBubble) {
            int height = getRootView().getHeight();
            return height - (int) (height * 0.45);
        }

        int keyboardHeight = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt("keyboard_height_portrait", defaultCustomKeyboardSize);
        return clamp(keyboardHeight, minCustomKeyboardSize, getRootView().getHeight() - minCustomKeyboardTopMarginPortrait);
    }

    private void setKeyboardHeight(int height) {
        if (isBubble) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit().putInt("keyboard_height_portrait", height).apply();
    }

    public int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public void postOnKeyboardClose(final Runnable runnable) {
        if (keyboardOpen) {
            addOnKeyboardHiddenListener(new OnKeyboardHiddenListener() {
                @Override
                public void onKeyboardHidden() {
                    removeOnKeyboardHiddenListener(this);
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    public void postOnKeyboardOpen(final Runnable runnable) {
        if (!keyboardOpen) {
            addOnKeyboardShownListener(new OnKeyboardShownListener() {
                @Override
                public void onKeyboardShown() {
                    removeOnKeyboardShownListener(this);
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    public void addOnKeyboardHiddenListener(OnKeyboardHiddenListener listener) {
        hiddenListeners.add(listener);
    }

    public void removeOnKeyboardHiddenListener(OnKeyboardHiddenListener listener) {
        hiddenListeners.remove(listener);
    }

    public void addOnKeyboardShownListener(OnKeyboardShownListener listener) {
        shownListeners.add(listener);
    }

    public void removeOnKeyboardShownListener(OnKeyboardShownListener listener) {
        shownListeners.remove(listener);
    }

    public void setFullscreen(boolean isFullscreen) {
        this.isFullscreen = isFullscreen;
    }

    private void notifyHiddenListeners() {
        final Set<OnKeyboardHiddenListener> listeners = new HashSet<>(hiddenListeners);
        for (OnKeyboardHiddenListener listener : listeners) {
            listener.onKeyboardHidden();
        }
    }

    private void notifyShownListeners() {
        final Set<OnKeyboardShownListener> listeners = new HashSet<>(shownListeners);
        for (OnKeyboardShownListener listener : listeners) {
            listener.onKeyboardShown();
        }
    }

    public interface OnKeyboardHiddenListener {
        void onKeyboardHidden();
    }

    public interface OnKeyboardShownListener {
        void onKeyboardShown();
    }
}
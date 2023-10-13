package one.mixin.android.widget.PhotoView;

import android.view.View;

class Compat {

    private static final int SIXTY_FPS_INTERVAL = 1000 / 60;

    static void postOnAnimation(View view, Runnable runnable) {
        postOnAnimationJellyBean(view, runnable);
    }

    private static void postOnAnimationJellyBean(View view, Runnable runnable) {
        view.postOnAnimation(runnable);
    }
}
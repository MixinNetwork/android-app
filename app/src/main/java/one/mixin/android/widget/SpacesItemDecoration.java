package one.mixin.android.widget;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;
    private boolean onlyTop = false;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    public SpacesItemDecoration(int space, boolean onlyTop) {
        this.space = space;
        this.onlyTop = onlyTop;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        outRect.top = space;
        if (onlyTop) {
            return;
        }
        outRect.right = space;
    }
}
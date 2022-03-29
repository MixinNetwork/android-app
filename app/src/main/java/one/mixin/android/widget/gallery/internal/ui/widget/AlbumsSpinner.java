package one.mixin.android.widget.gallery.internal.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.TextView;
import one.mixin.android.R;
import one.mixin.android.widget.gallery.internal.entity.Album;

public class AlbumsSpinner {

    private static final int MAX_SHOWN_COUNT = 6;
    private CursorAdapter mAdapter;
    private TextView mSelected;
    private final ListPopupWindow mListPopupWindow;
    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;

    public AlbumsSpinner(@NonNull Context context) {
        mListPopupWindow = new ListPopupWindow(context, null, R.attr.listPopupWindowStyle);
        mListPopupWindow.setModal(true);
        float density = context.getResources().getDisplayMetrics().density;
        mListPopupWindow.setContentWidth((int) (216 * density));
        mListPopupWindow.setHorizontalOffset((int) (16 * density));
        mListPopupWindow.setVerticalOffset((int) (-48 * density));

        mListPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            AlbumsSpinner.this.onItemSelected(parent.getContext(), position);
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(parent, view, position, id);
            }
        });
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public void setSelection(Context context, int position) {
        mListPopupWindow.setSelection(position);
        onItemSelected(context, position);
    }

    private void onItemSelected(Context context, int position) {
        mListPopupWindow.dismiss();
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        Album album = Album.valueOf(cursor);
        String displayName = album.getDisplayName(context);
        if (mSelected.getVisibility() == View.VISIBLE) {
            mSelected.setText(displayName);
        } else {
            mSelected.setAlpha(0.0f);
            mSelected.setVisibility(View.VISIBLE);
            mSelected.setText(displayName);
            mSelected.animate().alpha(1.0f).setDuration(context.getResources().getInteger(
                    android.R.integer.config_longAnimTime)).start();

        }
    }

    public void setAdapter(CursorAdapter adapter) {
        mListPopupWindow.setAdapter(adapter);
        mAdapter = adapter;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setSelectedTextView(TextView textView) {
        mSelected = textView;
        // tint dropdown arrow icon
        Drawable[] drawables = mSelected.getCompoundDrawables();
        Drawable right = drawables[2];
        if (right != null) {
            right.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        }

        mSelected.setVisibility(View.GONE);
        mSelected.setOnClickListener(v -> {
            int itemHeight = v.getResources().getDimensionPixelSize(R.dimen.album_item_height);
            mListPopupWindow.setHeight(
                    mAdapter.getCount() > MAX_SHOWN_COUNT ? itemHeight * MAX_SHOWN_COUNT
                            : itemHeight * mAdapter.getCount());
            mListPopupWindow.show();
        });
        mSelected.setOnTouchListener(mListPopupWindow.createDragToOpenListener(mSelected));
    }

    public void setPopupAnchorView(View view) {
        mListPopupWindow.setAnchorView(view);
    }

}

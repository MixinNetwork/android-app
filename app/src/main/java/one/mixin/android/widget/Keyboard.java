package one.mixin.android.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import one.mixin.android.R;

public class Keyboard extends RelativeLayout {
    private Context context;

    private OnClickKeyboardListener onClickKeyboardListener;
    private String[] key;

    public Keyboard(Context context) {
        this(context, null);
    }

    public Keyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Keyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    /**
     * 初始化KeyboardView
     */
    private void initKeyboardView() {
        View view = View.inflate(context, R.layout.view_keyboard, this);
        GridView gvKeyboard = view.findViewById(R.id.gv_keyboard);
        gvKeyboard.setAdapter(keyboardAdapter);
    }

    public interface OnClickKeyboardListener {
        void onKeyClick(int position, String value);

        void onLongClick(int position, String value);
    }


    public void setOnClickKeyboardListener(OnClickKeyboardListener onClickKeyboardListener) {
        this.onClickKeyboardListener = onClickKeyboardListener;
    }

    /**
     * 设置键盘所显示的内容
     *
     * @param key
     */
    public void setKeyboardKeys(String[] key) {
        this.key = key;
        initKeyboardView();
    }

    private BaseAdapter keyboardAdapter = new BaseAdapter() {
        private static final int KEY_NINE = 11;

        @Override
        public int getCount() {
            return key.length;
        }

        @Override
        public Object getItem(int position) {
            return key[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (getItemId(position) == KEY_NINE) ? 0 : 1;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == 1) {
                ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_grid_keyboard, parent, false);
                }
                viewHolder = new ViewHolder(convertView);
                viewHolder.tvKey.setText(key[position]);
                if (TextUtils.isEmpty(key[position])) {
                    convertView.setEnabled(false);
                } else {
                    convertView.setEnabled(true);

                }
            } else {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_grid_keyboard_delete, parent, false);
                }
            }

            convertView.setOnClickListener(v -> {
                if (onClickKeyboardListener != null) {
                    onClickKeyboardListener.onKeyClick(position, key[position]);
                }
            });
            convertView.setOnLongClickListener(v -> {
                if (onClickKeyboardListener != null) {
                    onClickKeyboardListener.onLongClick(position, key[position]);
                }
                return true;
            });
            return convertView;
        }
    };

    static class ViewHolder {
        private TextView tvKey;

        public ViewHolder(View view) {
            tvKey = view.findViewById(R.id.tv_keyboard_keys);
            view.setTag(this);
        }
    }
}

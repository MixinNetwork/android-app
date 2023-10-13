package one.mixin.android.widget.picker;

import android.graphics.Typeface;

import java.util.List;


public interface IWheelPicker {

    int getVisibleItemCount();

    void setVisibleItemCount(int count);

    boolean isCyclic();

    void setCyclic(boolean isCyclic);

    void setOnItemSelectedListener(WheelPicker.OnItemSelectedListener listener);

    int getSelectedItemPosition();

    void setSelectedItemPosition(int position);

    int getCurrentItemPosition();

    List getData();

    void setData(List data);

    void setSameWidth(boolean hasSameSize);

    boolean hasSameWidth();

    void setOnWheelChangeListener(WheelPicker.OnWheelChangeListener listener);

    String getMaximumWidthText();

    void setMaximumWidthText(String text);

    int getMaximumWidthTextPosition();

    void setMaximumWidthTextPosition(int position);

    int getSelectedItemTextColor();

    void setSelectedItemTextColor(int color);

    int getItemTextColor();

    void setItemTextColor(int color);

    int getItemTextSize();

    void setItemTextSize(int size);

    int getItemSpace();

    void setItemSpace(int space);

    void setIndicator(boolean hasIndicator);

    boolean hasIndicator();

    int getIndicatorSize();

    void setIndicatorSize(int size);

    int getIndicatorColor();

    void setIndicatorColor(int color);

    void setCurtain(boolean hasCurtain);

    boolean hasCurtain();

    int getCurtainColor();

    void setCurtainColor(int color);

    void setAtmospheric(boolean hasAtmospheric);

    boolean hasAtmospheric();

    boolean isCurved();

    void setCurved(boolean isCurved);

    int getItemAlign();

    void setItemAlign(int align);

    Typeface getTypeface();

    void setTypeface(Typeface tf);
}
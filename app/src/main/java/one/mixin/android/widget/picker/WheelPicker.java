package one.mixin.android.widget.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;
import one.mixin.android.R;

import java.util.Arrays;
import java.util.List;

public class WheelPicker extends View implements IDebug, IWheelPicker, Runnable {

    public static final int SCROLL_STATE_IDLE = 0, SCROLL_STATE_DRAGGING = 1,
            SCROLL_STATE_SCROLLING = 2;

    public static final int ALIGN_CENTER = 0, ALIGN_LEFT = 1, ALIGN_RIGHT = 2;

    private static final String TAG = WheelPicker.class.getSimpleName();

    private final Handler mHandler = new Handler();

    private Paint mPaint;
    private Scroller mScroller;
    private VelocityTracker mTracker;
  
    private boolean isTouchTriggered;

    private OnItemSelectedListener mOnItemSelectedListener;
    private OnWheelChangeListener mOnWheelChangeListener;

    private Rect mRectDrawn;
    private Rect mRectIndicatorHead, mRectIndicatorFoot;
    private Rect mRectCurrentItem;

    private Camera mCamera;
    private Matrix mMatrixRotate, mMatrixDepth;

    private List mData;

   
    private String mMaxWidthText;

    private int mVisibleItemCount, mDrawnItemCount;

  
    private int mHalfDrawnItemCount;

    
    private int mTextMaxWidth, mTextMaxHeight;

    private int mItemTextColor, mSelectedItemTextColor;

    private int mItemTextSize;

    private int mIndicatorSize;

    private int mIndicatorColor;

    private int mCurtainColor;

    private int mItemSpace;

    private int mItemAlign;

    private int mItemHeight, mHalfItemHeight;

    private int mHalfWheelHeight;

    private int mSelectedItemPosition;

    private int mCurrentItemPosition;

    private int mMinFlingY, mMaxFlingY;

    private int mMinimumVelocity = 50, mMaximumVelocity = 8000;

    private int mWheelCenterX, mWheelCenterY;

    private int mDrawnCenterX, mDrawnCenterY;

    private int mScrollOffsetY;

    private int mTextMaxWidthPosition;

    private int mLastPointY;

    private int mDownPointY;

    private int mTouchSlop = 8;

    private boolean hasSameWidth;

    private boolean hasIndicator;

    private boolean hasCurtain;

    private boolean hasAtmospheric;

    private boolean isCyclic;

    private boolean isCurved;

    private boolean isClick;

    private boolean isForceFinishScroll;

    private String fontPath;

    private boolean isDebug;

    public WheelPicker(Context context) {
        this(context, null);
    }

    public WheelPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker);
        int idData = a.getResourceId(R.styleable.WheelPicker_wheel_data, 0);
        mData = idData == 0 ? Arrays.asList("a", "b", "c") : Arrays.asList(getResources()
                .getStringArray(idData));
        mItemTextSize = a.getDimensionPixelSize(R.styleable.WheelPicker_wheel_item_text_size, 0);
        mVisibleItemCount = a.getInt(R.styleable.WheelPicker_wheel_visible_item_count, 7);
        mSelectedItemPosition = a.getInt(R.styleable.WheelPicker_wheel_selected_item_position, 0);
        hasSameWidth = a.getBoolean(R.styleable.WheelPicker_wheel_same_width, false);
        mTextMaxWidthPosition =
                a.getInt(R.styleable.WheelPicker_wheel_maximum_width_text_position, -1);
        mMaxWidthText = a.getString(R.styleable.WheelPicker_wheel_maximum_width_text);
        mSelectedItemTextColor = a.getColor
                (R.styleable.WheelPicker_wheel_selected_item_text_color, -1);
        mItemTextColor = a.getColor(R.styleable.WheelPicker_wheel_item_text_color, 0xFF888888);
        mItemSpace = a.getDimensionPixelSize(R.styleable.WheelPicker_wheel_item_space, 0);
        isCyclic = a.getBoolean(R.styleable.WheelPicker_wheel_cyclic, false);
        hasIndicator = a.getBoolean(R.styleable.WheelPicker_wheel_indicator, false);
        mIndicatorColor = a.getColor(R.styleable.WheelPicker_wheel_indicator_color, 0xFFEE3333);
        mIndicatorSize = a.getDimensionPixelSize(R.styleable.WheelPicker_wheel_indicator_size, 0);
        hasCurtain = a.getBoolean(R.styleable.WheelPicker_wheel_curtain, false);
        mCurtainColor = a.getColor(R.styleable.WheelPicker_wheel_curtain_color, 0x88FFFFFF);
        hasAtmospheric = a.getBoolean(R.styleable.WheelPicker_wheel_atmospheric, false);
        isCurved = a.getBoolean(R.styleable.WheelPicker_wheel_curved, false);
        mItemAlign = a.getInt(R.styleable.WheelPicker_wheel_item_align, ALIGN_CENTER);
        fontPath = a.getString(R.styleable.WheelPicker_wheel_font_path);
        a.recycle();

        // 可见数据项改变后更新与之相关的参数
        // Update relevant parameters when the count of visible item changed
        updateVisibleItemCount();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.LINEAR_TEXT_FLAG);
        mPaint.setTextSize(mItemTextSize);

        if (fontPath != null) {
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
            setTypeface(typeface);
        }

        // 更新文本对齐方式
        // Update alignment of text
        updateItemTextAlign();

        // 计算文本尺寸
        // Correct sizes of text
        computeTextSize();

        mScroller = new Scroller(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            ViewConfiguration conf = ViewConfiguration.get(getContext());
            mMinimumVelocity = conf.getScaledMinimumFlingVelocity();
            mMaximumVelocity = conf.getScaledMaximumFlingVelocity();
            mTouchSlop = conf.getScaledTouchSlop();
        }
        mRectDrawn = new Rect();

        mRectIndicatorHead = new Rect();
        mRectIndicatorFoot = new Rect();

        mRectCurrentItem = new Rect();

        mCamera = new Camera();

        mMatrixRotate = new Matrix();
        mMatrixDepth = new Matrix();
    }

    private void updateVisibleItemCount() {
        if (mVisibleItemCount < 2)
            throw new ArithmeticException("Wheel's visible item count can not be less than 2!");

        // 确保滚轮选择器可见数据项数量为奇数
        // Be sure count of visible item is odd number
        if (mVisibleItemCount % 2 == 0)
            mVisibleItemCount += 1;
        mDrawnItemCount = mVisibleItemCount + 2;
        mHalfDrawnItemCount = mDrawnItemCount / 2;
    }

    private void computeTextSize() {
        mTextMaxWidth = mTextMaxHeight = 0;
        if (hasSameWidth) {
            mTextMaxWidth = (int) mPaint.measureText(String.valueOf(mData.get(0)));
        } else if (isPosInRang(mTextMaxWidthPosition)) {
            mTextMaxWidth = (int) mPaint.measureText
                    (String.valueOf(mData.get(mTextMaxWidthPosition)));
        } else if (!TextUtils.isEmpty(mMaxWidthText)) {
            mTextMaxWidth = (int) mPaint.measureText(mMaxWidthText);
        } else {
            for (Object obj : mData) {
                String text = String.valueOf(obj);
                int width = (int) mPaint.measureText(text);
                mTextMaxWidth = Math.max(mTextMaxWidth, width);
            }
        }
        Paint.FontMetrics metrics = mPaint.getFontMetrics();
        mTextMaxHeight = (int) (metrics.bottom - metrics.top);
    }

    private void updateItemTextAlign() {
        switch (mItemAlign) {
            case ALIGN_LEFT:
                mPaint.setTextAlign(Paint.Align.LEFT);
                break;
            case ALIGN_RIGHT:
                mPaint.setTextAlign(Paint.Align.RIGHT);
                break;
            default:
                mPaint.setTextAlign(Paint.Align.CENTER);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 计算原始内容尺寸
        // Correct sizes of original content
        int resultWidth = mTextMaxWidth;
        int resultHeight = mTextMaxHeight * mVisibleItemCount + mItemSpace * (mVisibleItemCount - 1);

        // 如果开启弯曲效果则需要重新计算弯曲后的尺寸
        // Correct view sizes again if curved is enable
        if (isCurved) {
            resultHeight = (int) (2 * resultHeight / Math.PI);
        }
        if (isDebug)
            Log.i(TAG, "Wheel's content size is (" + resultWidth + ":" + resultHeight + ")");

        // 考虑内边距对尺寸的影响
        // Consideration padding influence the view sizes
        resultWidth += getPaddingLeft() + getPaddingRight();
        resultHeight += getPaddingTop() + getPaddingBottom();
        if (isDebug)
            Log.i(TAG, "Wheel's size is (" + resultWidth + ":" + resultHeight + ")");

        // 考虑父容器对尺寸的影响
        // Consideration sizes of parent can influence the view sizes
        resultWidth = measureSize(modeWidth, sizeWidth, resultWidth);
        resultHeight = measureSize(modeHeight, sizeHeight, resultHeight);

        setMeasuredDimension(resultWidth, resultHeight);
    }

    private int measureSize(int mode, int sizeExpect, int sizeActual) {
        int realSize;
        if (mode == MeasureSpec.EXACTLY) {
            realSize = sizeExpect;
        } else {
            realSize = sizeActual;
            if (mode == MeasureSpec.AT_MOST)
                realSize = Math.min(realSize, sizeExpect);
        }
        return realSize;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        // 设置内容区域
        // Set content region
        mRectDrawn.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        if (isDebug)
            Log.i(TAG, "Wheel's drawn rect size is (" + mRectDrawn.width() + ":" +
                    mRectDrawn.height() + ") and location is (" + mRectDrawn.left + ":" +
                    mRectDrawn.top + ")");

        // 获取内容区域中心坐标
        // Get the center coordinates of content region
        mWheelCenterX = mRectDrawn.centerX();
        mWheelCenterY = mRectDrawn.centerY();

        // 计算数据项绘制中心
        // Correct item drawn center
        computeDrawnCenter();

        mHalfWheelHeight = mRectDrawn.height() / 2;

        mItemHeight = mRectDrawn.height() / mVisibleItemCount;
        mHalfItemHeight = mItemHeight / 2;

        // 初始化滑动最大坐标
        // Initialize fling max Y-coordinates
        computeFlingLimitY();

        // 计算指示器绘制区域
        // Correct region of indicator
        computeIndicatorRect();

        // 计算当前选中的数据项区域
        // Correct region of current select item
        computeCurrentItemRect();
    }

    private void computeDrawnCenter() {
        switch (mItemAlign) {
            case ALIGN_LEFT:
                mDrawnCenterX = mRectDrawn.left;
                break;
            case ALIGN_RIGHT:
                mDrawnCenterX = mRectDrawn.right;
                break;
            default:
                mDrawnCenterX = mWheelCenterX;
                break;
        }
        mDrawnCenterY = (int) (mWheelCenterY - ((mPaint.ascent() + mPaint.descent()) / 2));
    }

    private void computeFlingLimitY() {
        int currentItemOffset = mSelectedItemPosition * mItemHeight;
        mMinFlingY = isCyclic ? Integer.MIN_VALUE :
                -mItemHeight * (mData.size() - 1) + currentItemOffset;
        mMaxFlingY = isCyclic ? Integer.MAX_VALUE : currentItemOffset;
    }

    private void computeIndicatorRect() {
        if (!hasIndicator) return;
        int halfIndicatorSize = mIndicatorSize / 2;
        int indicatorHeadCenterY = mWheelCenterY + mHalfItemHeight;
        int indicatorFootCenterY = mWheelCenterY - mHalfItemHeight;
        mRectIndicatorHead.set(mRectDrawn.left, indicatorHeadCenterY - halfIndicatorSize,
                mRectDrawn.right, indicatorHeadCenterY + halfIndicatorSize);
        mRectIndicatorFoot.set(mRectDrawn.left, indicatorFootCenterY - halfIndicatorSize,
                mRectDrawn.right, indicatorFootCenterY + halfIndicatorSize);
    }

    private void computeCurrentItemRect() {
        if (!hasCurtain && mSelectedItemTextColor == -1) return;
        mRectCurrentItem.set(mRectDrawn.left, mWheelCenterY - mHalfItemHeight, mRectDrawn.right,
                mWheelCenterY + mHalfItemHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != mOnWheelChangeListener)
            mOnWheelChangeListener.onWheelScrolled(mScrollOffsetY);
        if(mData.size() == 0)
            return;
        int drawnDataStartPos = -mScrollOffsetY / mItemHeight - mHalfDrawnItemCount;
        for (int drawnDataPos = drawnDataStartPos + mSelectedItemPosition,
             drawnOffsetPos = -mHalfDrawnItemCount;
             drawnDataPos < drawnDataStartPos + mSelectedItemPosition + mDrawnItemCount;
             drawnDataPos++, drawnOffsetPos++) {
            String data = "";
            if (isCyclic) {
                int actualPos = drawnDataPos % mData.size();
                actualPos = actualPos < 0 ? (actualPos + mData.size()) : actualPos;
                data = String.valueOf(mData.get(actualPos));
            } else {
                if (isPosInRang(drawnDataPos))
                    data = String.valueOf(mData.get(drawnDataPos));
            }
            mPaint.setColor(mItemTextColor);
            mPaint.setStyle(Paint.Style.FILL);
            int mDrawnItemCenterY = mDrawnCenterY + (drawnOffsetPos * mItemHeight) +
                    mScrollOffsetY % mItemHeight;

            int distanceToCenter = 0;
            if (isCurved) {
                // 计算数据项绘制中心距离滚轮中心的距离比率
                // Correct ratio of item's drawn center to wheel center
                float ratio = (mDrawnCenterY - Math.abs(mDrawnCenterY - mDrawnItemCenterY) -
                        mRectDrawn.top) * 1.0F / (mDrawnCenterY - mRectDrawn.top);

                // 计算单位
                // Correct unit
                int unit = 0;
                if (mDrawnItemCenterY > mDrawnCenterY)
                    unit = 1;
                else if (mDrawnItemCenterY < mDrawnCenterY)
                    unit = -1;

                float degree = (-(1 - ratio) * 90 * unit);
                if (degree < -90) degree = -90;
                if (degree > 90) degree = 90;
                distanceToCenter = computeSpace((int) degree);

                int transX = mWheelCenterX;
                switch (mItemAlign) {
                    case ALIGN_LEFT:
                        transX = mRectDrawn.left;
                        break;
                    case ALIGN_RIGHT:
                        transX = mRectDrawn.right;
                        break;
                }
                int transY = mWheelCenterY - distanceToCenter;

                mCamera.save();
                mCamera.rotateX(degree);
                mCamera.getMatrix(mMatrixRotate);
                mCamera.restore();
                mMatrixRotate.preTranslate(-transX, -transY);
                mMatrixRotate.postTranslate(transX, transY);

                mCamera.save();
                mCamera.translate(0, 0, computeDepth((int) degree));
                mCamera.getMatrix(mMatrixDepth);
                mCamera.restore();
                mMatrixDepth.preTranslate(-transX, -transY);
                mMatrixDepth.postTranslate(transX, transY);

                mMatrixRotate.postConcat(mMatrixDepth);
            }
            if (hasAtmospheric) {
                int alpha = (int) ((mDrawnCenterY - Math.abs(mDrawnCenterY - mDrawnItemCenterY)) *
                        1.0F / mDrawnCenterY * 255);
                alpha = alpha < 0 ? 0 : alpha;
                mPaint.setAlpha(alpha);
            }
            // 根据卷曲与否计算数据项绘制Y方向中心坐标
            // Correct item's drawn centerY base on curved state
            int drawnCenterY = isCurved ? mDrawnCenterY - distanceToCenter : mDrawnItemCenterY;

            // 判断是否需要为当前数据项绘制不同颜色
            // Judges need to draw different color for current item or not
            if (mSelectedItemTextColor != -1) {
                canvas.save();
                if (isCurved) canvas.concat(mMatrixRotate);
                canvas.clipRect(mRectCurrentItem, Region.Op.DIFFERENCE);
                canvas.drawText(data, mDrawnCenterX, drawnCenterY, mPaint);
                canvas.restore();

                mPaint.setColor(mSelectedItemTextColor);
                canvas.save();
                if (isCurved) canvas.concat(mMatrixRotate);
                canvas.clipRect(mRectCurrentItem);
                canvas.drawText(data, mDrawnCenterX, drawnCenterY, mPaint);
                canvas.restore();
            } else {
                canvas.save();
                canvas.clipRect(mRectDrawn);
                if (isCurved) canvas.concat(mMatrixRotate);
                canvas.drawText(data, mDrawnCenterX, drawnCenterY, mPaint);
                canvas.restore();
            }
            if (isDebug) {
                canvas.save();
                canvas.clipRect(mRectDrawn);
                mPaint.setColor(0xFFEE3333);
                int lineCenterY = mWheelCenterY + (drawnOffsetPos * mItemHeight);
                canvas.drawLine(mRectDrawn.left, lineCenterY, mRectDrawn.right, lineCenterY,
                        mPaint);
                mPaint.setColor(0xFF3333EE);
                mPaint.setStyle(Paint.Style.STROKE);
                int top = lineCenterY - mHalfItemHeight;
                canvas.drawRect(mRectDrawn.left, top, mRectDrawn.right, top + mItemHeight, mPaint);
                canvas.restore();
            }
        }
        // 是否需要绘制幕布
        // Need to draw curtain or not
        if (hasCurtain) {
            mPaint.setColor(mCurtainColor);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(mRectCurrentItem, mPaint);
        }
        // 是否需要绘制指示器
        // Need to draw indicator or not
        if (hasIndicator) {
            mPaint.setColor(mIndicatorColor);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(mRectIndicatorHead, mPaint);
            canvas.drawRect(mRectIndicatorFoot, mPaint);
        }
        if (isDebug) {
            mPaint.setColor(0x4433EE33);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, getPaddingLeft(), getHeight(), mPaint);
            canvas.drawRect(0, 0, getWidth(), getPaddingTop(), mPaint);
            canvas.drawRect(getWidth() - getPaddingRight(), 0, getWidth(), getHeight(), mPaint);
            canvas.drawRect(0, getHeight() - getPaddingBottom(), getWidth(), getHeight(), mPaint);
        }
    }

    private boolean isPosInRang(int position) {
        return position >= 0 && position < mData.size();
    }

    private int computeSpace(int degree) {
        return (int) (Math.sin(Math.toRadians(degree)) * mHalfWheelHeight);
    }

    private int computeDepth(int degree) {
        return (int) (mHalfWheelHeight - Math.cos(Math.toRadians(degree)) * mHalfWheelHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouchTriggered = true;
                if (null != getParent())
                    getParent().requestDisallowInterceptTouchEvent(true);
                if (null == mTracker)
                    mTracker = VelocityTracker.obtain();
                else
                    mTracker.clear();
                mTracker.addMovement(event);
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    isForceFinishScroll = true;
                }
                mDownPointY = mLastPointY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(mDownPointY - event.getY()) < mTouchSlop) {
                    isClick = true;
                    break;
                }
                isClick = false;
                mTracker.addMovement(event);
                if (null != mOnWheelChangeListener)
                    mOnWheelChangeListener.onWheelScrollStateChanged(SCROLL_STATE_DRAGGING);

                // 滚动内容
                // Scroll WheelPicker's content
                float move = event.getY() - mLastPointY;
                if (Math.abs(move) < 1) break;
                mScrollOffsetY += move;
                mLastPointY = (int) event.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (null != getParent())
                    getParent().requestDisallowInterceptTouchEvent(false);
                if (isClick && !isForceFinishScroll) break;
                mTracker.addMovement(event);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT)
                    mTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                else
                    mTracker.computeCurrentVelocity(1000);

                // 根据速度判断是该滚动还是滑动
                // Judges the WheelPicker is scroll or fling base on current velocity
                isForceFinishScroll = false;
                int velocity = (int) mTracker.getYVelocity();
                if (Math.abs(velocity) > mMinimumVelocity) {
                    mScroller.fling(0, mScrollOffsetY, 0, velocity, 0, 0, mMinFlingY, mMaxFlingY);
                    mScroller.setFinalY(mScroller.getFinalY() +
                            computeDistanceToEndPoint(mScroller.getFinalY() % mItemHeight));
                } else {
                    mScroller.startScroll(0, mScrollOffsetY, 0,
                            computeDistanceToEndPoint(mScrollOffsetY % mItemHeight));
                }
                // 校正坐标
                // Correct coordinates
                if (!isCyclic)
                    if (mScroller.getFinalY() > mMaxFlingY)
                        mScroller.setFinalY(mMaxFlingY);
                    else if (mScroller.getFinalY() < mMinFlingY)
                        mScroller.setFinalY(mMinFlingY);
                mHandler.post(this);
                if (null != mTracker) {
                    mTracker.recycle();
                    mTracker = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (null != getParent())
                    getParent().requestDisallowInterceptTouchEvent(false);
                if (null != mTracker) {
                    mTracker.recycle();
                    mTracker = null;
                }
                break;
        }
        return true;
    }

    private int computeDistanceToEndPoint(int remainder) {
        if (Math.abs(remainder) > mHalfItemHeight)
            if (mScrollOffsetY < 0)
                return -mItemHeight - remainder;
            else
                return mItemHeight - remainder;
        else
            return -remainder;
    }

    @Override
    public void run() {
        if (null == mData || mData.size() == 0) return;
        if (mScroller.isFinished() && !isForceFinishScroll) {
            if (mItemHeight == 0) return;
            int position = (-mScrollOffsetY / mItemHeight + mSelectedItemPosition) % mData.size();
            position = position < 0 ? position + mData.size() : position;
            if (isDebug)
                Log.i(TAG, position + ":" + mData.get(position) + ":" + mScrollOffsetY);
            mCurrentItemPosition = position;
            if (null != mOnItemSelectedListener && isTouchTriggered)
                mOnItemSelectedListener.onItemSelected(this, mData.get(position), position);
            if (null != mOnWheelChangeListener && isTouchTriggered) {
                mOnWheelChangeListener.onWheelSelected(position);
                mOnWheelChangeListener.onWheelScrollStateChanged(SCROLL_STATE_IDLE);
            }
        }
        if (mScroller.computeScrollOffset()) {
            if (null != mOnWheelChangeListener)
                mOnWheelChangeListener.onWheelScrollStateChanged(SCROLL_STATE_SCROLLING);
            mScrollOffsetY = mScroller.getCurrY();
            postInvalidate();
            mHandler.postDelayed(this, 16);
        }
    }

    @Override
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    @Override
    public int getVisibleItemCount() {
        return mVisibleItemCount;
    }

    @Override
    public void setVisibleItemCount(int count) {
        mVisibleItemCount = count;
        updateVisibleItemCount();
        requestLayout();
    }

    @Override
    public boolean isCyclic() {
        return isCyclic;
    }

    @Override
    public void setCyclic(boolean isCyclic) {
        this.isCyclic = isCyclic;
        computeFlingLimitY();
        invalidate();
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    @Override
    public int getSelectedItemPosition() {
        return mSelectedItemPosition;
    }

    @Override
    public void setSelectedItemPosition(int position) {
        setSelectedItemPosition(position, true);
    }

    public void setSelectedItemPosition(int position, final boolean animated) {
      isTouchTriggered = false;
      if (animated && mScroller.isFinished()) { // We go non-animated regardless of "animated" parameter if scroller is in motion
        int length = getData().size();
        int itemDifference = position - mCurrentItemPosition;
        if (itemDifference == 0)
          return;
        if (isCyclic && Math.abs(itemDifference) > (length / 2)) { // Find the shortest path if it's cyclic
          itemDifference += (itemDifference > 0) ? -length : length;
        }
        mScroller.startScroll(0, mScroller.getCurrY(), 0, (-itemDifference) * mItemHeight);
        mHandler.post(this);
      } else {
        if (!mScroller.isFinished())
          mScroller.abortAnimation();
        position = Math.min(position, mData.size() - 1);
        position = Math.max(position, 0);
        mSelectedItemPosition = position;
        mCurrentItemPosition = position;
        mScrollOffsetY = 0;
        computeFlingLimitY();
        requestLayout();
        invalidate();
      }
    }

    @Override
    public int getCurrentItemPosition() {
        return mCurrentItemPosition;
    }

    @Override
    public List getData() {
        return mData;
    }

    @Override
    public void setData(List data) {
        if (null == data)
            throw new NullPointerException("WheelPicker's data can not be null!");
        mData = data;

        if (mSelectedItemPosition > data.size() - 1 || mCurrentItemPosition > data.size() - 1) {
            mSelectedItemPosition = mCurrentItemPosition = data.size() - 1;
        } else {
            mSelectedItemPosition = mCurrentItemPosition;
        }
        mScrollOffsetY = 0;
        computeTextSize();
        computeFlingLimitY();
        requestLayout();
        invalidate();
    }

    public void setSameWidth(boolean hasSameWidth) {
        this.hasSameWidth = hasSameWidth;
        computeTextSize();
        requestLayout();
        invalidate();
    }

    @Override
    public boolean hasSameWidth() {
        return hasSameWidth;
    }

    @Override
    public void setOnWheelChangeListener(OnWheelChangeListener listener) {
        mOnWheelChangeListener = listener;
    }

    @Override
    public String getMaximumWidthText() {
        return mMaxWidthText;
    }

    @Override
    public void setMaximumWidthText(String text) {
        if (null == text)
            throw new NullPointerException("Maximum width text can not be null!");
        mMaxWidthText = text;
        computeTextSize();
        requestLayout();
        invalidate();
    }

    @Override
    public int getMaximumWidthTextPosition() {
        return mTextMaxWidthPosition;
    }

    @Override
    public void setMaximumWidthTextPosition(int position) {
        if (!isPosInRang(position))
            throw new ArrayIndexOutOfBoundsException("Maximum width text Position must in [0, " +
                    mData.size() + "), but current is " + position);
        mTextMaxWidthPosition = position;
        computeTextSize();
        requestLayout();
        invalidate();
    }

    @Override
    public int getSelectedItemTextColor() {
        return mSelectedItemTextColor;
    }

    @Override
    public void setSelectedItemTextColor(int color) {
        mSelectedItemTextColor = color;
        computeCurrentItemRect();
        invalidate();
    }

    @Override
    public int getItemTextColor() {
        return mItemTextColor;
    }

    @Override
    public void setItemTextColor(int color) {
        mItemTextColor = color;
        invalidate();
    }

    @Override
    public int getItemTextSize() {
        return mItemTextSize;
    }

    @Override
    public void setItemTextSize(int size) {
        mItemTextSize = size;
        mPaint.setTextSize(mItemTextSize);
        computeTextSize();
        requestLayout();
        invalidate();
    }

    @Override
    public int getItemSpace() {
        return mItemSpace;
    }

    @Override
    public void setItemSpace(int space) {
        mItemSpace = space;
        requestLayout();
        invalidate();
    }

    @Override
    public void setIndicator(boolean hasIndicator) {
        this.hasIndicator = hasIndicator;
        computeIndicatorRect();
        invalidate();
    }

    @Override
    public boolean hasIndicator() {
        return hasIndicator;
    }

    @Override
    public int getIndicatorSize() {
        return mIndicatorSize;
    }

    @Override
    public void setIndicatorSize(int size) {
        mIndicatorSize = size;
        computeIndicatorRect();
        invalidate();
    }

    @Override
    public int getIndicatorColor() {
        return mIndicatorColor;
    }

    @Override
    public void setIndicatorColor(int color) {
        mIndicatorColor = color;
        invalidate();
    }

    @Override
    public void setCurtain(boolean hasCurtain) {
        this.hasCurtain = hasCurtain;
        computeCurrentItemRect();
        invalidate();
    }

    @Override
    public boolean hasCurtain() {
        return hasCurtain;
    }

    @Override
    public int getCurtainColor() {
        return mCurtainColor;
    }

    @Override
    public void setCurtainColor(int color) {
        mCurtainColor = color;
        invalidate();
    }

    @Override
    public void setAtmospheric(boolean hasAtmospheric) {
        this.hasAtmospheric = hasAtmospheric;
        invalidate();
    }

    @Override
    public boolean hasAtmospheric() {
        return hasAtmospheric;
    }

    @Override
    public boolean isCurved() {
        return isCurved;
    }

    @Override
    public void setCurved(boolean isCurved) {
        this.isCurved = isCurved;
        requestLayout();
        invalidate();
    }

    @Override
    public int getItemAlign() {
        return mItemAlign;
    }

    @Override
    public void setItemAlign(int align) {
        mItemAlign = align;
        updateItemTextAlign();
        computeDrawnCenter();
        invalidate();
    }

    @Override
    public Typeface getTypeface() {
        if (null != mPaint)
            return mPaint.getTypeface();
        return null;
    }

    @Override
    public void setTypeface(Typeface tf) {
        if (null != mPaint)
            mPaint.setTypeface(tf);
        computeTextSize();
        requestLayout();
        invalidate();
    }

    public interface OnItemSelectedListener {
        void onItemSelected(WheelPicker picker, Object data, int position);
    }

    public interface OnWheelChangeListener {
          
        void onWheelScrolled(int offset);

        void onWheelSelected(int position);

        void onWheelScrollStateChanged(int state);
    }
}

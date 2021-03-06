package com.epam.android.intervalslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class IntervalSliderView extends View implements View.OnTouchListener {

    public static final String BUNDLE_SUPER_INSTANCE_STATE = "super_instance_state";
    public static final String BUNDLE_INTERVAL_SLIDER_PROGRESS = "interval_slider_progress";

    private OnValueChangedListener listener;

    private int backgroundDrawableResourceId = R.drawable.interval_selector_background;
    private int selectionDrawableResourceId = R.drawable.interval_selector_selection_default;
    private int thumbDrawableResourceId = R.drawable.interval_selector_thumb;
    private int thumbPressedDrawableResourceId = R.drawable.interval_selector_thumb_pressed;

    private int thumbSize = getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_thumb_size);
    private int thumbPressedSize = getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_thumb_pressed_size);

    private Drawable background;
    private Drawable selection;
    private Drawable thumb;
    private Drawable thumbPressed;

//    private static final int[] TOUCH_DOWN_STATE_SET = {
//            R.attr.state_touch_down
//    };

    // [0, 1]
    private float progress[] = new float[2];
    private boolean[] thumbIsDown = new boolean[2];

    private int pointerIds[] = new int[2];
    private int thumbDownOffsetX[] = new int[2];
    private int pointerDownX[] = new int[2];
    private int pointerDownY[] = new int[2];

    private int touchOffset = getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_touchoffset);
    private int offset = getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_offset);

    //region constructors
    public IntervalSliderView(Context context) {
        super(context);
        init(null, 0);
    }

    public IntervalSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public IntervalSliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }
    //endregion

    private void init(AttributeSet attrs, int defStyle) {
        Log.d("init touchOffset " + touchOffset);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.IntervalSliderView, defStyle, 0);
        try {
            thumbSize = typedArray.getDimensionPixelSize(R.styleable.IntervalSliderView_thumbSize, thumbSize);
            thumbPressedSize = typedArray.getDimensionPixelSize(R.styleable.IntervalSliderView_thumbPressedSize, thumbPressedSize);
            touchOffset = typedArray.getDimensionPixelSize(R.styleable.IntervalSliderView_touchOffset, touchOffset);
            offset = typedArray.getDimensionPixelSize(R.styleable.IntervalSliderView_offset, offset);

            backgroundDrawableResourceId = typedArray.getResourceId(R.styleable.IntervalSliderView_backgroundDrawable, backgroundDrawableResourceId);
            selectionDrawableResourceId = typedArray.getResourceId(R.styleable.IntervalSliderView_selectionDrawable, selectionDrawableResourceId);
            thumbDrawableResourceId = typedArray.getResourceId(R.styleable.IntervalSliderView_thumbDrawable, thumbDrawableResourceId);
            thumbPressedDrawableResourceId = typedArray.getResourceId(R.styleable.IntervalSliderView_thumbPressedDrawable, thumbPressedDrawableResourceId);

        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d("onAttachedToWindow");
        background = getContext().getResources().getDrawable(backgroundDrawableResourceId);
        selection = getContext().getResources().getDrawable(selectionDrawableResourceId);
        thumb = getContext().getResources().getDrawable(thumbDrawableResourceId);
        thumbPressed = getContext().getResources().getDrawable(thumbPressedDrawableResourceId);
        setOnTouchListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("onDetachedToWindow");
        setOnTouchListener(null);
    }

    private int getSliderWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight() - 2 * offset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        Log.d("onDraw paddingLeft " + paddingLeft + " paddingTop " + paddingTop + " paddingRight " + paddingRight + " paddingBottom " + paddingBottom);

        int contentWidth = getWidth() - paddingLeft - paddingRight - 2 * offset;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        Log.d("onDraw contentWidth " + contentWidth + " contentHeight " + contentHeight);

        background.setBounds(getPaddingLeft() + offset, getPaddingTop(), getPaddingLeft() + contentWidth, getPaddingTop() + contentHeight);
        background.draw(canvas);

        int[] thumbX = new int[]{
                offset + paddingLeft + (int) (contentWidth * progress[0]),
                offset + paddingLeft + (int) (contentWidth * progress[1])
        };

        selection.setBounds(
                thumbX[0],
                paddingTop,
                thumbX[1],
                paddingTop + contentHeight
        );
        selection.draw(canvas);

        for (int i = 0; i < 2; i++) {
            Drawable thumbDrawable = thumbIsDown[i] ? thumbPressed : thumb;
            int size = thumbIsDown[i] ? thumbPressedSize : thumbSize;
            thumbDrawable.setBounds(
                    thumbX[i] - size / 2,
                    (getHeight() - size) / 2,
                    thumbX[i] - size / 2 + size,
                    (getHeight() - size) / 2 + size
            );
            thumbDrawable.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int retWidth, retHeight;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        Log.d("onMeasure widthMode " + widthMode + " heightMode " + heightMode + " widthSize " + widthSize + " heightSize " + heightSize);

        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            retWidth = widthSize;
        } else {
            retWidth = getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_width);
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            retHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            retHeight = Math.min(heightSize, getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_height));
        } else {
            retHeight = getContext().getResources().getDimensionPixelSize(R.dimen.interval_slider_default_height) + getPaddingTop() + getPaddingBottom();
        }

        setMeasuredDimension(retWidth, retHeight);
    }

    int[] mPreTouchDownState;

    private static final boolean STATE_HANDLE = true;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("onTouch " + event.getAction());
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            mPreTouchDownState = background.getState();
//            background.setState(View.PRESSED_ENABLED_STATE_SET);
////            invalidate();
//        }
//
//        if (event.getAction() == MotionEvent.ACTION_UP) {
//            background.setState(mPreTouchDownState);
////            invalidate();
//        }

        final int action = MotionEventCompat.getActionMasked(event);
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final int pointerCount = event.getPointerCount();
        int pointerId = event.getPointerId(pointerIndex);

        Log.d("onTouch " + Utils.actionToString(action) + " (" + pointerId + ")" + pointerIndex + "/" + pointerCount);

        final int x = (int) event.getX(pointerIndex);
        final int y = (int) event.getY(pointerIndex);

        int[] thumbX = new int[]{
                offset + (int) (getSliderWidth() * progress[0]),
                offset + (int) (getSliderWidth() * progress[1])
        };

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {

                if (STATE_HANDLE) {
                    mPreTouchDownState = background.getState();
                    background.setState(View.PRESSED_ENABLED_STATE_SET);
                }

                int thumbToTrack = -1;
                if (Math.abs(thumbX[0] - x) < touchOffset && Math.abs(thumbX[0] - x) <= Math.abs(thumbX[1] - x)) {
                    thumbToTrack = 0;
                }
                if (Math.abs(thumbX[1] - x) < touchOffset && Math.abs(thumbX[1] - x) <= Math.abs(thumbX[0] - x)) {
                    thumbToTrack = 1;
                }

                if (thumbToTrack != -1) {
                    pointerIds[thumbToTrack] = pointerId;
                    thumbIsDown[thumbToTrack] = true;
                    thumbDownOffsetX[thumbToTrack] = x - thumbX[thumbToTrack];
                    pointerDownX[thumbToTrack] = x;
                    pointerDownY[thumbToTrack] = y;
                }

//                direction = Direction.UNKNOWN;
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                for (int j = 0; j < pointerCount; j++) {
                    int id = event.getPointerId(j);
                    int tx = (int) event.getX(j);
                    int ty = (int) event.getY(j);
                    for (int i = 0; i < 2; i++) {
                        if (thumbIsDown[i] && id == pointerIds[i]) {
                            int newX = tx - thumbDownOffsetX[i];
                            progress[i] = (float) (newX - offset) / getSliderWidth();

                            if (progress[i] < 0)
                                progress[i] = 0;
                            if (progress[i] > 1)
                                progress[i] = 1;

//                            int dx = pointerDownX[i] - tx;
//                            int dy = pointerDownY[i] - ty;
//
//                            if (direction == Direction.UNKNOWN && (Math.abs(dx) > DIRECTION_DETECTION_GAP || Math.abs(dy) > DIRECTION_DETECTION_GAP)) {
//                                if (Math.abs(dy) > Math.abs(dx)) {
//                                    direction = Direction.VERTICAL;
//                                    return false;
//                                } else {
//                                    direction = Direction.HORIZONTAL;
//
//                                    //значит, скроллить нельзя
//                                    ScrollView underlyingScrollView = this.underlyingScrollView();
//                                    if (underlyingScrollView != null) {
//                                        underlyingScrollView.requestDisallowInterceptTouchEvent(true);
//                                    }
//                                }
//                            }
                        }

                        if (i == 0 && progress[0] > progress[1])
                            progress[1] = progress[0];

                        if (i == 1 && progress[0] > progress[1])
                            progress[0] = progress[1];

                    }
                }
            }
            break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {

                if (STATE_HANDLE) {
                    background.setState(mPreTouchDownState);
                }

                for (int i = 0; i < 2; i++) {
                    if (thumbIsDown[i] && pointerId == pointerIds[i]) {
                        thumbIsDown[i] = false;
                        pointerIds[i] = -1;
                    }
                }
            }
            break;
        }

        postInvalidate();

        Log.d("value changed " + progress[0] + " " + progress[1] + " listener " + listener + " this " + this);

        if (listener != null)
            listener.onValueChanged(this, progress[0], progress[1]);

        return true;
    }

    public void setListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    public static interface OnValueChangedListener {
        public void onValueChanged(IntervalSliderView slider, float minIntervalValue, float maxIntervalValue);
    }

    //region save/restore
    @Override
    public Parcelable onSaveInstanceState() {
        Log.d("interval_slider onSaveInstanceState");
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_SUPER_INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloatArray(BUNDLE_INTERVAL_SLIDER_PROGRESS, progress);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Log.d("interval_slider onRestoreInstanceState");
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            progress = bundle.getFloatArray(BUNDLE_INTERVAL_SLIDER_PROGRESS);
            state = bundle.getParcelable(BUNDLE_SUPER_INSTANCE_STATE);
        }
        super.onRestoreInstanceState(state);
    }
    //endregion

    //region getters/setters
    public float getMinProgress() {
        return progress[0];
    }

    public float getMaxProgress() {
        return progress[1];
    }

    private float getValidProgress(float progress) {
        float ret;
        if (progress > 1) ret = 1;
        else if (progress < 0) ret = 0;
        else
            ret = progress;

        return ret;
    }

    public void setMinProgress(float value) {
        progress[0] = getValidProgress(value);
        Log.d("setMinProgress " + value + " -> " + progress[0]);
    }

    public void setMaxProgress(float value) {
        progress[1] = getValidProgress(value);
        Log.d("setMaxProgress " + value + " -> " + progress[1]);
    }
    //endregion
}

/*
 * Copyright 2014 Alex Curran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.amlcurran.showcaseview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.IntDef;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.targets.Target;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationEndListener;
import static com.github.amlcurran.showcaseview.AnimationFactory.AnimationStartListener;

/**
 * A view which allows you to showcase areas of your app with an explanation.
 */
public class ShowcaseView extends RelativeLayout
        implements View.OnTouchListener, ShowcaseViewApi {

    private static final int HOLO_BLUE = Color.parseColor("#33B5E5");
    public static final int UNDEFINED = -1;
    public static final int LEFT_OF_SHOWCASE = 0;
    public static final int RIGHT_OF_SHOWCASE = 2;
    public static final int ABOVE_SHOWCASE = 1;
    public static final int BELOW_SHOWCASE = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UNDEFINED, LEFT_OF_SHOWCASE, RIGHT_OF_SHOWCASE, ABOVE_SHOWCASE, BELOW_SHOWCASE})
    public @interface TextPosition {
    }

    private ImageButton mEndButton;
    private final TextDrawer textDrawer;
    private ShowcaseDrawer showcaseDrawer;
    private final ShowcaseAreaCalculator showcaseAreaCalculator;
    private final AnimationFactory animationFactory;
    private final ShotStateStore shotStateStore;

    // Showcase metrics
    private int showcaseX = -1;
    private int showcaseY = -1;
    private float scaleMultiplier = 1f;

    // Touch items
    private boolean hasCustomClickListener = false;
    private boolean blockTouches = true;
    private boolean hideOnTouch = false;
    private OnShowcaseEventListener mEventListener = OnShowcaseEventListener.NONE;

    private boolean hasAlteredText = false;
    private boolean hasNoTarget = false;
    private boolean shouldCentreText;
    private Bitmap bitmapBuffer;

    // Animation items
    private long fadeInMillis;
    private long fadeOutMillis;
    private boolean isShowing;
    private int backgroundColor;
    private int showcaseColor;
    private boolean blockAllTouches;
    private final int[] positionInWindow = new int[2];

    protected ShowcaseView(Context context, boolean newStyle) {
        this(context, null, R.styleable.CustomTheme_showcaseViewStyle, newStyle);
    }

    protected ShowcaseView(Context context, AttributeSet attrs, int defStyle, boolean newStyle) {
        super(context, attrs, defStyle);

        ApiUtils apiUtils = new ApiUtils();
        if (apiUtils.isCompatWithHoneycomb()) {
            animationFactory = new AnimatorAnimationFactory();
        } else {
            animationFactory = new NoAnimationFactory();
        }
        showcaseAreaCalculator = new ShowcaseAreaCalculator();
        shotStateStore = new ShotStateStore(context);

        // Get the attributes for the ShowcaseView
        final TypedArray styled = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.ShowcaseView, R.attr.showcaseViewStyle,
                R.style.ShowcaseView);

        // Set the default animation times
        fadeInMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        fadeOutMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mEndButton = (ImageButton) LayoutInflater.from(context).inflate(R.layout.showcase_button, null);
        if (newStyle) {
            showcaseDrawer = new NewShowcaseDrawer(getResources(), context.getTheme(), this);
        } else {
            showcaseDrawer = new StandardShowcaseDrawer(getResources(), context.getTheme());
        }
        textDrawer = new TextDrawer(getResources(), getContext());

        updateStyle(styled, false, null, null);

        init();
    }

    private void init() {

        setOnTouchListener(this);

        if (mEndButton.getParent() == null) {
            int topMargin = (int) getResources().getDimension(R.dimen.skip_button_top_margin);
            int sideMargin = (int) getResources().getDimension(R.dimen.skip_button_side_margin);
            RelativeLayout.LayoutParams lps = (LayoutParams) generateDefaultLayoutParams();
            lps.setMargins(sideMargin, topMargin, sideMargin, topMargin);
            mEndButton.setLayoutParams(lps);
            if (!hasCustomClickListener) {
                mEndButton.setOnClickListener(hideOnClickListener);
            }
            addView(mEndButton);
        }

    }

    private boolean hasShot() {
        return shotStateStore.hasShot();
    }

    void setShowcasePosition(Point point) {
        setShowcasePosition(point.x, point.y);
    }

    void setShowcasePosition(int x, int y) {
        if (shotStateStore.hasShot()) {
            return;
        }
        getLocationInWindow(positionInWindow);
        showcaseX = x - positionInWindow[0];
        showcaseY = y - positionInWindow[1];
        //init();
        recalculateText();
        invalidate();
    }

    public void setTarget(final Target target) {
        setShowcase(target, false);
    }

    public void setShowcase(final Target target, final boolean animate) {
        postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!shotStateStore.hasShot()) {

                    if (canUpdateBitmap()) {
                        updateBitmap();
                    }

                    Point targetPoint = target.getPoint();
                    if (targetPoint != null) {
                        hasNoTarget = false;
                        if (animate) {
                            animationFactory.animateTargetToPoint(ShowcaseView.this, targetPoint);
                        } else {
                            setShowcasePosition(targetPoint);
                        }
                    } else {
                        hasNoTarget = true;
                        invalidate();
                    }

                }
            }
        }, 100);
    }

    private void updateBitmap() {
        if (bitmapBuffer == null || haveBoundsChanged()) {
            if (bitmapBuffer != null) {
                bitmapBuffer.recycle();
            }
            bitmapBuffer = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        }
    }

    private boolean haveBoundsChanged() {
        return getMeasuredWidth() != bitmapBuffer.getWidth() ||
                getMeasuredHeight() != bitmapBuffer.getHeight();
    }

    public boolean hasShowcaseView() {
        return (showcaseX != 1000000 && showcaseY != 1000000) && !hasNoTarget;
    }

    public void setShowcaseX(int x) {
        setShowcasePosition(x, getShowcaseY());
    }

    public void setShowcaseY(int y) {
        setShowcasePosition(getShowcaseX(), y);
    }

    public int getShowcaseX() {
        getLocationInWindow(positionInWindow);
        return showcaseX + positionInWindow[0];
    }

    public int getShowcaseY() {
        getLocationInWindow(positionInWindow);
        return showcaseY + positionInWindow[1];
    }

    /**
     * Override the standard button click event
     *
     * @param listener Listener to listen to on click events
     */
    public void overrideButtonClick(OnClickListener listener) {
        if (shotStateStore.hasShot()) {
            return;
        }
        if (mEndButton != null) {
            if (listener != null) {
                mEndButton.setOnClickListener(listener);
            } else {
                mEndButton.setOnClickListener(hideOnClickListener);
            }
        }
        hasCustomClickListener = true;
    }

    public void setOnShowcaseEventListener(OnShowcaseEventListener listener) {
        if (listener != null) {
            mEventListener = listener;
        } else {
            mEventListener = OnShowcaseEventListener.NONE;
        }
    }

    private void recalculateText() {
        boolean recalculatedCling = showcaseAreaCalculator.calculateShowcaseRect(showcaseX, showcaseY, showcaseDrawer);
        boolean recalculateText = recalculatedCling || hasAlteredText;
        if (recalculateText) {
            Rect rect = hasShowcaseView() ? showcaseAreaCalculator.getShowcaseRect() : new Rect();
            textDrawer.calculateTextPosition(getMeasuredWidth(), getMeasuredHeight(), shouldCentreText, rect);
        }
        hasAlteredText = false;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (showcaseX < 0 || showcaseY < 0 || shotStateStore.hasShot() || bitmapBuffer == null) {
            super.dispatchDraw(canvas);
            return;
        }

        //Draw background color
        showcaseDrawer.erase(bitmapBuffer);

        // Draw the showcase drawable
        if (!hasNoTarget) {
            showcaseDrawer.drawShowcase(bitmapBuffer, showcaseX, showcaseY, scaleMultiplier);
            showcaseDrawer.drawToCanvas(canvas, bitmapBuffer);
        }

        // Draw the text on the screen, recalculating its position if necessary
        textDrawer.draw(canvas);

        super.dispatchDraw(canvas);

    }

    @Override
    public void hide() {
        // If the type is set to one-shot, store that it has shot
        shotStateStore.storeShot();
        mEventListener.onShowcaseViewHide(this);
        fadeOutShowcase();
    }

    private void clearBitmap() {
        if (bitmapBuffer != null && !bitmapBuffer.isRecycled()) {
            bitmapBuffer.recycle();
            bitmapBuffer = null;
        }
    }

    private void fadeOutShowcase() {
        animationFactory.fadeOutView(
                this, fadeOutMillis, new AnimationEndListener() {
                    @Override
                    public void onAnimationEnd() {
                        setVisibility(View.GONE);
                        clearBitmap();
                        isShowing = false;
                        mEventListener.onShowcaseViewDidHide(ShowcaseView.this);
                    }
                }
        );
    }

    @Override
    public void show() {
        isShowing = true;
        if (canUpdateBitmap()) {
            updateBitmap();
        }
        mEventListener.onShowcaseViewShow(this);
        fadeInShowcase();
    }

    private boolean canUpdateBitmap() {
        return getMeasuredHeight() > 0 && getMeasuredWidth() > 0;
    }

    private void fadeInShowcase() {
        animationFactory.fadeInView(
                this, fadeInMillis,
                new AnimationStartListener() {
                    @Override
                    public void onAnimationStart() {
                        setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (blockAllTouches) {
            mEventListener.onShowcaseViewTouchBlocked(motionEvent);
            return true;
        }

        float xDelta = Math.abs(motionEvent.getRawX() - showcaseX);
        float yDelta = Math.abs(motionEvent.getRawY() - showcaseY);
        double distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2) + Math.pow(yDelta, 2));

        if (MotionEvent.ACTION_UP == motionEvent.getAction() &&
                hideOnTouch && distanceFromFocus > showcaseDrawer.getBlockedRadius()) {
            this.hide();
            return true;
        }

        boolean blocked = blockTouches && distanceFromFocus > showcaseDrawer.getBlockedRadius();
        if (blocked) {
            mEventListener.onShowcaseViewTouchBlocked(motionEvent);
        }
        return blocked;
    }

    private static void insertShowcaseView(ShowcaseView showcaseView, ViewGroup parent, int parentIndex) {
        parent.addView(showcaseView, parentIndex);
        if (!showcaseView.hasShot()) {
            showcaseView.show();
        } else {
            showcaseView.hideImmediate();
        }
    }

    private void hideImmediate() {
        isShowing = false;
        setVisibility(GONE);
    }

    @Override
    public void setContentTitle(CharSequence title) {
        textDrawer.setContentTitle(title);
        invalidate();
    }

    @Override
    public void setContentText(CharSequence text) {
        textDrawer.setContentText(text);
        invalidate();
    }

    private void setScaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier;
    }

    public void hideButton() {
        mEndButton.setVisibility(GONE);
    }

    public void showButton() {
        mEndButton.setVisibility(VISIBLE);
    }

    /**
     * Builder class which allows easier creation of {@link ShowcaseView}s.
     * It is recommended that you use this Builder class.
     */
    public static class Builder {

        private final ShowcaseView showcaseView;
        private final Activity activity;

        private ViewGroup parent;
        private int parentIndex;
        private int innerRadiusDimen;
        private int outerRadiusDimen;

        public Builder(Activity activity) {
            this(activity, false);
        }

        /**
         * @param useNewStyle should use "new style" showcase (see {@link #withNewStyleShowcase()}
         * @deprecated use {@link #withHoloShowcase()}, {@link #withNewStyleShowcase()}, or
         * {@link #setShowcaseDrawer(ShowcaseDrawer)}
         */
        @Deprecated
        public Builder(Activity activity, boolean useNewStyle) {
            this.activity = activity;
            this.showcaseView = new ShowcaseView(activity, useNewStyle);
            this.showcaseView.setTarget(Target.NONE);
            this.parent = (ViewGroup) activity.findViewById(android.R.id.content);
            this.parentIndex = parent.getChildCount();
            this.innerRadiusDimen = R.dimen.showcase_radius_inner;
            this.outerRadiusDimen = R.dimen.showcase_radius_outer;
        }

        /**
         * Create the {@link com.github.amlcurran.showcaseview.ShowcaseView} and show it.
         *
         * @return the created ShowcaseView
         */
        public ShowcaseView build() {
            insertShowcaseView(showcaseView, parent, parentIndex);
            return showcaseView;
        }

        /**
         * Draw a holo-style showcase. This is the default.<br/>
         * <img alt="Holo showcase example" src="../../../../../../../../example2.png" />
         */
        public Builder withHoloShowcase() {
            return setShowcaseDrawer(new StandardShowcaseDrawer(activity.getResources(), activity.getTheme()));
        }

        /**
         * Draw a new-style showcase.<br/>
         * <img alt="Holo showcase example" src="../../../../../../../../example.png" />
         */
        public Builder withNewStyleShowcase() {
            return setShowcaseDrawer(new NewShowcaseDrawer(activity.getResources(), activity.getTheme()));
        }

        /**
         * Draw a material style showcase.
         * <img alt="Material showcase" src="../../../../../../../../material.png" />
         */
        public Builder withMaterialShowcase() {
            return setShowcaseDrawer(new MaterialShowcaseDrawer(activity.getResources()));
        }

        public Builder withInnerRadius(int innerRadiusDimen) {
            this.innerRadiusDimen = innerRadiusDimen;
            if (this.showcaseView.showcaseDrawer != null && this.showcaseView.showcaseDrawer instanceof NewShowcaseDrawer) {
                ((NewShowcaseDrawer) this.showcaseView.showcaseDrawer).setInnerRadius(innerRadiusDimen);
            }
            return this;
        }

        public Builder withOuterRadius(int outerRadiusDimen) {
            this.outerRadiusDimen = outerRadiusDimen;
            if (this.showcaseView.showcaseDrawer != null && this.showcaseView.showcaseDrawer instanceof NewShowcaseDrawer) {
                ((NewShowcaseDrawer) this.showcaseView.showcaseDrawer).setOuterRadius(outerRadiusDimen);
            }
            return this;
        }

        /**
         * Set a custom showcase drawer which will be responsible for measuring and drawing the showcase
         */
        public Builder setShowcaseDrawer(ShowcaseDrawer showcaseDrawer) {
            showcaseView.setShowcaseDrawer(showcaseDrawer);
            if (this.showcaseView.showcaseDrawer instanceof NewShowcaseDrawer) {
                ((NewShowcaseDrawer) this.showcaseView.showcaseDrawer).setInnerRadius(this.innerRadiusDimen);
                ((NewShowcaseDrawer) this.showcaseView.showcaseDrawer).setOuterRadius(this.outerRadiusDimen);
            }
            return this;
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setContentTitle(int resId) {
            return setContentTitle(activity.getString(resId));
        }

        /**
         * Set the title text shown on the ShowcaseView.
         */
        public Builder setContentTitle(CharSequence title) {
            showcaseView.setContentTitle(title);
            return this;
        }

        /**
         * Set the descriptive text shown on the ShowcaseView.
         */
        public Builder setContentText(int resId) {
            return setContentText(activity.getString(resId));
        }

        /**
         * Set the descriptive text shown on the ShowcaseView.
         */
        public Builder setContentText(CharSequence text) {
            showcaseView.setContentText(text);
            return this;
        }

        /**
         * Set the target of the showcase.
         *
         * @param target a {@link com.github.amlcurran.showcaseview.targets.Target} representing
         *               the item to showcase (e.g., a button, or action item).
         */
        public Builder setTarget(Target target) {
            showcaseView.setTarget(target);
            return this;
        }

        /**
         * Set the style of the ShowcaseView. See the sample app for example styles.
         */
        public Builder setStyle(int theme) {
            showcaseView.setStyle(theme);
            return this;
        }

        public Builder setStyle(int theme, Typeface titleTypeface, Typeface detailTypeface) {
            showcaseView.setStyle(theme, titleTypeface, detailTypeface);
            return this;
        }

        /**
         * Set a listener which will override the button clicks.
         * <p/>
         * Note that you will have to manually hide the ShowcaseView
         */
        public Builder setOnClickListener(OnClickListener onClickListener) {
            showcaseView.overrideButtonClick(onClickListener);
            return this;
        }

        /**
         * Don't make the ShowcaseView block touches on itself. This doesn't
         * block touches in the showcased area.
         * <p/>
         * By default, the ShowcaseView does block touches
         */
        public Builder doNotBlockTouches() {
            showcaseView.setBlocksTouches(false);
            return this;
        }

        /**
         * Make this ShowcaseView hide when the user touches outside the showcased area.
         * This enables {@link #doNotBlockTouches()} as well.
         * <p/>
         * By default, the ShowcaseView doesn't hide on touch.
         */
        public Builder hideOnTouchOutside() {
            showcaseView.setBlocksTouches(true);
            showcaseView.setHideOnTouchOutside(true);
            return this;
        }

        /**
         * Set the ShowcaseView to only ever show once.
         *
         * @param shotId a unique identifier (<em>across the app</em>) to store
         *               whether this ShowcaseView has been shown.
         */
        public Builder singleShot(long shotId) {
            showcaseView.setSingleShot(shotId);
            return this;
        }

        public Builder setShowcaseEventListener(OnShowcaseEventListener showcaseEventListener) {
            showcaseView.setOnShowcaseEventListener(showcaseEventListener);
            return this;
        }

        public Builder setParent(ViewGroup parent, int index) {
            this.parent = parent;
            this.parentIndex = index;
            return this;
        }

        /**
         * Sets the paint that will draw the text as specified by {@link #setContentText(CharSequence)}
         * or {@link #setContentText(int)}. If you're using a TextAppearance (set by {@link #setStyle(int)},
         * then this {@link TextPaint} will override that TextAppearance.
         */
        public Builder setContentTextPaint(TextPaint textPaint) {
            showcaseView.setContentTextPaint(textPaint);
            return this;
        }

        public Builder withCenteredText() {
            showcaseView.setShouldCentreText(true);
            showcaseView.setTitleTextAlignment(Layout.Alignment.ALIGN_CENTER);
            showcaseView.setDetailTextAlignment(Layout.Alignment.ALIGN_CENTER);
            return this;
        }

        /**
         * Sets the paint that will draw the text as specified by {@link #setContentTitle(CharSequence)}
         * or {@link #setContentTitle(int)}. If you're using a TextAppearance (set by {@link #setStyle(int)},
         * then this {@link TextPaint} will override that TextAppearance.
         */
        public Builder setContentTitlePaint(TextPaint textPaint) {
            showcaseView.setContentTitlePaint(textPaint);
            return this;
        }

        /**
         * Block any touch made on the ShowcaseView, even inside the showcase
         */
        public Builder blockAllTouches() {
            showcaseView.setBlockAllTouches(true);
            return this;
        }

        /**
         * Uses the android decor view to insert a showcase, this is not recommended
         * as then UI elements in showcase view can hide behind the nav bar
         */
        public Builder useDecorViewAsParent() {
            this.parent = ((ViewGroup) activity.getWindow().getDecorView());
            this.parentIndex = -1;
            return this;
        }
    }

    private void setShowcaseDrawer(ShowcaseDrawer showcaseDrawer) {
        this.showcaseDrawer = showcaseDrawer;
        this.showcaseDrawer.setBackgroundColour(backgroundColor);
        this.showcaseDrawer.setShowcaseColour(showcaseColor);
        hasAlteredText = true;
        invalidate();
    }

    private void setContentTitlePaint(TextPaint textPaint) {
        this.textDrawer.setTitlePaint(textPaint);
        hasAlteredText = true;
        invalidate();
    }

    private void setContentTextPaint(TextPaint paint) {
        this.textDrawer.setContentPaint(paint);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * Set whether the text should be centred in the screen, or left-aligned (which is the default).
     */
    public void setShouldCentreText(boolean shouldCentreText) {
        this.shouldCentreText = shouldCentreText;
        hasAlteredText = true;
        invalidate();
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setSingleShot(long)
     */
    private void setSingleShot(long shotId) {
        shotStateStore.setSingleShot(shotId);
    }

    /**
     * Change the position of the ShowcaseView's button from the default bottom-right position.
     *
     * @param layoutParams a {@link android.widget.RelativeLayout.LayoutParams} representing
     *                     the new position of the button
     */
    @Override
    public void setButtonPosition(RelativeLayout.LayoutParams layoutParams) {
        mEndButton.setLayoutParams(layoutParams);
    }

    /**
     * Sets the text alignment of the detail text
     */
    public void setDetailTextAlignment(Layout.Alignment textAlignment) {
        textDrawer.setDetailTextAlignment(textAlignment);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * Sets the text alignment of the title text
     */
    public void setTitleTextAlignment(Layout.Alignment textAlignment) {
        textDrawer.setTitleTextAlignment(textAlignment);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * Set the duration of the fading in and fading out of the ShowcaseView
     */
    public void setFadeDurations(long fadeInMillis, long fadeOutMillis) {
        this.fadeInMillis = fadeInMillis;
        this.fadeOutMillis = fadeOutMillis;
    }

    public void forceTextPosition(@TextPosition int textPosition) {
        textDrawer.forceTextPosition(textPosition);
        hasAlteredText = true;
        invalidate();
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#hideOnTouchOutside()
     */
    @Override
    public void setHideOnTouchOutside(boolean hideOnTouch) {
        this.hideOnTouch = hideOnTouch;
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#doNotBlockTouches()
     */
    @Override
    public void setBlocksTouches(boolean blockTouches) {
        this.blockTouches = blockTouches;
    }

    private void setBlockAllTouches(boolean blockAllTouches) {
        this.blockAllTouches = blockAllTouches;
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setStyle(int)
     */
    @Override
    public void setStyle(int theme) {
        TypedArray array = getContext().obtainStyledAttributes(theme, R.styleable.ShowcaseView);
        updateStyle(array, true, null, null);
    }

    /**
     * @see com.github.amlcurran.showcaseview.ShowcaseView.Builder#setStyle(int)
     */
    @Override
    public void setStyle(int theme, Typeface titleTypeface, Typeface detailTypeface) {
        TypedArray array = getContext().obtainStyledAttributes(theme, R.styleable.ShowcaseView);
        updateStyle(array, true, titleTypeface, detailTypeface);
    }

    @Override
    public boolean isShowing() {
        return isShowing;
    }

    private void updateStyle(TypedArray styled, boolean invalidate, Typeface titleTypeface, Typeface detailTypeface) {
        backgroundColor = styled.getColor(R.styleable.ShowcaseView_sv_backgroundColor, Color.argb(128, 80, 80, 80));
        showcaseColor = styled.getColor(R.styleable.ShowcaseView_sv_showcaseColor, HOLO_BLUE);

        int titleTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_titleTextAppearance,
                R.style.TextAppearance_ShowcaseView_Title);
        int detailTextAppearance = styled.getResourceId(R.styleable.ShowcaseView_sv_detailTextAppearance,
                R.style.TextAppearance_ShowcaseView_Detail);

        styled.recycle();

        showcaseDrawer.setShowcaseColour(showcaseColor);
        showcaseDrawer.setBackgroundColour(backgroundColor);
        if (titleTypeface == null) textDrawer.setTitleStyling(titleTextAppearance);
        else textDrawer.setTitleStyling(titleTextAppearance, titleTypeface);
        if (detailTypeface == null) textDrawer.setDetailStyling(detailTextAppearance);
        else textDrawer.setDetailStyling(detailTextAppearance, detailTypeface);
        hasAlteredText = true;

        if (invalidate) {
            invalidate();
        }
    }

    private OnClickListener hideOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            hide();
        }
    };

}

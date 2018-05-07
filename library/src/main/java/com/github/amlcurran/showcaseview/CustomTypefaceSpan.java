package com.github.amlcurran.showcaseview;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

/**
 * Created by Lee Howett on 2018-05-07.
 */
public class CustomTypefaceSpan extends TextAppearanceSpan {

    private final Typeface typeface;

    public CustomTypefaceSpan(Context context, int appearance, Typeface typeface) {
        super(context, appearance);

        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setTypeface(typeface);
    }

    @Override
    public void updateMeasureState(TextPaint ds) {
        super.updateMeasureState(ds);
        ds.setTypeface(typeface);
    }
}

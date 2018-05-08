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

package com.github.amlcurran.showcaseview

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat

internal open class StandardShowcaseDrawer(resources: Resources, theme: Resources.Theme): ShowcaseDrawer {

    protected val eraserPaint: Paint
    protected val showcaseDrawable: Drawable?
    private val basicPaint: Paint
    private val showcaseRadius: Float
    override var backgroundColor: Int = 0

    override val showcaseWidth: Int
        get() = showcaseDrawable?.intrinsicWidth ?: 0

    override val showcaseHeight: Int
        get() = showcaseDrawable?.intrinsicHeight ?: 0

    init {
        val xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        eraserPaint = Paint()
        eraserPaint.color = 0xFFFFFF
        eraserPaint.alpha = 0
        eraserPaint.xfermode = xfermode
        eraserPaint.isAntiAlias = true
        basicPaint = Paint()
        showcaseRadius = resources.getDimension(R.dimen.showcase_radius)
        showcaseDrawable = ResourcesCompat.getDrawable(resources, R.drawable.cling_bleached, theme)
    }

    override fun setShowcaseColour(color: Int) {
        showcaseDrawable?.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }

    override fun drawShowcase(buffer: Bitmap, x: Float, y: Float, scaleMultiplier: Float) {
        val bufferCanvas = Canvas(buffer)
        bufferCanvas.drawCircle(x, y, showcaseRadius, eraserPaint)
        val halfW = showcaseWidth / 2
        val halfH = showcaseHeight / 2
        val left = (x - halfW).toInt()
        val top = (y - halfH).toInt()
        showcaseDrawable?.setBounds(left, top,
                left + showcaseWidth,
                top + showcaseHeight)
        showcaseDrawable?.draw(bufferCanvas)
    }

    override fun isWithinBlockedArea(centerX: Float, centerY: Float, rawX: Float, rawY: Float): Boolean {
        val xDelta = Math.abs(rawX - centerX).toDouble()
        val yDelta = Math.abs(rawY - centerY).toDouble()
        val distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2.0) + Math.pow(yDelta, 2.0))
        return distanceFromFocus > showcaseRadius
    }

    override fun erase(bitmapBuffer: Bitmap) {
        bitmapBuffer.eraseColor(backgroundColor)
    }

    override fun drawToCanvas(canvas: Canvas, bitmapBuffer: Bitmap) {
        canvas.drawBitmap(bitmapBuffer, 0f, 0f, basicPaint)
    }

}

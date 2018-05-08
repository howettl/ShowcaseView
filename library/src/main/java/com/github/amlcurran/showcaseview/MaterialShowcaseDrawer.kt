package com.github.amlcurran.showcaseview

import android.content.res.Resources
import android.graphics.*

class MaterialShowcaseDrawer(resources: Resources): ShowcaseDrawer {

    private val radius: Float
    private val basicPaint: Paint
    private val eraserPaint: Paint
    override var backgroundColor: Int = 0

    override val showcaseWidth: Int
        get() = (radius * 2).toInt()

    override val showcaseHeight: Int
        get() = (radius * 2).toInt()

    init {
        this.radius = resources.getDimension(R.dimen.showcase_radius_material)
        this.eraserPaint = Paint()
        this.eraserPaint.color = 0xFFFFFF
        this.eraserPaint.alpha = 0
        this.eraserPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        this.eraserPaint.isAntiAlias = true
        this.basicPaint = Paint()
    }

    override fun setShowcaseColour(color: Int) {
        // no-op
    }

    override fun drawShowcase(buffer: Bitmap, x: Float, y: Float, scaleMultiplier: Float) {
        val bufferCanvas = Canvas(buffer)
        bufferCanvas.drawCircle(x, y, radius, eraserPaint)
    }

    override fun isWithinBlockedArea(centerX: Float, centerY: Float, rawX: Float, rawY: Float): Boolean {
        val xDelta = Math.abs(rawX - centerX).toDouble()
        val yDelta = Math.abs(rawY - centerY).toDouble()
        val distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2.0) + Math.pow(yDelta, 2.0))
        return distanceFromFocus > radius
    }

    override fun erase(bitmapBuffer: Bitmap) {
        bitmapBuffer.eraseColor(backgroundColor)
    }

    override fun drawToCanvas(canvas: Canvas, bitmapBuffer: Bitmap) {
        canvas.drawBitmap(bitmapBuffer, 0f, 0f, basicPaint)
    }
}

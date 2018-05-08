package com.github.amlcurran.showcaseview

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF

/**
 *
 * Created by Lee Howett on 2018-05-08.
 *
 */
internal class OvalShowcaseDrawer(resources: Resources, theme: Resources.Theme): StandardShowcaseDrawer(resources, theme) {

    var innerHeight: Int = 0
    var innerWidth: Int = 0
    var outerHeight: Int = 0
    var outerWidth: Int = 0

    override val showcaseWidth = outerWidth
    override val showcaseHeight = outerHeight

    override fun setShowcaseColour(color: Int) {
        eraserPaint.color = color
    }

    override fun drawShowcase(buffer: Bitmap, x: Float, y: Float, scaleMultiplier: Float) {
        val bufferCanvas = Canvas(buffer)
        eraserPaint.alpha = ALPHA_45_PERCENT
        bufferCanvas.drawOval(getOuterRect(x, y), eraserPaint)
        eraserPaint.alpha = 0
        bufferCanvas.drawOval(getInnerRect(x, y), eraserPaint)
    }

    override fun isWithinBlockedArea(centerX: Float, centerY: Float, rawX: Float, rawY: Float): Boolean {
        val x = rawX.toDouble()
        val y = rawY.toDouble()
        val h = centerX.toDouble()
        val k = centerY.toDouble()
        val rx = (innerWidth / 2).toDouble()
        val ry = (innerHeight / 2).toDouble()
        val result = (Math.pow((x - h), 2.0) / Math.pow(rx, 2.0)) + ((Math.pow((y - k), 2.0)) / (Math.pow(ry, 2.0)))
        return result > 1
    }

    private fun getOuterRect(centerX: Float, centerY: Float) =
            RectF(centerX - outerWidth / 2, centerY - outerHeight / 2,
                    centerX + outerWidth / 2, centerY + outerHeight / 2)
    private fun getInnerRect(centerX: Float, centerY: Float) =
            RectF(centerX - innerWidth / 2, centerY - innerHeight / 2,
                    centerX + innerWidth / 2, centerY + innerHeight / 2)

    companion object {
        private const val ALPHA_45_PERCENT = 140
    }
}
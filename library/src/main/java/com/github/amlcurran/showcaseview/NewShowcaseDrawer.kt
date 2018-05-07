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
import android.graphics.Bitmap
import android.graphics.Canvas

internal class NewShowcaseDrawer @JvmOverloads constructor(private val resources: Resources, theme: Resources.Theme, private val parentView: ShowcaseView? = null) : StandardShowcaseDrawer(resources, theme) {
    var outerRadius: Float = 0.toFloat()
        set(value) {
            field = value
            parentView?.invalidate()
        }
    var innerRadius: Float = 0.toFloat()
        set(value) {
            field = value
            parentView?.invalidate()
        }

    init {
        outerRadius = resources.getDimension(R.dimen.showcase_radius_inner)
        innerRadius = resources.getDimension(R.dimen.showcase_radius_outer)
    }

    override fun setShowcaseColour(color: Int) {
        eraserPaint.color = color
    }

    fun setOuterRadius(outerRadiusDimen: Int) {
        outerRadius = resources.getDimension(outerRadiusDimen)
    }

    fun setInnerRadius(innerRadiusDimen: Int) {
        innerRadius = resources.getDimension(innerRadiusDimen)
    }

    override fun drawShowcase(buffer: Bitmap, x: Float, y: Float, scaleMultiplier: Float) {
        val bufferCanvas = Canvas(buffer)
        eraserPaint.alpha = ALPHA_45_PERCENT
        bufferCanvas.drawCircle(x, y, outerRadius, eraserPaint)
        eraserPaint.alpha = 0
        bufferCanvas.drawCircle(x, y, innerRadius, eraserPaint)
    }

    override fun getShowcaseWidth() = (outerRadius * 2).toInt()

    override fun getShowcaseHeight() = (outerRadius * 2).toInt()

    override fun getBlockedRadius() = innerRadius

    companion object {
        private const val ALPHA_45_PERCENT = 140
    }

}

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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.annotation.ColorInt

/**
 * Class to implement your own drawing of a showcase view, should you want more
 * control. See the other implementations for examples
 */
interface ShowcaseDrawer {

    /**
     * @return the width of the showcase, used to calculate where to place text
     */
    val showcaseWidth: Int

    /**
     * @return the height of the showcase, used to calculate where to place text
     */
    val showcaseHeight: Int

    /**
     * Set the background color of the showcase. What this means is up to your implementation,
     * but typically this should be the color used to draw in [.erase]
     */
    var backgroundColor: Int

    /**
     * Sets the value of the showcase color from themes. What this does is dependent on
     * your implementation of [.drawShowcase]
     * @param color the color supplied in the theme
     */
    fun setShowcaseColour(@ColorInt color: Int)

    /**
     * Draw the showcase. How this is performed is up to you!
     * @param buffer the bitmap to draw onto
     * @param x the x position of the point to showcase
     * @param y the y position of the point to showcase
     * @param scaleMultiplier a scale factor. Currently unused
     */
    fun drawShowcase(buffer: Bitmap, x: Float, y: Float, scaleMultiplier: Float)

    /**
     * Return true if the raw coordinates are within the blocked area.
     * @param centerX Center of the showcase
     * @param centerY Center of the showcase
     * @param rawX X position of touch input
     * @param rawY Y position of touch input
     * @return True if the raw coordinates are within the blocked area
     */
    fun isWithinBlockedArea(centerX: Float, centerY: Float, rawX: Float, rawY: Float): Boolean

    /**
     * Remove all drawing on the bitmap. Typically, this would do a color fill of the background
     * color. See [StandardShowcaseDrawer] for an example
     * @param bitmapBuffer the Bitmap to erase drawing from
     */
    fun erase(bitmapBuffer: Bitmap)

    /**
     * Draw the commands drawn to the canvas. Typically this is a single implementation, see
     * [StandardShowcaseDrawer].
     * @param canvas canvas to draw to
     * @param bitmapBuffer bitmap to draw
     */
    fun drawToCanvas(canvas: Canvas, bitmapBuffer: Bitmap)
}

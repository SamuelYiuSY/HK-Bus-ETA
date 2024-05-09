/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkbuseta.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy
import kotlin.math.roundToInt


@Composable
inline fun UIImage.asPainter(isIcon: Boolean = false, filterQuality: FilterQuality = FilterQuality.Low) : Painter {
    return remember(this, filterQuality) { UIImagePainter(this, isIcon, filterQuality) }
}

class UIImagePainter(
    private val uiImage: UIImage,
    val isIcon: Boolean,
    private val filterQuality: FilterQuality = FilterQuality.Low
): Painter() {

    private val size: IntSize by lazy { uiImage.toImageBitmap().let { IntSize(it.width, it.height) } }
    private var alpha: Float = 1F
    private var colorFilter: ColorFilter? = null

    override fun DrawScope.onDraw() {
        val (nativeWidth, nativeHeight) = size / drawContext.density.density
        drawImage(
            image = uiImage.resize(nativeWidth.toDouble(), nativeHeight.toDouble()).toImageBitmap(),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = filterQuality
        )
    }

    override val intrinsicSize: Size get() = size.toSize()

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }
}

@OptIn(ExperimentalForeignApi::class)
inline fun UIImage.resize(width: Double): UIImage {
    val (sizeWidth, sizeHeight) = size.useContents { this.width to this.height }
    val scale = width / sizeWidth
    val height = sizeHeight * scale
    return resize(width, height)
}

@OptIn(ExperimentalForeignApi::class)
inline fun UIImage.resize(width: Double, height: Double): UIImage {
    try {
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, 0.0)
        drawInRect(CGRectMake(0.0, 0.0, width, height))
        return UIGraphicsGetImageFromCurrentImageContext()!!
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
inline fun UIImage.toImageBitmap(): ImageBitmap {
    val bytes = UIImagePNGRepresentation(this)!!
    val byteArray = ByteArray(bytes.length.toInt())
    byteArray.usePinned { memcpy(it.addressOf(0), bytes.bytes, bytes.length) }
    return Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}

package com.messenger.prime.shapes

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val androidPath = polygon.toPath()
        val bounds = android.graphics.RectF()
        androidPath.computeBounds(bounds, true)

        val scaleX = size.width / bounds.width()
        val scaleY = size.height / bounds.height()

        val matrix = Matrix()
        matrix.postTranslate(-bounds.left, -bounds.top)
        matrix.postScale(scaleX, scaleY)

        androidPath.transform(matrix)

        return Outline.Generic(androidPath.asComposePath())
    }
}

class PathShape(private val pathProvider: (width: Float, height: Float) -> android.graphics.Path) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val androidPath = pathProvider(size.width, size.height)
        return Outline.Generic(androidPath.asComposePath())
    }
}

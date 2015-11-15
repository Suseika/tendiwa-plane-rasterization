package org.tendiwa.rasterization.shapes

import org.tendiwa.geometry.continuum.rectangles.maxX
import org.tendiwa.geometry.continuum.rectangles.maxY
import org.tendiwa.geometry.continuum.shapes.Shape
import org.tendiwa.grid.rectangles.GridRectangle
import org.tendiwa.math.doubles.closestInt

val Shape.gridHull: GridRectangle
    get() {
        val hull = this.hull
        val minX = hull.x.closestInt
        val minY = hull.y.closestInt
        val maxX = hull.maxX.closestInt
        val maxY = hull.maxY.closestInt
        println("$hull ${hull.maxX} ${hull.maxY}")
        return GridRectangle(
            minX,
            minY,
            maxX - minX + 1,
            maxY - minY + 1
        )
    }


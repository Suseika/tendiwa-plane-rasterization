package org.tendiwa.plane.rasterization.shapes

import org.tendiwa.math.doubles.closestInt
import org.tendiwa.plane.geometry.shapes.SegmentGroup
import org.tendiwa.plane.grid.rectangles.GridRectangle

val SegmentGroup.gridHull: GridRectangle
    get() {
        val hull = this.hull
        val minX = hull.x.closestInt
        val minY = hull.y.closestInt
        val maxX = hull.maxX.closestInt
        val maxY = hull.maxY.closestInt
        return GridRectangle(
            minX,
            minY,
            maxX - minX + 1,
            maxY - minY + 1
        )
    }


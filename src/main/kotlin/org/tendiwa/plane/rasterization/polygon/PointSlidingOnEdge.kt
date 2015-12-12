package org.tendiwa.plane.rasterization.polygon

import org.tendiwa.math.doubles.isCloseToZero
import org.tendiwa.math.matrices.determinant
import org.tendiwa.plane.geometry.points.Point

internal class PointSlidingOnEdge {
    var x: Double = 0.toDouble()
    var y: Double = 0.toDouble()

    /**
     * Sets [.x] and [.y] to the coordinates of intersection of a line from `start` to `end` with a horizontal line on `y`-coordinate.

     * @param start
     * * 	Start of a segment.
     * *
     * @param end
     * * 	End of a segment.
     * *
     * @param y
     * * 	Y-coordinate of a horizontal line.
     */
    fun setToIntersection(start: Point, end: Point, y: Int) {
        val a = start.y - end.y
        val b = end.x - start.x
        val c = start.x * end.y - end.x * start.y
        val horizontalA = 0.0
        val horizontalB = 100.0
        val horizontalC = (-100 * y).toDouble()
        val zn = determinant(a, b, horizontalA, horizontalB).toDouble()
        if (zn.isCloseToZero) {
            throw RuntimeException()
        }
        this.x = -determinant(c, b, horizontalC, horizontalB) / zn
        this.y = -determinant(a, c, horizontalA, horizontalC) / zn
    }
}

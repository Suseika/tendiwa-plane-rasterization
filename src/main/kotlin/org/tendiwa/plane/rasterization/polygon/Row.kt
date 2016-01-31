package org.tendiwa.plane.rasterization.polygon

import org.tendiwa.collections.loopedNextIndex
import org.tendiwa.collections.nextAfter
import org.tendiwa.collections.prevBefore
import org.tendiwa.math.matrices.determinant
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.points.segmentTo
import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.geometry.segments.intersectsHorizontalLine
import org.tendiwa.plane.grid.masks.mutable.MutableArrayGridMask
import org.tendiwa.plane.grid.segments.ortho.HorizontalGridSegment
import org.tendiwa.plane.grid.tiles.Tile
import java.util.*

internal class Row(
    val polygon: Polygon,
    y: Int,
    val mask: MutableArrayGridMask
) {
    private val yInt = y

    private val y = y.toDouble()

    val numberOfVertices = polygon.points.size

    private val intersections: Array<Any?> =
        intersectionsWithPolygon()
            .apply { sortByX(this) }

    internal fun sortByX(array: Array<Any?>) {
        Arrays.sort<Any>(array) {
            a: Any, b: Any ->
            val valueA: Double
            val valueB: Double
            if (a is Point) {
                valueA = a.x
            } else {
                assert(a is Pair<*, *>)
                valueA = polygon.points[(a as Pair<Int, Int>).first].x
            }
            if (b is Point) {
                valueB = b.x
            } else {
                assert(a is Pair<*, *>)
                valueB = polygon.points[(b as Pair<Int, Int>).first].x
            }
            java.lang.Double.compare(valueA, valueB)
        }
        assert(
            array.size % 2 == 0
                || array.size == 1
                && array[0] is Pair<*, *>
        ) { "${array.size}, y=$y" }
    }

    private fun intersectionsWithPolygon(): Array<Any?> {
        var numberOfIntersections = 0
        /*
         * Here are saved pairs of indices of vertices for which
         * (1) vertex.y == y and (2) for that pair of indices a vertex
         * before the 0th index and a vertex after the 1st index are from
         * different sides from the horizontal line y.
         */
        var consecutiveSegments: MutableList<Pair<Int, Int>>? = null
        var vertex: Point
        var i = 0
        while (i < numberOfVertices) {
            vertex = polygon.points[i]
            val nextVertex = polygon.points.nextAfter(i)
            if (nextVertex.segmentTo(vertex).intersectsHorizontalLine(y)) {
                numberOfIntersections++
            } else if (vertex.y == y) {
                /*
                 * If we encounter a point right on the horizontal line y,
                 * then there are 0+ points after it on line y (there will
                 * usually be 0 points after it).
                 */
                val firstIndex = i
                while (
                i < numberOfVertices
                    && polygon.points.nextAfter(i).y == y
                ) {
                    // Modification of the counter!
                    i++
                }
                val secondIndex = loopedNextIndex(i - 1, numberOfVertices)
                var segment = Pair(firstIndex, secondIndex)
                val point1 = polygon.points[segment.first]
                val point2 = polygon.points[segment.second]
                assert(point1.y == point2.y)
                var westToEast = true
                if (point1.x > point2.x) {
                    // Index of a point with lesser x-coordinate must be first.
                    westToEast = false
                    segment = Pair(segment.second, segment.first)
                }
                val westFromFirstPoint: Point
                if (westToEast) {
                    westFromFirstPoint = polygon.points.prevBefore(segment.first)
                } else {
                    westFromFirstPoint = polygon.points.nextAfter(segment.first)
                }
                val eastFromLastPoint: Point
                if (westToEast) {
                    eastFromLastPoint = polygon.points.nextAfter(segment.second)
                } else {
                    eastFromLastPoint = polygon.points.prevBefore(segment.second)
                }
                if (
                Math.signum(westFromFirstPoint.y - y)
                    == -Math.signum(eastFromLastPoint.y - y)
                ) {
                    if (consecutiveSegments == null) {
                        consecutiveSegments = LinkedList()
                    }
                    consecutiveSegments.add(segment)
                    numberOfIntersections++
                }
            }
            i++
        }
        assert(numberOfIntersections % 2 == 0 || numberOfIntersections == 1 && consecutiveSegments!!.size == 1)

        val intersections = arrayOfNulls<Any>(numberOfIntersections)
        var j = 0
        for (segment in polygon.segments) {
            if (segment.intersectsHorizontalLine(y)) {
                intersections[j++] =
                    segment.intersectionWithHorizontalLine(y)
            }
        }
        if (consecutiveSegments != null) {
            for (segment in consecutiveSegments) {
                intersections[j++] = segment
            }
        }
        assert(j == intersections.size)
        return intersections
    }

    fun fillConsecutiveSegments() {
        if (intersections.size == 1) {
            assert(intersections[0] is Pair<*, *>)
            val array = intersections[0] as Pair<Int, Int>
            val ax = polygon.points[array.first].x
            val bx = polygon.points[array.second].x
            assert(bx > ax)
            mask.fillHorizontalSegment(horizontalSegment(ax, bx, yInt))
        } else {
            var i = 0
            while (i < intersections.size) {
                val a = intersections[i]
                val b = intersections[i + 1]
                val ax: Double
                val bx: Double
                if (a is Point) {
                    ax = a.x
                } else {
                    assert(a is Pair<*, *>)
                    val aint = a as Pair<Int, Int>
                    ax = polygon.points[aint.first].x
                }
                if (b is Point) {
                    bx = b.x
                } else {
                    assert(b is Pair<*, *>)
                    val bint = b as Pair<Int, Int>
                    bx = polygon.points[bint.second].x
                }
                assert(bx > ax)
                mask.fillHorizontalSegment(
                    horizontalSegment(ax, bx, yInt)
                )
                i += 2
            }
        }
    }

    private fun horizontalSegment(
        ax: Double,
        bx: Double,
        y: Int
    ): HorizontalGridSegment {
        val startX = Math.round(ax).toInt()
        val endX = Math.round(bx).toInt()
        return HorizontalGridSegment(Tile(startX, y), endX - startX + 1)
    }

    /**
     * Finds intersection of a line from `start` to `end` with a horizontal line
     * on `y`-coordinate.
     * @param start Start of a segment.
     * @param end End of a segment.
     * @param y Y-coordinate of a horizontal line.
     */
    fun Segment.intersectionWithHorizontalLine(y: Double): Point {
        val a = start.y - end.y
        val b = end.x - start.x
        val c = start.x * end.y - end.x * start.y
        val horizontalA = 0.0
        val horizontalB = 100.0
        val horizontalC = (-100.0 * y)
        val zn = determinant(a, b, horizontalA, horizontalB)
//        if (zn.isCloseToZero) {
//             Horizontal line is parallel to the segment
//            throw RuntimeException()
//        }
        return Point(
            -determinant(c, b, horizontalC, horizontalB) / zn,
            -determinant(a, c, horizontalA, horizontalC) / zn
        )
    }
}

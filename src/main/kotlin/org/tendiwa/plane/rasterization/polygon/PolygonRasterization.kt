package org.tendiwa.plane.rasterization.polygon

import org.tendiwa.collections.loopedNextIndex
import org.tendiwa.collections.nextAfter
import org.tendiwa.collections.prevBefore
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.points.segmentTo
import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.geometry.segments.intersectsHorizontalLine
import org.tendiwa.plane.grid.masks.mutable.MutableArrayGridMask
import org.tendiwa.plane.grid.rectangles.GridRectangle
import org.tendiwa.plane.grid.rectangles.maxY
import org.tendiwa.plane.grid.segments.ortho.HorizontalGridSegment
import org.tendiwa.plane.grid.tiles.Tile
import org.tendiwa.plane.rasterization.segmentGroups.gridHull
import org.tendiwa.plane.rasterization.segments.GridSegment
import java.util.*

val Polygon.rasterized: MutableArrayGridMask
    get() = PolygonRasterization(this).result

/**
 * The algorithm is described here:
 * [Алгоритмы со списком рёберных точек](http://habrahabr.ru/post/116398/).
 *
 */
private class PolygonRasterization(poly: Polygon) {
    /*
     * The base algorithm is pretty simple. However, this class' complexity
     * comes from handling the case where there are several edges of a polygon
     * on the same horizontal line with a whole number y-coordinate.
     */
    private val polygon: Polygon = poly.collapseHorizontalChains()

    internal val result: MutableArrayGridMask

    val numberOfVertices = polygon.points.size

    val cornerToSlidingPoint = polygon.points
        .map { Pair(it, PointSlidingOnEdge()) }
        .toMap()

    init {
        result = computeGridMask(poly.gridHull)
    }

    private fun computeGridMask(bounds: GridRectangle): MutableArrayGridMask =
        MutableArrayGridMask(bounds)
            .apply {
                if (bounds.width == 1 || bounds.height == 1) {
                    fill()
                } else {
                    (bounds.y..bounds.maxY)
                        .map { Row(it, this) }
                        .forEach { it.fillConsecutiveSegments() }
                    drawIntegerHorizontalEdges()
                }
            }

    private fun MutableArrayGridMask.drawIntegerHorizontalEdges() {
        polygon
            .segments
            .filter { edgeNeedsExplicitRasterization(it) }
            .map { GridSegment(it) }
            .map { it.tiles }
            .forEach { fillWithTiles(it) }
    }

    private fun edgeNeedsExplicitRasterization(segment: Segment): Boolean {
        val startY = segment.start.y
        if (startY != Math.floor(startY)) {
            return false
        }
        val endY = segment.end.y
        return startY == endY
    }

    private fun MutableArrayGridMask.fillWithTiles(cells: Iterable<Tile>) {
        cells.forEach { tile ->
            if (!contains(tile.x, tile.y)) {
                add(tile.x, tile.y)
            }
        }
    }

    internal inner class Row(val y: Int, val mask: MutableArrayGridMask) {
        private val yDouble = y.toDouble()

        val inters: Array<Any?> =
            intersectionsWithPolygon()
        private val intersections: Array<Any?> =
            inters
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
                if (nextVertex.segmentTo(vertex).intersectsHorizontalLine(y.toDouble())) {
                    numberOfIntersections++
                } else if (vertex.y == yDouble) {
                    /*
                     * If we encounter a point right on the horizontal line y,
                     * then there are 0+ points after it on line y (there will
                     * usually be 0 points after it).
                     */
                    val firstIndex = i
                    while (
                    i < numberOfVertices
                        && polygon.points.nextAfter(i).y == y.toDouble()
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
                if (segment.intersectsHorizontalLine(y.toDouble())) {
                    val pointSlidingOnEdge =
                        cornerToSlidingPoint[segment.start]!!
                            .atIntersection(segment.start, segment.end, y)
                    intersections[j++] = pointSlidingOnEdge
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
                mask.fillHorizontalSegment(horizontalSegment(ax, bx, y))
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
                        horizontalSegment(ax, bx, y)
                    )
                    i += 2
                }
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
}

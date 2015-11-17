package org.tendiwa.rasterization.polygon

import org.tendiwa.collectioins.loopedNextIndex
import org.tendiwa.collectioins.nextAfter
import org.tendiwa.collectioins.prevBefore
import org.tendiwa.geometry.points.Point
import org.tendiwa.geometry.polygons.Polygon
import org.tendiwa.geometry.segments.Segment
import org.tendiwa.grid.masks.mutable.MutableArrayGridMask
import org.tendiwa.grid.rectangles.maxY
import org.tendiwa.grid.segments.ortho.HorizontalGridSegment
import org.tendiwa.grid.tiles.Tile
import org.tendiwa.rasterization.segments.GridSegment
import org.tendiwa.rasterization.shapes.gridHull
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

    val map = polygon.points.map { Pair(it, PointSlidingOnEdge()) }.toMap()

    init {
        val bounds = poly.gridHull
        this.result = MutableArrayGridMask(bounds)
        (bounds.y..bounds.maxY).forEach {
            Row(it).fillConsecutiveSegments()
        }
        drawIntegerHorizontalEdges()
    }

    private fun drawIntegerHorizontalEdges() {
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

    private fun fillWithTiles(cells: Iterable<Tile>) {
        cells.forEach { tile ->
            if (!this.result.contains(tile.x, tile.y)) {
                this.result.add(tile.x, tile.y)
            }
        }
    }

    internal inner class Row(val y: Int) {
        private val yDouble = y.toDouble()

        private val intersections: Array<Any?> =
            intersectionsWithPolygon()
                .apply {
                    sortByX()
                }

        private fun Array<Any?>.sortByX() {
            Arrays.sort<Any>(this) {
                a: Any, b: Any ->
                val valueA: Double
                val valueB: Double
                if (a is PointSlidingOnEdge) {
                    valueA = a.x
                } else {
                    assert(a is Pair<*, *>)
                    valueA = polygon.points[(a as Pair<Int, Int>).first].x
                }
                if (b is PointSlidingOnEdge) {
                    valueB = b.x
                } else {
                    assert(a is Pair<*, *>)
                    valueB = polygon.points[(b as Pair<Int, Int>).first].x
                }
                java.lang.Double.compare(valueA, valueB)
            }
            assert(
                this.size % 2 == 0
                    || this.size == 1
                    && this[0] is Pair<*, *>
            ) { "$size, y=$y" }
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
                if (horizontalLineIntersectsSegment(nextVertex, vertex, y)) {
                    numberOfIntersections++
                } else if (vertex.y == yDouble) {
                    /*
                     * If we encounter a point right on the horizontal line y,
                     * then there are 0+ points after it on line y (there will
                     * usually be 0 points after it).
                     */
                    val firstIndex = i;
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
            vertex = polygon.points[0]
            for (k in 0..numberOfVertices - 1) {
                val nextVertex = polygon.points[if (k + 1 == numberOfVertices) 0 else k + 1]
                if (horizontalLineIntersectsSegment(nextVertex, vertex, y)) {
                    val pointSlidingOnEdge = map[vertex]
                    pointSlidingOnEdge!!
                        .setToIntersection(vertex, nextVertex, y)
                    intersections[j++] = pointSlidingOnEdge
                }
                vertex = nextVertex
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
                result.fillHorizontalSegment(
                    horizontalSegment(ax, bx, y))
            } else {
                var i = 0
                while (i < intersections.size) {
                    val a = intersections[i]
                    val b = intersections[i + 1]
                    val ax: Double
                    val bx: Double
                    if (a is PointSlidingOnEdge) {
                        ax = a.x
                    } else {
                        assert(a is Pair<*, *>)
                        val aint = a as Pair<Int, Int>
                        ax = polygon.points[aint.first].x
                    }
                    if (b is PointSlidingOnEdge) {
                        bx = b.x
                    } else {
                        assert(b is Pair<*, *>)
                        val bint = b as Pair<Int, Int>
                        bx = polygon.points[bint.second].x
                    }
                    assert(bx > ax)
                    result.fillHorizontalSegment(
                        horizontalSegment(ax, bx, y)
                    )
                    i += 2
                }
            }
        }
    }

    private fun horizontalLineIntersectsSegment(
        start: Point,
        end: Point,
        y: Int
    ) = end.y < y && start.y > y || end.y > y && start.y < y

    private fun horizontalSegment(
        ax: Double,
        bx: Double,
        y: Int
    ): HorizontalGridSegment {
        val startX = Math.floor(ax).toInt()
        val endX = Math.ceil(bx).toInt()
        return HorizontalGridSegment(Tile(startX, y), endX - startX + 1)
    }
}

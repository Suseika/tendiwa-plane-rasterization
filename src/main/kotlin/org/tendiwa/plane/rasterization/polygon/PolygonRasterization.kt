package org.tendiwa.plane.rasterization.polygon

import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.grid.masks.mutable.MutableArrayGridMask
import org.tendiwa.plane.grid.rectangles.GridRectangle
import org.tendiwa.plane.grid.rectangles.maxY
import org.tendiwa.plane.grid.tiles.Tile
import org.tendiwa.plane.rasterization.segmentGroups.gridHull
import org.tendiwa.plane.rasterization.segments.GridSegment

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
                        .map { Row(polygon, it, this) }
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

}

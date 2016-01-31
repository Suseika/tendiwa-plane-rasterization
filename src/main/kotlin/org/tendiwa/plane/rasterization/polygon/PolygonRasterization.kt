package org.tendiwa.plane.rasterization.polygon

import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.grid.masks.mutable.MutableArrayGridMask
import org.tendiwa.plane.grid.rectangles.GridRectangle
import org.tendiwa.plane.grid.rectangles.maxY
import org.tendiwa.plane.rasterization.segmentGroups.gridHull
import org.tendiwa.plane.rasterization.segments.GridSegment

fun Polygon.rasterize(): MutableArrayGridMask =
    PolygonRasterization(this).result

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

    internal val result: MutableArrayGridMask = computeGridMask(poly.gridHull)

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
            .filter { it.needsExplicitRasterization() }
            .flatMap { GridSegment(it).tiles }
            .forEach {
                tile ->
                if (!contains(tile.x, tile.y)) {
                    add(tile.x, tile.y)
                }
            }
    }

    private fun Segment.needsExplicitRasterization(): Boolean =
        start.y != Math.floor(start.y)
            && start.y == end.y
}

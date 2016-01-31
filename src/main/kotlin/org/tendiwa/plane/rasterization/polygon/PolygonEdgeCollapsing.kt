package org.tendiwa.plane.rasterization.polygon

import org.tendiwa.collections.loopedTriLinks
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.polygons.Polygon

/**
 * Returns a polygon that is the same as this one, but has no such three
 * consecutive points a, b, c for which a.y == b.y == c.y
 */
internal fun Polygon.collapseHorizontalChains(): Polygon =
    points
        .loopedTriLinks
        .filter { !haveSameYCoord(it.first, it.second, it.third) }
        .map { it.second }
        .let { Polygon(it) }

private fun haveSameYCoord(a: Point, b: Point, c: Point): Boolean =
    a.y == b.y && b.y == c.y

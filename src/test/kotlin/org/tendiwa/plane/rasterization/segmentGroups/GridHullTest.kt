package org.tendiwa.plane.rasterization.segmentGroups

import org.junit.Test
import org.tendiwa.plane.geometry.dimensions.by
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.rectangles.Rectangle
import org.tendiwa.plane.grid.rectangles.GridRectangle
import kotlin.test.assertEquals

class GridHullTest {
    @Test
    fun gridHull() {
        assertEquals(
            GridRectangle(0, 0, 2, 2),
            Rectangle(0.0, 0.0, 1.0, 1.0).gridHull
        )
        assertEquals(
            GridRectangle(-1, -1, 1, 1),
            Rectangle(-1.0, -1.0, 0.4, 0.4).gridHull
        )
    }

    @Test
    fun `grid hull of a polygon smaller than 1 tile is 1 tile`() {
        assertEquals(
            GridRectangle(0, 0, 1, 1),
            Rectangle(Point(0.0, 0.0), 0.1 by 0.1).gridHull
        )
    }

    @Test
    fun `grid hull of a polygon with width less than 1 tile is 1 tile wide`() {
        Rectangle(Point(0.0, 0.0), 0.1 by 4.1)
            .gridHull
            .apply { assertEquals(1, width) }
    }

    @Test
    fun `grid hull of a polygon with height less than 1 tile is 1 tile tall`() {
        Rectangle(Point(0.0, 0.0), 4.1 by 0.1)
            .gridHull
            .apply { assertEquals(1, height) }
    }
}


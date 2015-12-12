package org.tendiwa.plane.rasterization.shapes

import org.junit.Test
import org.tendiwa.plane.geometry.rectangles.Rectangle
import org.tendiwa.plane.grid.rectangles.GridRectangle
import kotlin.test.assertEquals

class ShapeRasterizationTest {
    @Test fun gridHull() {
        assertEquals(
            GridRectangle(0, 0, 2, 2),
            Rectangle(0.0, 0.0, 1.0, 1.0).gridHull
        )
        assertEquals(
            GridRectangle(-1, -1, 1, 1),
            Rectangle(-1.0, -1.0, 0.4, 0.4).gridHull
        )
    }
}


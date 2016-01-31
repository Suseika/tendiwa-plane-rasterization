package org.tendiwa.plane.rasterization.polygons

import org.junit.Test
import org.tendiwa.plane.directions.CardinalDirection.*
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.rasterization.polygon.rasterized

class PolygonRasterizationTest {
    @Test fun rasterizesPolygonWithConsecutiveHorizontalEdges() {
        Polygon(
            Point(0.0, 10.0),
            Point(10.0, 10.0),
            Point(10.0, 20.0),
            Point(20.0, 20.0),
            Point(20.0, 0.0),
            Point(30.0, 0.0),
            Point(30.0, 30.0),
            Point(40.0, 30.0),
            Point(40.0, 20.0),
            Point(50.0, 20.0),
            Point(50.0, 30.0),
            Point(60.0, 30.0),
            Point(60.0, 20.0),
            Point(70.0, 20.0),
            Point(70.0, 40.0),
            Point(0.0, 40.0)
        )
            .rasterized
            .apply { assert(tiles.size > 0) }
    }

    @Test
    fun `rasterizes a polygon smaller than 1 tile`() {
        Polygon(
            Point(0.0, 0.0),
            Point(0.1, 0.0),
            Point(0.1, 0.1)
        )
            .rasterized
            .apply { assert(tiles.size > 0) }
    }

    @Test
    fun `rasterizes a triangle`() {
        Polygon(
            Point(6.60156925537876, 2.19927888657355),
            Point(4.28376357283713, 3.48064706953204),
            Point(5.1318503223609, 5.01471066926644)
        )
            .rasterized
            .apply { assert(tiles.size > 0) }
    }

    @Test
    fun `rasterizes a polygon with an almost horizontal edge`() {
        Polygon(
            Point(0.0, 0.0),
            {
                move(10.0, E)
                move(10.0 + 1e-11, N)
                move(10.0, -2e-11)
                move(10.0, E)
                move(20.0, N)
                move(30.0, W)
            }
        )
            .rasterized
            .apply { assert(tiles.size > 0) }
    }
}

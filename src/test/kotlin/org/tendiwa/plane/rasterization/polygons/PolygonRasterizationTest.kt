package org.tendiwa.plane.rasterization.polygons

import org.junit.Test
import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.geometry.polygons.Polygon
import org.tendiwa.plane.rasterization.polygon.rasterized

class PolygonRasterizationTest {
    @Test fun rasterizes() {
        Polygon(
            Point(0.0, 0.0),
            Point(20.0, 5.0),
            Point(10.0, 10.0)
        ).rasterized
    }

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
        ).rasterized
    }
}

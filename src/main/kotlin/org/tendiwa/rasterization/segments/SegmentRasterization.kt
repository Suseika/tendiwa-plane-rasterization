package org.tendiwa.rasterization.segments

import org.tendiwa.geometry.continuum.segments.Segment
import org.tendiwa.grid.segments.GridSegment
import org.tendiwa.rasterization.points.Tile

fun GridSegment(segment: Segment): GridSegment =
    GridSegment(Tile(segment.start), Tile(segment.end))


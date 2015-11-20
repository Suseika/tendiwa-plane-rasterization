package org.tendiwa.rasterization.segments

import org.tendiwa.geometry.segments.Segment
import org.tendiwa.grid.segments.GridSegment
import org.tendiwa.rasterization.points.tile

fun GridSegment(segment: Segment): GridSegment =
    GridSegment(segment.start.tile, segment.end.tile)


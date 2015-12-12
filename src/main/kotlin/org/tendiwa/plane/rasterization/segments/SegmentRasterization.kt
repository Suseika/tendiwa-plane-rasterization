package org.tendiwa.plane.rasterization.segments

import org.tendiwa.plane.geometry.segments.Segment
import org.tendiwa.plane.grid.segments.GridSegment
import org.tendiwa.plane.rasterization.points.tile

fun GridSegment(segment: Segment): GridSegment =
    GridSegment(segment.start.tile, segment.end.tile)


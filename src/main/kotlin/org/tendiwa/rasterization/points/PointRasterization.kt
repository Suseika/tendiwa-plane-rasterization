package org.tendiwa.rasterization.points

import org.tendiwa.geometry.continuum.points.Point
import org.tendiwa.grid.tiles.Tile
import org.tendiwa.math.doubles.closestInt

fun Tile(point: Point): Tile =
    Tile(point.x.closestInt, point.y.closestInt)

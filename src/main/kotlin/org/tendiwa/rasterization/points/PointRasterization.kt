package org.tendiwa.rasterization.points

import org.tendiwa.geometry.points.Point
import org.tendiwa.grid.tiles.Tile
import org.tendiwa.math.doubles.closestInt

val Point.tile: Tile
    get() = Tile(x.closestInt, y.closestInt)


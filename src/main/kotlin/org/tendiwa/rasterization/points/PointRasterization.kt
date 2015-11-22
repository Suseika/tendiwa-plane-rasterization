package org.tendiwa.rasterization.points

import org.tendiwa.geometry.points.Point
import org.tendiwa.grid.tiles.Tile

val Point.tile: Tile
    get() = Tile(x.toInt(), y.toInt())


package org.tendiwa.plane.rasterization.points

import org.tendiwa.plane.geometry.points.Point
import org.tendiwa.plane.grid.tiles.Tile

val Point.tile: Tile
    get() = Tile(x.toInt(), y.toInt())


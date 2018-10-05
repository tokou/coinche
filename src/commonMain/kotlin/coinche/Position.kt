package coinche

enum class Position(private val str: String) {
    NORTH("N"),
    WEST("W"),
    SOUTH("S"),
    EAST("E");

    override fun toString(): String = str
}

operator fun Position.plus(offset: Int): Position {
    val positions = Position.values()
    val size = positions.size
    val index = this.ordinal + offset
    return positions[(index % size + size) % size] // ensure we stay in [0, size)
}

operator fun Position.minus(offset: Int): Position = plus(-offset)

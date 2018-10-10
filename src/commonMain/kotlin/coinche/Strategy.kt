package coinche

interface Strategy {
    fun handleGameState(state: GameState)
    fun playCard(playableCards: Set<Card>): Card
    fun makeBid(history: List<Pair<Position, BiddingStep>>): BiddingStep
}

object DummyStrategy : Strategy {
    override fun handleGameState(state: GameState) {}
    override fun playCard(playableCards: Set<Card>): Card = playableCards.first()
    override fun makeBid(history: List<Pair<Position, BiddingStep>>): BiddingStep =
        if (history.isEmpty()) Bid.Contract(Position.NORTH, Suit.HEART, 90) else Pass
}

expect class HumanStrategy(position: Position) : Strategy

object StrategiesFactory {
    val humanVsDummy = mapOf(
        Position.NORTH to HumanStrategy(Position.NORTH),
        Position.WEST to DummyStrategy,
        Position.SOUTH to DummyStrategy,
        Position.EAST to DummyStrategy
    )

    val dummies = Position.values().associate { it to DummyStrategy }

    val humans = Position.values().associate { it to HumanStrategy(it) }
}

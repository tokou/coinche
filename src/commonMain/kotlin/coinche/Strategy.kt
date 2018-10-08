package coinche

interface Strategy {
    fun handleGameState(state: GameState)
    fun playCard(playableCards: Set<Card>): Card
    fun makeBid(history: List<Pair<Position, BiddingStep>>): BiddingStep
}

object DummyStrategy : Strategy {
    override fun handleGameState(state: GameState) {}
    override fun playCard(playableCards: Set<Card>): Card = playableCards.first()
    override fun makeBid(history: List<Pair<Position, BiddingStep>>): BiddingStep = Pass
}

expect class HumanStrategy(position: Position) : Strategy

object StrategiesFactory {
    val humanVsDummy = mapOf(
        Position.NORTH to HumanStrategy(Position.NORTH),
        Position.WEST to DummyStrategy,
        Position.SOUTH to DummyStrategy,
        Position.EAST to DummyStrategy
    )
}

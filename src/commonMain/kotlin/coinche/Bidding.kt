package coinche

typealias BidScore = Int

enum class CoincheStatus {
    NONE, COINCHE, SURCOINCHE
}

sealed class BiddingStep

object Pass : BiddingStep() {
    override fun toString(): String = "Pass"
}

sealed class Bid(val coincheStatus: CoincheStatus = CoincheStatus.NONE) : BiddingStep() {
    abstract val suit: Suit
    abstract val position: Position

    data class Contract(override val position: Position, override val suit: Suit, val contract: BidScore) : Bid()
    data class Capot(override val position: Position, override val suit: Suit) : Bid()
    data class Generale(override val position: Position, override val suit: Suit) : Bid()
}

package coinche

data class Player(
    val hand: Set<Card>
) {
    override fun toString(): String = " P$hand"
}

fun Map<Position, Player>.areEmptyHanded(): Boolean = values.map { it.hand.size }.sum() == 0

fun Map<Position, Player>.findBelote(trumpSuit: Suit): Position? = entries.find {
    val hand = it.value.hand
    val belote = Card(Rank.QUEEN, trumpSuit)
    val rebelote = Card(Rank.KING, trumpSuit)
    hand.contains(belote) && hand.contains(rebelote)
}?.key

typealias Score = Pair<Int, Int>

operator fun Score.plus(other: Score) = first + other.first to second + other.second

data class Trick(
    val cards: List<Card>,
    val players: Map<Position, Player>,
    val startingPosition: Position
) {
    val currentPosition: Position = startingPosition + cards.size

    val currentPlayer: Player? = if (isDone()) null else players[currentPosition]

    fun isDone(): Boolean = cards.size == players.size

    override fun toString(): String = "T$cards | ${currentPosition - 1} played ${cards.lastOrNull() ?: ""}"
}

fun Trick.isNotDone(): Boolean = !isDone()

data class Round(
    val tricks: List<Trick>,
    val players: Map<Position, Player>,
    val startingPosition: Position,
    val bid: Bid,
    val belotePosition: Position?,
    val currentPoints: Score = 0 to 0
) {
    fun isDone(): Boolean = players.areEmptyHanded()
}

fun Round.isNotDone(): Boolean = !isDone()

fun Round.findWinners() = tricks.map {
    val winningCard = findWinningCard(it, bid.suit)
    it.startingPosition + it.cards.indexOf(winningCard)
}.distinct()

data class Game(
    val firstToPlay: Position,
    val rounds: List<Round> = emptyList(),
    val score: Score = 0 to 0,
    val winningScore: Int = 1001
) {
    fun isDone(): Boolean = score.first >= winningScore || score.second >= winningScore
}

fun Game.isNotDone(): Boolean = !isDone()

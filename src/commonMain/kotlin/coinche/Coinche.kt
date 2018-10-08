package coinche

data class Player(
    val hand: Set<Card>
) {
    fun updateHand(hand: Set<Card>): Player = copy(hand = hand)

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

    fun addCard(card: Card): Trick = copy(cards = cards + card)

    fun updatePlayers(players: Map<Position, Player>): Trick = copy(players = players)

    override fun toString(): String = "T$cards | ${currentPosition - 1} played ${cards.lastOrNull() ?: ""}"
}

fun Trick.isNotDone(): Boolean = !isDone()

data class Round(
    val tricks: List<Trick>,
    val players: Map<Position, Player>,
    val startingPosition: Position,
    val biddingSteps: List<Pair<Position, BiddingStep>>,
    val belotePosition: Position?,
    val currentPoints: Score = 0 to 0
) {
    val currentTrick: Trick
        get() = tricks.last()
    val bid: Bid
        get() = biddingSteps.last { it.second is Bid }.second as Bid

    fun isDone(): Boolean = biddingSteps.all { it.second == Pass } || players.areEmptyHanded()

    fun addTrick(trick: Trick): Round = copy(tricks = tricks + trick)

    fun updateCurrentTrick(trick: Trick): Round = copy(tricks = tricks.dropLast(1) + trick)

    fun updatePlayers(players: Map<Position, Player>): Round = copy(players = players)

    fun updateStartingPosition(position: Position): Round = copy(startingPosition = position)

    fun addPoints(points: Score): Round = copy(currentPoints = currentPoints + points)
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
    val currentRound: Round
    get() = rounds.last()

    val currentTrick: Trick
    get() = currentRound.currentTrick

    fun isDone(): Boolean = score.first >= winningScore || score.second >= winningScore

    fun addRound(round: Round): Game = copy(rounds = rounds + round)

    fun updateCurrentRound(round: Round): Game = copy(rounds = rounds.dropLast(1) + round)

    fun addScore(roundScore: Score): Game = copy(score = score + roundScore)

    fun changeDealer(): Game = copy(firstToPlay = firstToPlay + 1)

    fun updateCurrentTrick(trick: Trick): Game =
        copy(rounds = rounds.dropLast(1) + currentRound.updateCurrentTrick(trick))
}

fun Game.isNotDone(): Boolean = !isDone()

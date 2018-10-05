package coinche

import kotlin.random.Random

enum class Suit(private val str: String) {
    SPADE("♠"),
    HEART("♥"),
    CLUB("♣"),
    DIAMOND("♦");

    override fun toString(): String = str
}

enum class Rank(
    val value: Int,
    val trumpValue: Int,
    private val str: String
) {
    SEVEN(0, 0, "7"),
    EIGHT(0, 0, "8"),
    NINE(0, 14, "9"),
    TEN(10, 10, "10"),
    JACK(2, 20, "J"),
    QUEEN(3, 3, "Q"),
    KING(4, 4, "K"),
    ACE(11, 11, "A");

    override fun toString(): String = str
}

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

typealias BidScore = Int

sealed class BiddingStep
object Pass : BiddingStep()
sealed class Bid(val coincheStatus: CoincheStatus = CoincheStatus.NONE) : BiddingStep() {
    abstract val suit: Suit
    abstract val position: Position

    data class Contract(override val position: Position, override val suit: Suit, val contract: BidScore) : Bid()
    data class Capot(override val position: Position, override val suit: Suit) : Bid()
    data class Generale(override val position: Position, override val suit: Suit) : Bid()

}

enum class CoincheStatus {
    NONE, COINCHE, SURCOINCHE
}

data class Card(
    val rank: Rank,
    val suit: Suit
) {
    override fun toString(): String = "$rank$suit"
}

data class Player(
    val hand: Set<Card>
) {
    override fun toString(): String = " P$hand"
}

data class Trick(
    val cards: List<Card>,
    val players: Map<Position, Player>,
    val startingPosition: Position
) {
    val currentPosition: Position = startingPosition + cards.size

    val currentPlayer: Player? =
        if (isDone()) null else players[currentPosition]

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

fun Map<Position, Player>.areEmptyHanded(): Boolean = values.map { it.hand.size }.sum() == 0

fun Map<Position, Player>.findBelote(trumpSuit: Suit): Position? = entries.find {
        val hand = it.value.hand
        val belote = Card(Rank.QUEEN, trumpSuit)
        val rebelote = Card(Rank.KING, trumpSuit)
        hand.contains(belote) && hand.contains(rebelote)
    }?.key

fun Round.isNotDone(): Boolean = !isDone()

typealias Score = Pair<Int, Int>

data class Game(
    val firstToPlay: Position,
    val rounds: List<Round> = emptyList(),
    val score: Score = 0 to 0,
    val winningScore: Int = 1001
) {
    fun isDone(): Boolean = score.first >= winningScore || score.second >= winningScore
}

fun Game.isNotDone(): Boolean = !isDone()

val biddingStrategy = Position.values().associate { position ->
    position to { history: List<Pair<Position, BiddingStep>> ->
        val random = Random.nextInt(16)
        if (random < 8) Pass else {
            val highestBid = history.findLast { it.second is Bid }?.second as Bid?
            if (highestBid == null) Bid.Contract(position, Suit.values()[Random.nextInt(Suit.values().size)], random * 10)
            else when (highestBid) {
                is Bid.Contract -> if (random * 10 > highestBid.contract) Bid.Contract(position, Suit.values()[Random.nextInt(Suit.values().size)], random * 10) else Pass
                else -> Pass
            }
        }
    }
}

fun play() {

    val firstToPlay = Position.NORTH
    var game = Game(firstToPlay)

    while (game.isNotDone()) {
        var drawnCards: Map<Position, Player>
        var tentativeBid: BiddingStep
        var roundStarter = game.firstToPlay
        do {
            drawnCards = drawCards()
            tentativeBid = doBidding(roundStarter, biddingStrategy)
            if (tentativeBid == Pass) roundStarter += 1
            println("Done bidding, tentative bid: $tentativeBid")
        } while (tentativeBid == Pass)

        val freshPlayers = drawnCards
        val winningBid = tentativeBid as Bid
        println("Winning bid : $winningBid")
        println()

        val belotePosition = freshPlayers.findBelote(winningBid.suit)
        var round = Round(emptyList(), freshPlayers, roundStarter, winningBid, belotePosition)

        while (round.isNotDone()) {
            var trick = Trick(emptyList(), round.players, round.startingPosition)
            while (trick.isNotDone()) {
                trick = advanceTrick(trick, round.bid.suit) {
                    findPlayableCards(trick, round.bid.suit).first()
                }
                println(trick)
            }
            val winningCard = findWinningCard(trick, round.bid.suit)
            val winnerPosition = trick.startingPosition + trick.cards.indexOf(winningCard)
            val isLastTrick = trick.players.areEmptyHanded()

            round = round.copy(
                tricks = round.tricks + trick,
                players = trick.players,
                startingPosition = winnerPosition,
                currentPoints = round.currentPoints + computeTrickPoints(trick, round.bid.suit, winnerPosition, isLastTrick)
            )
            println()
            println("$winnerPosition won the trick! ${round.tricks.size} played. Points: ${round.currentPoints}.")
            println()
        }
        require(round.currentPoints.first + round.currentPoints.second == 162)
        game = game.copy(
            rounds = game.rounds + round,
            firstToPlay = game.firstToPlay + 1,
            score = game.score + computeRoundScore(round)
        )
        println()
        println("Round done. Score is ${game.score}.")
    }

    println()
    println("Game Over. Score is ${game.score}.")
}

fun computeRoundScore(round: Round): Score {
    // Missing : Belote, Rebelote
    // Check also, other scoring rules
    require(round.isDone())
    val bid = round.bid
    val belotePoints = round.belotePosition?.let { when (it) {
        Position.NORTH, Position.SOUTH -> 20 to 0
        Position.EAST, Position.WEST -> 0 to 20
    } } ?: 0 to 0
    val points = round.currentPoints + belotePoints
    val attackerPoints = when (bid.position) {
        Position.NORTH, Position.SOUTH -> points.first
        Position.EAST, Position.WEST -> points.second
    }
    val bidSuccess = when (bid) {
        is Bid.Contract -> attackerPoints >= bid.contract
        is Bid.Capot -> round.findWinners().all { it == bid.position || it == bid.position + 2 }
        is Bid.Generale -> round.findWinners().all { it == bid.position }
    }
    val bidValue = when (bid) {
        is Bid.Contract -> bid.contract
        is Bid.Capot -> 250
        is Bid.Generale -> 500
    }
    val baseAttackerScore = if (bidSuccess) bidValue else 0
    val attackerScore = baseAttackerScore * when (bid.coincheStatus) {
        CoincheStatus.NONE -> 1
        CoincheStatus.COINCHE -> 2
        CoincheStatus.SURCOINCHE -> 4
    }
    val defenderScore = if (bidSuccess) 0 else when (bid.coincheStatus) {
        CoincheStatus.NONE -> 160
        CoincheStatus.COINCHE -> 2 * bidValue
        CoincheStatus.SURCOINCHE -> 4 * bidValue
    }
    val contractScore = when (bid.position) {
        Position.NORTH, Position.SOUTH -> attackerScore to defenderScore
        Position.EAST, Position.WEST -> defenderScore to attackerScore
    }
    return contractScore + belotePoints
}

fun Round.findWinners() = tricks.map {
        val winningCard = findWinningCard(it, bid.suit)
        it.startingPosition + it.cards.indexOf(winningCard)
    }.distinct()

fun computeTrickPoints(
    trick: Trick,
    trumpSuit: Suit,
    winnerPosition: Position,
    isLastTrick: Boolean
): Score {
    require(trick.isDone())
    val points = trick.cards.map { if (it.suit == trumpSuit) it.rank.trumpValue else it.rank.value }.sum()
    val total = points + if (isLastTrick) 10 else 0
    return when (winnerPosition) {
        Position.NORTH, Position.SOUTH -> total to 0
        Position.EAST, Position.WEST -> 0 to total
    }
}

operator fun Score.plus(other: Score) = first + other.first to second + other.second

fun findWinningCard(trick: Trick, trumpSuit: Suit): Card {
    require(trick.isDone())
    val trumpCards = trick.cards.filter { it.suit == trumpSuit }.sortedByDescending { it.rank.trumpValue }
    if (trumpCards.isNotEmpty()) return trumpCards.first()
    val playedSuit = trick.cards.first().suit
    return trick.cards.filter { it.suit == playedSuit }.sortedByDescending { it.rank.value }.first()
}

private fun doBidding(
    startingPosition: Position,
    deciders: Map<Position, (decisions: List<Pair<Position, BiddingStep>>) -> BiddingStep>
): BiddingStep {
    val steps = mutableListOf<Pair<Position, BiddingStep>>()
    var speaker = startingPosition
    do {
        val decision = speaker to deciders[speaker]!!(steps)
        if (decision is Bid && decision.coincheStatus == CoincheStatus.NONE) require(decision.position == speaker)
        println("$speaker bids ${decision.second}")
        steps.add(decision)
        speaker += 1
    } while (steps.size < 4 || steps.takeLast(3).map { it.second }.any { it != Pass })
    return steps.dropLast(3).last().second
}

fun advanceTrick(trick: Trick, trumpSuit: Suit, play: (Trick) -> Card): Trick {
    if (trick.isDone()) return trick
    val currentPlayer = trick.currentPlayer!! // Contract
    val currentPosition = trick.startingPosition + trick.cards.size
    val card = play(trick)
    val validCards = findPlayableCards(trick, trumpSuit)
    require(validCards.contains(card)) { "Invalid move playing $card. Valid cards are $validCards." }
    val newHand = currentPlayer.hand - card
    val newPlayers = trick.players.mapValues { when (it.key) {
        currentPosition -> currentPlayer.copy(hand = newHand)
        else -> it.value
    } }
    return trick.copy(cards = trick.cards + card, players = newPlayers)
}


val allCards = Rank.values().flatMap { r -> Suit.values().map { c -> Card(r, c) } }

private fun drawCards(): Map<Position, Player> {
    val shuffled = allCards.shuffled()
    return Position.values().associate { position ->
        val lowerBound = 8 * position.ordinal
        val higherBound = 8 * (position.ordinal + 1)
        val playerHand = shuffled.subList(lowerBound, higherBound).toSet()
        position to Player(playerHand)
    }
}

fun Card.isBetterThan(other: Card, trumpSuit: Suit): Boolean = when {
    suit == other.suit && suit == trumpSuit -> rank.trumpValue >= other.rank.trumpValue
    other.suit == trumpSuit -> false
    suit == trumpSuit -> true
    else -> rank.value >= other.rank.value
}

fun findPlayableCards(trick: Trick, trumpSuit: Suit): Set<Card> {
    if (trick.isDone()) return emptySet()

    val trickCards = trick.cards
    val playerHand = trick.currentPlayer!!.hand

    if (trickCards.isEmpty()) return playerHand

    val askedSuit = trickCards.first().suit
    val playerCardsOfAskedSuit = playerHand.filter { it.suit == askedSuit }.toSet()

    val trickTrumpCards = trickCards.filter { it.suit == trumpSuit }.sortedBy { it.rank.trumpValue }

    val playerTrumpCards = playerHand.filter { it.suit == trumpSuit }.toSet()

    if (trickTrumpCards.isEmpty()) {
        if (playerCardsOfAskedSuit.isNotEmpty()) return playerCardsOfAskedSuit

        if (partnerIsWinningTrick(trick, trumpSuit)) return playerHand

        if (playerTrumpCards.isNotEmpty()) return playerTrumpCards

        return playerHand
    }

    val bestPlayedTrump = trickTrumpCards.first()

    val playerBetterTrumpCards = playerTrumpCards
        .filter { it.isBetterThan(bestPlayedTrump, trumpSuit) }
        .toSet()

    if (askedSuit == trumpSuit && playerBetterTrumpCards.isNotEmpty()) return playerBetterTrumpCards

    if (playerCardsOfAskedSuit.isNotEmpty()) return playerCardsOfAskedSuit

    if (partnerIsWinningTrick(trick, trumpSuit)) return playerHand

    if (playerBetterTrumpCards.isNotEmpty()) return playerBetterTrumpCards

    return playerHand
}

fun partnerIsWinningTrick(trick: Trick, trumpSuit: Suit): Boolean {
    val partnerPosition = trick.currentPosition - 2
    val partnerCard = trick.cards.getOrNull(partnerPosition.ordinal) ?: return false
    return trick.cards.all {
        partnerCard.isBetterThan(it, trumpSuit) &&
                (partnerCard.suit == trumpSuit || partnerCard.suit == trick.cards.first().suit)
    }
}

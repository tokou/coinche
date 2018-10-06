package coinche

import coinche.Rank.*
import coinche.Suit.*

fun String.cards(): List<Card> = split(' ')
    .map { it.dropLast(1) to it.last().toString() }
    .map { parseRank(it.first) to parseColor(it.second) }
    .map { (rank, suit) -> Card(rank = rank, suit = suit) }

fun parseRank(str: String) = when (str) {
    "7" -> SEVEN
    "8" -> EIGHT
    "9" -> NINE
    "T", "10" -> TEN
    "J" -> JACK
    "Q" -> QUEEN
    "K" -> KING
    "A" -> ACE
    else -> throw RuntimeException("Unknown card rank")
}

fun parseColor(str: String) = when (str) {
    "♣", "C", "c" -> CLUB
    "♠", "S", "s" -> SPADE
    "♦", "D", "d" -> DIAMOND
    "♥", "H", "h" -> HEART
    else -> throw RuntimeException("Unknown card color")
}

fun createPlayer(cards: List<Card> = "7♣ 8♣ 9♣ 10♣ J♣ 7♦".cards()): Player = Player(cards.toSet())

fun createTrick(cards: List<Card>, currentPlayer: Player): Trick = Trick(
    cards = cards,
    players = mapOf(Position.values()[cards.size] to currentPlayer),
    startingPosition = Position.NORTH
)

package coinche

import coinche.Rank.*
import coinche.Suit.*

fun String.cards(): List<Card> = split(' ')
    .map { it.dropLast(1) to it.last().toString() }
    .map { parseRank(it.first) to parseSuit(it.second) }
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

fun parseSuit(str: String) = when (str) {
    "♣", "C", "c" -> CLUB
    "♠", "S", "s" -> SPADE
    "♦", "D", "d" -> DIAMOND
    "♥", "H", "h" -> HEART
    else -> throw RuntimeException("Unknown card color")
}

fun String.bid(position: Position): BiddingStep = when (this) {
    "pass", "p", "" -> Pass
    else -> Bid.Contract(position, parseSuit(takeLast(1)), dropLast(1).toInt())
}

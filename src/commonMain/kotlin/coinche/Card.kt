package coinche

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

data class Card(
    val rank: Rank,
    val suit: Suit
) {
    override fun toString(): String = "$rank$suit"
}

val allCards = Rank.values().flatMap { r -> Suit.values().map { c -> Card(r, c) } }

fun Card.isBetterThan(other: Card, trumpSuit: Suit): Boolean = when {
    suit == other.suit && suit == trumpSuit -> rank.trumpValue >= other.rank.trumpValue
    other.suit == trumpSuit -> false
    suit == trumpSuit -> true
    else -> rank.value >= other.rank.value
}

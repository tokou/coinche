package coinche

fun createPlayer(cards: List<Card> = "7♣ 8♣ 9♣ 10♣ J♣ 7♦".cards()): Player = Player(cards.toSet())

fun createTrick(cards: List<Card>, currentPlayer: Player): Trick = Trick(
    cards = cards,
    players = mapOf(Position.values()[cards.size] to currentPlayer),
    startingPosition = Position.NORTH
)

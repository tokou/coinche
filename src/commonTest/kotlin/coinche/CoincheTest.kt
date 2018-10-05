package coinche

import coinche.Rank.*
import coinche.Suit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoincheTest {

    private fun String.cards(): List<Card> = split(' ')
        .map { it.dropLast(1) to it.last().toString() }
        .map { parseRank(it.first) to parseColor(it.second) }
        .map { (rank, suit) -> Card(rank = rank, suit = suit) }

    private fun parseRank(str: String) = when (str) {
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

    private fun parseColor(str: String) = when (str) {
        "♣", "C", "c" -> CLUB
        "♠", "S", "s" -> SPADE
        "♦", "D", "d" -> DIAMOND
        "♥", "H", "h" -> HEART
        else -> throw RuntimeException("Unknown card color")
    }

    private fun createPlayer(cards: List<Card> = "7♣ 8♣ 9♣ 10♣ J♣ 7♦".cards()): Player = Player(cards.toSet())

    private fun createTrick(cards: List<Card>, currentPlayer: Player): Trick = Trick(
        cards = cards,
        players = mapOf(Position.values()[cards.size] to currentPlayer),
        startingPosition = Position.NORTH
    )

    @Test
    fun `player can play any cards if he starts`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♦".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = emptyList<Card>(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals(playerHand.toSet(), actualPlayableCards)
    }

    @Test
    fun `player must played regular asked color if he has one`() {
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "8♥ A♥".cards(),
                currentPlayer = createPlayer(cards = "7♣ 8♣ 9♣ 10♣ J♣ 7♥".cards())
            ),
            trumpSuit = SPADE
        )
        assertEquals("7♥".cards().toSet(), actualPlayableCards)
    }

    @Test
    fun `player must cut`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♠".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "8♥ A♥".cards(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals("7♠".cards().toSet(), actualPlayableCards)
    }

    @Test
    fun `player can piss if partner is winning`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♠".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "A♥ 8♥".cards(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals(playerHand.toSet(), actualPlayableCards)
    }

    @Test
    fun `player must play higher trump`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♠ J♠".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "A♠ 8♥".cards(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals("J♠".cards().toSet(), actualPlayableCards)
    }

    @Test
    fun `player must play higher trump if opponent cut`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♠ J♠".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "8♥ A♠".cards(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals("J♠".cards().toSet(), actualPlayableCards)
    }

    @Test
    fun `player can play any card if opponent cut with higher trump`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♠".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "8♥ A♠".cards(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals(playerHand.toSet(), actualPlayableCards)
    }

    @Test
    fun `player can play any cards if he has not asked suit nor trump`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♦".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = "8♥ A♥".cards(),
                currentPlayer = createPlayer(cards = playerHand)
            ),
            trumpSuit = SPADE
        )
        assertEquals(playerHand.toSet(), actualPlayableCards)
    }


    @Test
    fun `partner is winning`() {
        val trick = createTrick(
            cards = "A♥ 8♥".cards(),
            currentPlayer = createPlayer()
        )
        assertTrue(partnerIsWinningTrick(trick, SPADE))
    }

    @Test
    fun `partner is not winning`() {
        val trick = createTrick(
            cards = "8♥ A♥".cards(),
            currentPlayer = createPlayer()
        )
        assertFalse(partnerIsWinningTrick(trick, SPADE))
    }
}

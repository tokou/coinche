package coinche

import coinche.Suit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoincheTest {

    @Test
    fun `player can play any cards if he starts`() {
        val playerHand = "7♣ 8♣ 9♣ 10♣ J♣ 7♦".cards()
        val actualPlayableCards = findPlayableCards(
            trick = createTrick(
                cards = emptyList(),
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

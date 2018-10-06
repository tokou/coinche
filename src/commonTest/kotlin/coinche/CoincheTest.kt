package coinche

import coinche.Suit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoincheTest {

    @Test
    fun player_can_play_any_cards_if_he_starts() {
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
    fun player_must_played_regular_asked_color_if_he_has_one() {
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
    fun player_must_cut() {
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
    fun player_can_piss_if_partner_is_winning() {
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
    fun player_must_play_higher_trump() {
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
    fun player_must_play_higher_trump_if_opponent_cut() {
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
    fun player_can_play_any_card_if_opponent_cut_with_higher_trump() {
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
    fun player_can_play_any_cards_if_he_has_not_asked_suit_nor_trump() {
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
    fun partner_is_winning() {
        val trick = createTrick(
            cards = "A♥ 8♥".cards(),
            currentPlayer = createPlayer()
        )
        assertTrue(partnerIsWinningTrick(trick, SPADE))
    }

    @Test
    fun partner_is_not_winning() {
        val trick = createTrick(
            cards = "8♥ A♥".cards(),
            currentPlayer = createPlayer()
        )
        assertFalse(partnerIsWinningTrick(trick, SPADE))
    }
}

package coinche

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

typealias GameState = Pair<Update, Game>

expect fun showGameState(state: GameState)
expect fun inputCard(playableCards: Set<Card>): Card

suspend fun runGame() = coroutineScope {
    val states = Channel<Pair<Update, Game>>(Channel.UNLIMITED)
    val cards = Channel<Card>(Channel.UNLIMITED)
    val bids = Channel<BiddingStep>(Channel.UNLIMITED)

    launch { startGame(states, cards, bids) }

    for (state in states) {
        showGameState(state)
        if (state.shouldBid()) bid(state.second, bids)
        if (state.shouldPlay()) play(state.second, cards)
        if (state.second.isDone()) states.close()
    }
}

private suspend fun bid(game: Game, bids: SendChannel<BiddingStep>) {
    bids.send(randomBiddingStep(game))
}

private fun randomBiddingStep(game: Game): BiddingStep {
    val random = Random.nextInt(16)
    if (random < 8) return Pass

    val highestBid = game.currentRound.biddingSteps.findLast { it is Bid } as Bid?
    val bidderPosition = game.currentRound.startingPosition + game.currentRound.biddingSteps.size
    val contract = Bid.Contract(bidderPosition, Suit.values()[Random.nextInt(Suit.values().size)], random * 10)
    if (highestBid == null) return contract

    return when (highestBid) {
        is Bid.Contract -> if (random * 10 > highestBid.contract) contract else Pass
        else -> Pass
    }
}

private suspend fun play(game: Game, cards: SendChannel<Card>) {
    val playableCards = findPlayableCards(game.currentTrick, game.currentRound.bid.suit)
    val isHumanPlayer = game.currentTrick.currentPosition == Position.NORTH
    val card = if (isHumanPlayer) {
        inputCard(playableCards)
    } else {
        playableCards.first()
    }
    cards.send(card)
}

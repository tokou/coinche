package coinche

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

typealias GameState = Pair<Update, Game>

suspend fun runGame(
    strategies: Map<Position, Strategy> = StrategiesFactory.humanVsDummy
) = coroutineScope {
    val states = Channel<Pair<Update, Game>>(Channel.UNLIMITED)
    val cards = Channel<Card>(Channel.UNLIMITED)
    val bids = Channel<BiddingStep>(Channel.UNLIMITED)

    launch { startGame(states, cards, bids) }

    for (state in states) {
        strategies.values.forEach { it.handleGameState(state) }
        if (state.shouldBid()) bid(strategies, state.second, bids)
        if (state.shouldPlay()) play(strategies, state.second, cards)
        if (state.second.isDone()) states.close()
    }
}

private suspend fun bid(
    strategies: Map<Position, Strategy>, game: Game, bids: SendChannel<BiddingStep>
) {
    val steps = game.currentRound.biddingSteps
    val nextBidder =
        if (steps.isEmpty()) game.currentRound.startingPosition
        else steps.last().first + 1
    require(strategies.containsKey(nextBidder))
    val bid = strategies[nextBidder]!!.makeBid(steps)
    bids.send(bid)
}

private suspend fun play(strategies: Map<Position, Strategy>, game: Game, cards: SendChannel<Card>) {
    val playableCards = findPlayableCards(game.currentTrick, game.currentRound.bid.suit)
    val nextPlayer = game.currentTrick.currentPosition
    require(strategies.containsKey(nextPlayer))
    val card = strategies[nextPlayer]!!.playCard(playableCards)
    require(playableCards.contains(card))
    cards.send(card)
}

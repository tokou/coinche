package coinche

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

typealias GameState = Pair<Update, Game>

expect fun showGameState(state: GameState)
expect fun inputCard(playableCards: Set<Card>): Card

suspend fun runGame() = coroutineScope {
    val states = Channel<Pair<Update, Game>>(Channel.UNLIMITED)
    val cards = Channel<Card>(Channel.UNLIMITED)

    launch { startGame(states, cards) }

    for (state in states) {
        showGameState(state)
        if (state.shouldPlay()) play(state.second, cards)
        if (state.second.isDone()) states.close()
    }
}

private suspend fun play(game: Game, cards: Channel<Card>) {
    val playableCards = findPlayableCards(game.currentTrick, game.currentRound.bid.suit)
    val isHumanPlayer = game.currentTrick.currentPosition == Position.NORTH
    val card = if (isHumanPlayer) {
        inputCard(playableCards)
    } else {
        playableCards.first()
    }
    cards.send(card)
}

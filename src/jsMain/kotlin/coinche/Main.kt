package coinche

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.browser.window

fun main() {
    GlobalScope.launch { runGame() }
}

actual fun inputCard(playableCards: Set<Card>): Card {
    val input = window.prompt("Select card to play: $playableCards")
    return input!!.cards().first()
}

actual fun showGameState(state: GameState) {
    val (update, game) = state
    when (update) {
        Update.NEW_GAME -> {
            window.alert("Game starting.")
        }
        Update.NEW_ROUND -> {
            val hands = game.currentRound.players.map { e -> "${e.key} |${e.value.hand}" }
            window.alert("""
                |New round.
                |${hands.joinToString("\n")}
                |Bid: ${game.currentRound.bid}""".trimMargin())
        }
        Update.NEW_TRICK -> { }
        Update.ADVANCE_TRICK -> {
            window.alert("${game.currentTrick}")
        }
        Update.END_TRICK -> {
            window.alert("""
                |${game.currentRound.startingPosition} won the trick!
                |${game.currentRound.tricks.size} tricks played.
                |Points: ${game.currentRound.currentPoints}.""".trimMargin())
        }
        Update.END_ROUND -> {
            window.alert("""Round done.
                            |Score is ${game.score}.""".trimMargin())
        }
        Update.END_GAME -> {
            window.alert("Game over.")
        }
    }
}

package coinche

import kotlinx.coroutines.channels.Channel

suspend fun main(args: Array<String>) {
    val states = Channel<Pair<Update, Game>>(Channel.UNLIMITED)
    play(states)
    for ((update, game) in states) {
        when (update) {
            Update.NEW_GAME -> {
                println("Game starting.")
                println()
            }
            Update.NEW_ROUND -> {
                println("New round.")
                game.currentRound.players.forEach { p, h -> println("$p |$h") }
                println("Bid: ${game.currentRound.bid}")
                println()
            }
            Update.NEW_TRICK -> {}
            Update.ADVANCE_TRICK -> {
                println("${game.currentTrick}")
            }
            Update.END_TRICK -> {
                println()
                println("${game.currentRound.startingPosition} won the trick!")
                println("${game.currentRound.tricks.size} tricks played.")
                println("Points: ${game.currentRound.currentPoints}.")
                println()
            }
            Update.END_ROUND -> {
                println("Round done.")
                println("Score is ${game.score}.")
                println()
            }
            Update.END_GAME -> {
                println("Game over.")
                states.close()
            }
        }
    }
}

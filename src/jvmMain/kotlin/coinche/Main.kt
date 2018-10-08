package coinche

suspend fun main(args: Array<String>) {
    runGame()
}

actual fun inputCard(playableCards: Set<Card>): Card {
    println("Select card to play: $playableCards")
    return readLine()!!.cards().first()
}

actual fun showGameState(state: GameState) {
    val (update, game) = state
    when (update) {
        Update.NEW_GAME -> {
            println("Game starting.")
            println()
        }
        Update.NEW_BIDDING -> {}
        Update.NEW_BIDDING_STEP -> {
            println("New bidding step: ${game.currentRound.biddingSteps.last()}")
            println()
        }
        Update.END_BIDDING -> {
            println("End of biddings: ${game.currentRound.biddingSteps}")
            println()
        }
        Update.NEW_ROUND -> {
            println("New round.")
            game.currentRound.players.forEach { p, h -> println("$p |$h") }
            println("Bid: ${game.currentRound.bid}")
            println()
        }
        Update.NEW_TRICK -> {
        }
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
        }
    }
}

package coinche

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    runGame()
}

actual class HumanStrategy actual constructor(private val position: Position) : Strategy {

    override fun makeBid(history: List<Pair<Position, BiddingStep>>): BiddingStep {
        for ((pos, bid) in history) {
            println("$pos bid $bid")
        }
        println("Make bid:")
        return readLine()!!.bid(position)
    }

    override fun playCard(playableCards: Set<Card>): Card {
        println("Select card to play: $playableCards")
        return readLine()!!.cards().first()
    }

    override fun handleGameState(state: GameState) {
        val (update, game) = state
        when (update) {
            Update.NEW_GAME -> {
                println("Game starting.")
                println()
            }
            Update.NEW_ROUND -> {
                println("New round.")
                game.currentRound.players.forEach { e -> println("${e.key} |${e.value.hand}") }
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
            Update.NEW_BIDDING -> {}
            Update.NEW_BIDDING_STEP -> {
                val bid = game.currentRound.biddingSteps.last()
                println("${bid.first} bid ${bid.second}")
                println()
            }
            Update.END_BIDDING -> {
                if (game.currentRound.isDone()) return
                println("Winning bid: ${game.currentRound.bid}")
                println()
            }
        }
    }
}



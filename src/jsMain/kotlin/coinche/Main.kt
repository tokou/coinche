package coinche

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.browser.window

fun start() = GlobalScope.launch {
    runGame()
}

actual class HumanStrategy actual constructor(private val position: Position) : Strategy {

    override fun makeBid(history: List<Pair<Position, BiddingStep>>): BiddingStep {
        val prompt = history.joinToString(separator = "\n", postfix = "\n") { (pos, bid) ->
            "$pos bid $bid"
        } + "Make bid:"
        return window.prompt(prompt)!!.bid(position)
    }

    override fun playCard(playableCards: Set<Card>): Card {
        val input = window.prompt("Select card to play: $playableCards")
        return input!!.cards().first()
    }

    override fun handleGameState(state: GameState) {
        val (update, game) = state
        when (update) {
            Update.NEW_GAME -> {
                window.alert("Game starting.")
            }
            Update.NEW_ROUND -> {
                val hands = game.currentRound.players.map { e -> "${e.key} |${e.value.hand}" }
                window.alert("""
                |New round.
                |${hands.joinToString("\n")}""".trimMargin())
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
            Update.NEW_BIDDING -> {}
            Update.NEW_BIDDING_STEP -> {
                val bid = game.currentRound.biddingSteps.last()
                window.alert("${bid.first} bid ${bid.second}")
            }
            Update.END_BIDDING -> {
                if (game.currentRound.isDone()) return
                window.alert("Winning bid: ${game.currentRound.bid}")
            }
        }
    }
}

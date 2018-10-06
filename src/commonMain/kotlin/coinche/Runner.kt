package coinche

import kotlin.random.Random


fun makeBidForPosition(position: Position): suspend (history: List<Pair<Position, BiddingStep>>) -> BiddingStep =
    { history -> makeBid(position, history) }

suspend fun makeBid(position: Position, history: List<Pair<Position, BiddingStep>>): BiddingStep {
    val random = Random.nextInt(16)
    if (random < 8) return Pass
    val highestBid = history.findLast { it.second is Bid }?.second as Bid?
    val contract = Bid.Contract(position, Suit.values()[Random.nextInt(Suit.values().size)], random * 10)
    if (highestBid == null) return contract
    return when (highestBid) {
        is Bid.Contract -> if (random * 10 > highestBid.contract) contract else Pass
        else -> Pass
    }
}

val biddingStrategy = Position.values().associate { position ->
    position to makeBidForPosition(position)
}

val playingStrategy =  { game: Game -> suspend {
    findPlayableCards(game.currentTrick, game.currentRound.bid.suit).first()
} }

suspend fun play() {

    val firstToPlay = Position.NORTH
    var game = Game(firstToPlay)

    while (game.isNotDone()) {
        val (freshPlayers, winningBid, roundStarter) = makeBids(game)

        println("Winning bid : $winningBid")
        println()

        val belotePosition = freshPlayers.findBelote(winningBid.suit)
        val round = Round(emptyList(), freshPlayers, roundStarter, winningBid, belotePosition)
        game = game.addRound(round)

        while (game.currentRound.isNotDone()) {

            val trick = Trick(emptyList(), game.currentRound.players, game.currentRound.startingPosition)

            game = game.updateCurrentRound(game.currentRound.addTrick(trick))

            while (game.currentTrick.isNotDone()) {
                val advancedTrick = advanceTrick(
                    game.currentTrick, game.currentRound.bid.suit, playingStrategy(game)
                )
                game = game.updateCurrentTrick(advancedTrick)

                println(advancedTrick)
            }

            val winningCard = findWinningCard(game.currentTrick, game.currentRound.bid.suit)
            val winnerPosition = game.currentTrick.startingPosition + game.currentTrick.cards.indexOf(winningCard)
            val isLastTrick = game.currentTrick.players.areEmptyHanded()

            val trickPoints = computeTrickPoints(game.currentTrick, game.currentRound.bid.suit, winnerPosition, isLastTrick)

            val updatedRound = game.currentRound
                .updatePlayers(game.currentTrick.players)
                .updateStartingPosition(winnerPosition)
                .addPoints(trickPoints)
            game = game.updateCurrentRound(updatedRound)

            println()
            println("$winnerPosition won the trick! ${game.currentRound.tricks.size} played. Points: ${game.currentRound.currentPoints}.")
            println()
        }

        val roundPoints = game.currentRound.currentPoints
        require(roundPoints.first + roundPoints.second == 162)

        val roundScore = computeRoundScore(game.currentRound)
        game = game.addScore(roundScore).changeDealer()

        println("Round done. Score is ${game.score}.")
    }

    println()
    println("Game Over. Score is ${game.score}.")
}

private suspend fun makeBids(game: Game): Triple<Map<Position, Player>, Bid, Position> {
    var drawnCards: Map<Position, Player>
    var tentativeBid: BiddingStep
    var biddingStarter = game.firstToPlay

    do {
        drawnCards = drawCards()
        tentativeBid = doBidding(biddingStarter, biddingStrategy)
        if (tentativeBid == Pass) biddingStarter += 1
    } while (tentativeBid == Pass)

    return Triple(drawnCards, tentativeBid as Bid, biddingStarter)
}

fun computeRoundScore(round: Round): Score {
    // Missing : Belote, Rebelote
    // Check also, other scoring rules
    require(round.isDone())
    val bid = round.bid
    val belotePoints = round.belotePosition?.let { when (it) {
        Position.NORTH, Position.SOUTH -> 20 to 0
        Position.EAST, Position.WEST -> 0 to 20
    } } ?: 0 to 0
    val points = round.currentPoints + belotePoints
    val attackerPoints = when (bid.position) {
        Position.NORTH, Position.SOUTH -> points.first
        Position.EAST, Position.WEST -> points.second
    }
    val bidSuccess = when (bid) {
        is Bid.Contract -> attackerPoints >= bid.contract
        is Bid.Capot -> round.findWinners().all { it == bid.position || it == bid.position + 2 }
        is Bid.Generale -> round.findWinners().all { it == bid.position }
    }
    val bidValue = when (bid) {
        is Bid.Contract -> bid.contract
        is Bid.Capot -> 250
        is Bid.Generale -> 500
    }
    val baseAttackerScore = if (bidSuccess) bidValue else 0
    val attackerScore = baseAttackerScore * when (bid.coincheStatus) {
        CoincheStatus.NONE -> 1
        CoincheStatus.COINCHE -> 2
        CoincheStatus.SURCOINCHE -> 4
    }
    val defenderScore = if (bidSuccess) 0 else when (bid.coincheStatus) {
        CoincheStatus.NONE -> 160
        CoincheStatus.COINCHE -> 2 * bidValue
        CoincheStatus.SURCOINCHE -> 4 * bidValue
    }
    val contractScore = when (bid.position) {
        Position.NORTH, Position.SOUTH -> attackerScore to defenderScore
        Position.EAST, Position.WEST -> defenderScore to attackerScore
    }
    return contractScore + belotePoints
}

fun computeTrickPoints(
    trick: Trick,
    trumpSuit: Suit,
    winnerPosition: Position,
    isLastTrick: Boolean
): Score {
    require(trick.isDone())
    val points = trick.cards.map { if (it.suit == trumpSuit) it.rank.trumpValue else it.rank.value }.sum()
    val total = points + if (isLastTrick) 10 else 0
    return when (winnerPosition) {
        Position.NORTH, Position.SOUTH -> total to 0
        Position.EAST, Position.WEST -> 0 to total
    }
}

fun findWinningCard(trick: Trick, trumpSuit: Suit): Card {
    require(trick.isDone())
    val trumpCards = trick.cards.filter { it.suit == trumpSuit }.sortedByDescending { it.rank.trumpValue }
    if (trumpCards.isNotEmpty()) return trumpCards.first()
    val playedSuit = trick.cards.first().suit
    return trick.cards.filter { it.suit == playedSuit }.sortedByDescending { it.rank.value }.first()
}

private suspend fun doBidding(
    startingPosition: Position,
    deciders: Map<Position, suspend (decisions: List<Pair<Position, BiddingStep>>) -> BiddingStep>
): BiddingStep {
    val steps = mutableListOf<Pair<Position, BiddingStep>>()
    var speaker = startingPosition
    do {
        val decision = speaker to deciders[speaker]!!(steps)
        if (decision is Bid && decision.coincheStatus == CoincheStatus.NONE) require(decision.position == speaker)
        println("$speaker bids ${decision.second}")
        steps.add(decision)
        speaker += 1
    } while (steps.size < 4 || steps.takeLast(3).map { it.second }.any { it != Pass })
    return steps.dropLast(3).last().second
}

suspend fun advanceTrick(trick: Trick, trumpSuit: Suit, play: suspend () -> Card): Trick {
    if (trick.isDone()) return trick
    val currentPlayer = trick.currentPlayer!! // Contract
    val currentPosition = trick.startingPosition + trick.cards.size
    val card = play()
    val validCards = findPlayableCards(trick, trumpSuit)
    require(validCards.contains(card)) { "Invalid move playing $card. Valid cards are $validCards." }
    val newHand = currentPlayer.hand - card
    val newPlayers = trick.players.mapValues { when (it.key) {
        currentPosition -> currentPlayer.updateHand(newHand)
        else -> it.value
    } }
    return trick.addCard(card).updatePlayers(newPlayers)
}

private fun drawCards(): Map<Position, Player> {
    val shuffled = allCards.shuffled()
    return Position.values().associate { position ->
        val lowerBound = 8 * position.ordinal
        val higherBound = 8 * (position.ordinal + 1)
        val playerHand = shuffled.subList(lowerBound, higherBound).toSet()
        position to Player(playerHand)
    }
}

fun findPlayableCards(trick: Trick, trumpSuit: Suit): Set<Card> {
    if (trick.isDone()) return emptySet()

    val trickCards = trick.cards
    val playerHand = trick.currentPlayer!!.hand

    if (trickCards.isEmpty()) return playerHand

    val askedSuit = trickCards.first().suit
    val playerCardsOfAskedSuit = playerHand.filter { it.suit == askedSuit }.toSet()

    val trickTrumpCards = trickCards.filter { it.suit == trumpSuit }.sortedBy { it.rank.trumpValue }

    val playerTrumpCards = playerHand.filter { it.suit == trumpSuit }.toSet()

    if (trickTrumpCards.isEmpty()) {
        if (playerCardsOfAskedSuit.isNotEmpty()) return playerCardsOfAskedSuit

        if (partnerIsWinningTrick(trick, trumpSuit)) return playerHand

        if (playerTrumpCards.isNotEmpty()) return playerTrumpCards

        return playerHand
    }

    val bestPlayedTrump = trickTrumpCards.first()

    val playerBetterTrumpCards = playerTrumpCards
        .filter { it.isBetterThan(bestPlayedTrump, trumpSuit) }
        .toSet()

    if (askedSuit == trumpSuit && playerBetterTrumpCards.isNotEmpty()) return playerBetterTrumpCards

    if (playerCardsOfAskedSuit.isNotEmpty()) return playerCardsOfAskedSuit

    if (partnerIsWinningTrick(trick, trumpSuit)) return playerHand

    if (playerBetterTrumpCards.isNotEmpty()) return playerBetterTrumpCards

    return playerHand
}

fun partnerIsWinningTrick(trick: Trick, trumpSuit: Suit): Boolean {
    val partnerPosition = trick.currentPosition - 2
    val partnerCard = trick.cards.getOrNull(partnerPosition.ordinal) ?: return false
    return trick.cards.all {
        partnerCard.isBetterThan(it, trumpSuit) &&
                (partnerCard.suit == trumpSuit || partnerCard.suit == trick.cards.first().suit)
    }
}

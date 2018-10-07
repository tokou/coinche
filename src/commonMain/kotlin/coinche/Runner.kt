package coinche


import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel


enum class Update {
    NEW_GAME,
    NEW_BIDDING,
    NEW_BIDDING_STEP,
    END_BIDDING,
    NEW_ROUND,
    NEW_TRICK,
    ADVANCE_TRICK,
    END_TRICK,
    END_ROUND,
    END_GAME
}

fun GameState.shouldPlay(): Boolean =
    (first == Update.NEW_TRICK || first == Update.ADVANCE_TRICK) && second.currentTrick.isNotDone()

fun GameState.shouldBid(): Boolean =
    (first == Update.NEW_BIDDING_STEP || first == Update.NEW_BIDDING) && shouldContinueBidding(second.currentRound.biddingSteps)

suspend fun startGame(
    state: SendChannel<GameState>,
    cards: ReceiveChannel<Card>,
    bids: ReceiveChannel<BiddingStep>
) {
    val firstToPlay = Position.NORTH
    var game = Game(firstToPlay)
    state.send(Update.NEW_GAME to game)

    while (game.isNotDone()) {
        game = playGame(game, state, cards, bids)
    }
    state.send(Update.END_GAME to game)
}

private suspend fun playGame(
    initializedGame: Game,
    state: SendChannel<GameState>,
    cards: ReceiveChannel<Card>,
    bids: ReceiveChannel<BiddingStep>
): Game {
    val round = Round(emptyList(), drawCards(), initializedGame.firstToPlay, emptyList(), belotePosition = null)
    var game = initializedGame.addRound(round)

    state.send(Update.NEW_BIDDING to game)
    game = makeBids(game, state, bids)
    if (game.currentRound.isDone()) {
        state.send(Update.END_ROUND to game)
        return game
    }

    val belotePosition = game.currentRound.players.findBelote(game.currentRound.bid.suit)
    game.updateCurrentRound(round.copy(belotePosition = belotePosition))
    state.send(Update.NEW_ROUND to game)

    while (game.currentRound.isNotDone()) {
        game = playRound(game, state, cards)
    }

    val roundPoints = game.currentRound.currentPoints
    require(roundPoints.first + roundPoints.second == 162)

    val roundScore = computeRoundScore(game.currentRound)
    game = game.addScore(roundScore).changeDealer()
    state.send(Update.END_ROUND to game)
    return game
}

private suspend fun makeBids(
    game: Game,
    state: SendChannel<GameState>,
    bids: ReceiveChannel<BiddingStep>
): Game {
    val biddedGame = doBidding(game, state, bids)
    state.send(GameState(Update.END_BIDDING, biddedGame))
    return biddedGame
}

private suspend fun playRound(
    undoneRoundGame: Game,
    state: SendChannel<GameState>,
    cards: ReceiveChannel<Card>
): Game {
    var game = undoneRoundGame
    val trick = Trick(emptyList(), game.currentRound.players, game.currentRound.startingPosition)

    game = game.updateCurrentRound(game.currentRound.addTrick(trick))
    state.send(Update.NEW_TRICK to game)

    while (game.currentTrick.isNotDone()) {
        val advancedTrick = advanceTrick(
            game.currentTrick, game.currentRound.bid.suit, cards.receive()
        )
        game = game.updateCurrentTrick(advancedTrick)
        state.send(Update.ADVANCE_TRICK to game)
    }

    return updateGameAfterTrickIsDone(game, state)
}

private suspend fun updateGameAfterTrickIsDone(
    game: Game,
    state: SendChannel<GameState>
): Game {
    val winningCard = findWinningCard(game.currentTrick, game.currentRound.bid.suit)
    val winnerPosition = game.currentTrick.startingPosition + game.currentTrick.cards.indexOf(winningCard)
    val isLastTrick = game.currentTrick.players.areEmptyHanded()

    val trickPoints =
        computeTrickPoints(game.currentTrick, game.currentRound.bid.suit, winnerPosition, isLastTrick)

    val updatedRound = game.currentRound
        .updatePlayers(game.currentTrick.players)
        .updateStartingPosition(winnerPosition)
        .addPoints(trickPoints)
    val updatedGame = game.updateCurrentRound(updatedRound)
    state.send(Update.END_TRICK to updatedGame)
    return updatedGame
}

fun computeRoundScore(round: Round): Score {
    // Missing : other scoring rules (like point done + contract, steal belote)
    require(round.isDone())
    val bid = round.bid
    val belotePoints = round.belotePosition?.let {
        when (it) {
            Position.NORTH, Position.SOUTH -> 20 to 0
            Position.EAST, Position.WEST -> 0 to 20
        }
    } ?: 0 to 0
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
    game: Game,
    state: SendChannel<GameState>,
    bids: ReceiveChannel<BiddingStep>
): Game {
    var biddingGame = game
    val steps = mutableListOf<Pair<Position, BiddingStep>>()
    var speaker = biddingGame.firstToPlay

    do {
        val decision = speaker to bids.receive()
        validateNewBiddingStep(decision, speaker)

        steps.add(decision)

        biddingGame =
                biddingGame.updateCurrentRound(biddingGame.currentRound.copy(biddingSteps = steps.map { it.second }))
        state.send(GameState(Update.NEW_BIDDING_STEP, biddingGame))
        speaker += 1
    } while (shouldContinueBidding(steps.map { it.second }))

    return biddingGame
}

private fun shouldContinueBidding(steps: List<BiddingStep>) =
    steps.size < 4 || steps.takeLast(3).any { it != Pass }

private fun validateNewBiddingStep(
    decision: Pair<Position, BiddingStep>,
    speaker: Position
) {
    if (decision is Bid && decision.coincheStatus == CoincheStatus.NONE)
        require(decision.position == speaker)
}

fun advanceTrick(trick: Trick, trumpSuit: Suit, card: Card): Trick {
    if (trick.isDone()) return trick
    val currentPlayer = trick.currentPlayer!! // Contract
    val currentPosition = trick.startingPosition + trick.cards.size
    val validCards = findPlayableCards(trick, trumpSuit)
    require(validCards.contains(card)) { "Invalid move playing $card. Valid cards are $validCards." }
    val newHand = currentPlayer.hand - card
    val newPlayers = trick.players.mapValues {
        when (it.key) {
            currentPosition -> currentPlayer.updateHand(newHand)
            else -> it.value
        }
    }
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

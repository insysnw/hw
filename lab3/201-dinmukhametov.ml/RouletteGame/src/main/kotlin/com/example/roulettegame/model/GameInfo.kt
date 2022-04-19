package com.example.roulettegame.model

import com.example.roulettegame.repository.Repository.croupier
import com.example.roulettegame.repository.Repository.gamesSize

class GameInfo {
    val numberGame: Int = gamesSize()
    val bets: MutableMap<String, Bet> = mutableMapOf()
    var gameStatus: GameStatus = GameStatus.IsStill
    var hasCroupier: Boolean = croupier != null

    operator fun plus(bet: Pair<String, Bet>): GameInfo {
        bets[bet.first] = bet.second
        return this
    }
}

sealed class GameStatus(val status: GameStatusEnum) {
    object IsStill : GameStatus(GameStatusEnum.IS_STILL)
    data class IsEnded(val winNum: Int) : GameStatus(GameStatusEnum.IS_ENDED)
}

enum class GameStatusEnum {
    IS_STILL,
    IS_ENDED
}

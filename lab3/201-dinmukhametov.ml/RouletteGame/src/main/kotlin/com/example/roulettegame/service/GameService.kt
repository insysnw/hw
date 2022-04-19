package com.example.roulettegame.service

import com.example.roulettegame.error.UserIsNotCroupierException
import com.example.roulettegame.model.*
import com.example.roulettegame.repository.Repository.curGame
import com.example.roulettegame.repository.Repository.isCroupier
import com.example.roulettegame.repository.Repository.newGame
import org.springframework.stereotype.Service

@Service
class GameService {
    fun gameInfo(
        betTypeEnum: BetTypeEnum,
        num: Int,
        sum: Double,
        login: String,
    ): GameInfo {
        val betType =
            when (betTypeEnum) {
                BetTypeEnum.ODD -> BetType.Odd
                BetTypeEnum.EVEN -> BetType.Even
                BetTypeEnum.NUM -> BetType.Num(num)
            }

        val bet = Bet(sum, betType)
        return curGame() + (login to bet)
    }

    fun startGame(login: String) {
        if (!isCroupier(login))
            throw UserIsNotCroupierException()

        val num = (0..36).random()
        curGame().run { gameStatus = GameStatus.IsEnded(num) }
        newGame()
    }
}
package com.example.roulettegame.contoller

import com.example.roulettegame.model.BetTypeEnum
import com.example.roulettegame.model.GameInfo
import com.example.roulettegame.repository.Repository.gameInfo
import com.example.roulettegame.service.GameService
import com.example.roulettegame.service.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/game")
class GameController {
    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var gameService: GameService

    @PostMapping("/createBet")
    fun createBet(
        @RequestHeader("Authorization") token: String,
        @RequestParam sum: Double,
        @RequestParam betTypeEnum: BetTypeEnum,
        num: Int,
    ): GameInfo {
        val login = jwtService.validateToken(token)
        return gameService.gameInfo(betTypeEnum, num, sum, login)
    }

    @GetMapping("/info")
    fun info(
        @RequestHeader("Authorization") token: String,
        @RequestParam(defaultValue = "-1") numberGame: Int,
    ): GameInfo {
        jwtService.validateToken(token)
        return gameInfo(numberGame)
    }

    @PostMapping("/start")
    fun start(@RequestHeader("Authorization") token: String) {
        val login = jwtService.validateToken(token)
        gameService.startGame(login)
    }
}
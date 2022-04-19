package com.example.roulettegame.repository

import com.example.roulettegame.error.CroupierAlreadyExistException
import com.example.roulettegame.error.GameNotExistException
import com.example.roulettegame.error.UserAlreadyExistException
import com.example.roulettegame.error.UserNotExistException
import com.example.roulettegame.model.GameInfo

object Repository {
    private val users = mutableSetOf<String>()
    var croupier: String? = null
        private set

    private val games = mutableListOf<GameInfo>()

    init {
        newGame()
    }

    fun curGame(): GameInfo = games.last()

    fun gameInfo(num: Int): GameInfo =
        when (num) {
            -1 -> curGame()
            !in 0 until games.size -> throw GameNotExistException()
            else -> games[num]
        }

    fun newGame() = games.add(GameInfo())

    fun gamesSize() = games.size

    fun isCroupier(login: String) = croupier == login

    fun createCroupier(login: String) {
        if (croupier == null) {
            croupier = login
            curGame().hasCroupier = true
        } else
            throw CroupierAlreadyExistException()
    }

    fun createUser(login: String, token: String): String =
        if (!users.contains(login)) {
            users.add(login)
            token
        } else
            throw UserAlreadyExistException()

    fun checkUserExist(login: String): String {
        if (users.contains(login)) return login
        else throw UserNotExistException()
    }

    fun removeCroupier() {
        croupier = null
        games.lastOrNull()?.let { it.hasCroupier = false }
    }

    fun removeUser(login: String) {
        users.remove(login)
    }
}

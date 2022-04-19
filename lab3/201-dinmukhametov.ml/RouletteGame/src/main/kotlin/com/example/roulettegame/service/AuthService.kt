package com.example.roulettegame.service

import com.example.roulettegame.repository.Repository.createCroupier
import com.example.roulettegame.repository.Repository.createUser
import com.example.roulettegame.repository.Repository.isCroupier
import com.example.roulettegame.repository.Repository.removeCroupier
import com.example.roulettegame.repository.Repository.removeUser
import org.springframework.stereotype.Service

@Service
class AuthService {
    fun login(isCroupier: Boolean, login: String, token: String): String {
        if (isCroupier)
            createCroupier(login)
        return createUser(login, token)
    }

    fun logout(login: String) {
        if (isCroupier(login)) removeCroupier()
        removeUser(login)
    }
}
package com.example.roulettegame.contoller

import com.example.roulettegame.service.AuthService
import com.example.roulettegame.service.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class AuthController {
    @Autowired
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var authService: AuthService

    @GetMapping("/login")
    fun login(@RequestParam login: String, isCroupier: Boolean = false): String {
        val token = jwtService.generateToken(login)

        return authService.login(isCroupier, login, token)
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") token: String) {
        val login = jwtService.validateToken(token)
        authService.logout(login)
    }
}





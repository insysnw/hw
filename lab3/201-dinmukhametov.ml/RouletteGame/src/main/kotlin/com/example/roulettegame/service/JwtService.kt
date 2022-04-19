package com.example.roulettegame.service

import com.example.roulettegame.error.AccessTokenException
import com.example.roulettegame.repository.Repository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JwtService {
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    fun generateToken(userName: String): String {
        return Jwts.builder()
            .setSubject(userName)
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact()
    }

    fun validateToken(fullToken: String?): String {
        val token = fullToken?.substring(7)
        val claims = try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
        } catch (_: Exception) {
            throw AccessTokenException()
        }
        val login = claims.body.subject
        return Repository.checkUserExist(login)
    }
}
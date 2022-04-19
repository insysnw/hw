package com.example.roulettegame.error

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "The access token is not valid")
class AccessTokenException(override val message: String = "") : RuntimeException()

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "The user not exist")
class UserNotExistException(override val message: String = "") : RuntimeException()

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "A user with this name already exists")
class UserAlreadyExistException(override val message: String = "") : RuntimeException()

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "The croupier already exists")
class CroupierAlreadyExistException(override val message: String = "") : RuntimeException()

@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "The User is not croupier")
class UserIsNotCroupierException(override val message: String = "") : RuntimeException()

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "The Game is not exist")
class GameNotExistException(override val message: String = "") : RuntimeException()

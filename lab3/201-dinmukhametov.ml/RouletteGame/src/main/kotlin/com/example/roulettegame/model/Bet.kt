package com.example.roulettegame.model

import com.example.roulettegame.error.IllegalNumException

data class Bet(val sum: Double, val type: BetType)

sealed class BetType(val type: BetTypeEnum) {
    object Odd : BetType(BetTypeEnum.ODD)
    object Even : BetType(BetTypeEnum.EVEN)
    class Num(val num: Int) : BetType(BetTypeEnum.NUM) {
        init {
            if (num !in 0..36) throw IllegalNumException()
        }
    }
}

enum class BetTypeEnum {
    ODD,
    EVEN,
    NUM
}

package com.example.roulettegame.model

data class Bet(val sum: Double, val type: BetType)

sealed class BetType(val type: BetTypeEnum) {
    object Odd : BetType(BetTypeEnum.ODD)
    object Even : BetType(BetTypeEnum.EVEN)
    class Num(val num: Int) : BetType(BetTypeEnum.NUM)
}

enum class BetTypeEnum {
    ODD,
    EVEN,
    NUM
}

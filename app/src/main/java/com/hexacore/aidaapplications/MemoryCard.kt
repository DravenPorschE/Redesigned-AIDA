package com.hexacore.aidaapplications

data class MemoryCard(
    val id: Int,
    val symbol: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
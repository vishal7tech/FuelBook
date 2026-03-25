package com.example.fuelbook.model

data class UpiOption(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isActive: Boolean = true
)

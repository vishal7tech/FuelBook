package com.example.fuelbook.model

data class Nozzle(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isActive: Boolean = true
)

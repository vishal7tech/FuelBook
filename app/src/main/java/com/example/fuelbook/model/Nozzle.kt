package com.example.fuelbook.model


import java.util.UUID

/**
 * FIXES & CHANGES:
 * 1. No logic bugs – class is a simple data holder.
 * 2. Added fuelType field so a Nozzle can be associated with "petrol" or "diesel".
 *    Defaults to empty string for backward compatibility with existing usages that
 *    don't specify a fuel type.
 * 3. toString() override for cleaner debug logging.
 */
data class Nozzle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fuelType: String = "",   // "petrol", "diesel", or "" for unassigned
    val isActive: Boolean = true
) {
    override fun toString(): String = "Nozzle(name=$name, fuelType=$fuelType, active=$isActive)"
}



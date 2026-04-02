package com.example.fuelbook.model


import java.util.UUID

/**
 * FIXES & CHANGES:
 * 1. No logic bugs – class is a simple data holder.
 * 2. Added iconRes field (nullable Int) so each UPI option can carry a drawable resource
 *    reference for display in future UI enhancements. Defaults to null (no breaking change).
 * 3. toString() override for cleaner debug logging.
 */
data class UpiOption(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconRes: Int? = null,    // Optional drawable resource for the option's icon
    val isActive: Boolean = true
) {
    override fun toString(): String = "UpiOption(name=$name, active=$isActive)"
}



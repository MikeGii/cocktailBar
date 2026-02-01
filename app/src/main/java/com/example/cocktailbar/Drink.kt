package com.example.cocktailbar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Drink(
    val id: String = "",
    val name: String = "",
    val category: String? = null,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @Transient
    var variants: List<DrinkVariant> = emptyList()
) {
    // Helper to get display price (first variant or range)
    fun getDisplayPrice(): String {
        if (variants.isEmpty()) return "0.00 €"
        if (variants.size == 1) return String.format("%.2f €", variants[0].price)

        val minPrice = variants.minOf { it.price }
        val maxPrice = variants.maxOf { it.price }
        return if (minPrice == maxPrice) {
            String.format("%.2f €", minPrice)
        } else {
            String.format("%.2f - %.2f €", minPrice, maxPrice)
        }
    }
}
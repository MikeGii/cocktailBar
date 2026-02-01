package com.example.cocktailbar.data.model

import com.example.cocktailbar.data.model.DrinkVariant
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

    // Helper to get display price for lists (with decimals)
    fun getDisplayPrice(): String {
        if (variants.isEmpty()) return "0€"
        if (variants.size == 1) return formatPrice(variants[0].price)

        val prices = variants.sortedBy { it.price }
        val minPrice = prices.first().price
        val maxPrice = prices.last().price
        return if (minPrice == maxPrice) {
            formatPrice(minPrice)
        } else {
            "${formatPrice(minPrice)} - ${formatPrice(maxPrice)}"
        }
    }

    // Helper to get compact price for template display (e.g., "7€/14€")
    fun getTemplatePrice(): String {
        if (variants.isEmpty()) return "0€"
        if (variants.size == 1) return formatPriceCompact(variants[0].price)

        val prices = variants.sortedBy { it.price }.map { formatPriceCompact(it.price) }
        return prices.joinToString("/")
    }

    private fun formatPrice(price: Double): String {
        return if (price == price.toLong().toDouble()) {
            "${price.toLong()}€"
        } else {
            String.format("%.2f€", price)
        }
    }

    private fun formatPriceCompact(price: Double): String {
        return if (price == price.toLong().toDouble()) {
            "${price.toLong()}€"
        } else {
            String.format("%.1f€", price).replace(".0€", "€")
        }
    }
}
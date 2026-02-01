package com.example.cocktailbar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrinkRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class DrinkVariantRequest(
    @SerialName("drink_id")
    val drinkId: String,
    @SerialName("size_name")
    val sizeName: String,
    val price: Double,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)
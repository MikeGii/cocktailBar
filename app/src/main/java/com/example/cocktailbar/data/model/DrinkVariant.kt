package com.example.cocktailbar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrinkVariant(
    val id: String = "",
    @SerialName("drink_id")
    val drinkId: String = "",
    @SerialName("size_name")
    val sizeName: String = "",
    val price: Double = 0.0,
    @SerialName("sort_order")
    val sortOrder: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
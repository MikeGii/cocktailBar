package com.example.cocktailbar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Template(
    val id: String = "",
    val name: String = "",
    @SerialName("background_url")
    val backgroundUrl: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("folder_id")
    val folderId: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int? = null,

    // Background positioning
    @SerialName("background_scale")
    val backgroundScale: Float = 1f,
    @SerialName("background_offset_x")
    val backgroundOffsetX: Float = 0f,
    @SerialName("background_offset_y")
    val backgroundOffsetY: Float = 0f,

    // Logo positioning (0-1 relative to canvas)
    @SerialName("logo_x")
    val logoX: Float = 0.5f,
    @SerialName("logo_y")
    val logoY: Float = 0.1f,
    @SerialName("logo_scale")
    val logoScale: Float = 1f,

    // Drinks area positioning (0-1 relative to canvas)
    @SerialName("drinks_x")
    val drinksX: Float = 0.1f,
    @SerialName("drinks_y")
    val drinksY: Float = 0.25f,
    @SerialName("drinks_width")
    val drinksWidth: Float = 0.8f,
    @SerialName("drinks_height")
    val drinksHeight: Float = 0.65f,
    @SerialName("drinks_name_font_size")
    val drinksNameFontSize: Float = 18f,
    @SerialName("drinks_price_font_size")
    val drinksPriceFontSize: Float = 16f,
    @SerialName("drinks_description_font_size")
    val drinksDescriptionFontSize: Float = 12f,
    @SerialName("drinks_name_color")
    val drinksNameColor: String = "#FFFFFF",
    @SerialName("drinks_price_color")
    val drinksPriceColor: String = "#FFFFFF",
    @SerialName("drinks_description_color")
    val drinksDescriptionColor: String = "#CCCCCC",
    @SerialName("drinks_font")
    val drinksFont: String = "default",


    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,

    @Transient
    var drinks: List<Drink> = emptyList()
)

@Serializable
data class TemplateRequest(
    val name: String,
    @SerialName("background_url")
    val backgroundUrl: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("background_scale")
    val backgroundScale: Float = 1f,
    @SerialName("background_offset_x")
    val backgroundOffsetX: Float = 0f,
    @SerialName("background_offset_y")
    val backgroundOffsetY: Float = 0f,
    @SerialName("logo_x")
    val logoX: Float = 0.5f,
    @SerialName("logo_y")
    val logoY: Float = 0.1f,
    @SerialName("logo_scale")
    val logoScale: Float = 1f,
    @SerialName("drinks_x")
    val drinksX: Float = 0.1f,
    @SerialName("drinks_y")
    val drinksY: Float = 0.25f,
    @SerialName("drinks_width")
    val drinksWidth: Float = 0.8f,
    @SerialName("drinks_height")
    val drinksHeight: Float = 0.65f,
    @SerialName("drinks_name_font_size")
    val drinksNameFontSize: Float = 18f,
    @SerialName("drinks_price_font_size")
    val drinksPriceFontSize: Float = 16f,
    @SerialName("drinks_description_font_size")
    val drinksDescriptionFontSize: Float = 12f,
    @SerialName("drinks_name_color")
    val drinksNameColor: String = "#FFFFFF",
    @SerialName("drinks_price_color")
    val drinksPriceColor: String = "#FFFFFF",
    @SerialName("drinks_description_color")
    val drinksDescriptionColor: String = "#CCCCCC",
    @SerialName("drinks_font")
    val drinksFont: String = "default"
)

@Serializable
data class TemplateDrink(
    val id: String = "",
    @SerialName("template_id")
    val templateId: String = "",
    @SerialName("drink_id")
    val drinkId: String = "",
    @SerialName("sort_order")
    val sortOrder: Int? = null,
    @SerialName("custom_price")
    val customPrice: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class TemplateDrinkRequest(
    @SerialName("template_id")
    val templateId: String,
    @SerialName("drink_id")
    val drinkId: String,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("custom_price")
    val customPrice: Double? = null
)
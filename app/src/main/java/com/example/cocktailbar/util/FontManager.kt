package com.example.cocktailbar.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.cocktailbar.R

object FontManager {

    data class FontItem(
        val id: String,
        val displayName: String,
        val fontResId: Int? = null  // null means system font
    )

    // Define available fonts
    val availableFonts = listOf(
        FontItem("default", "Default", null),
        FontItem("serif", "Serif", null),
        FontItem("sans_serif", "Sans Serif", null),
        FontItem("monospace", "Monospace", null),
        FontItem("roboto", "Roboto", R.font.roboto_regular),
        FontItem("open_sans", "Open Sans", R.font.open_sans_regular),
        FontItem("lato", "Lato", R.font.lato_regular),
        FontItem("montserrat", "Montserrat", R.font.montserrat_regular),
        FontItem("playfair", "Playfair Display", R.font.playfair_display_regular),
        FontItem("dancing_script", "Dancing Script", R.font.dancing_script_regular),
        FontItem("pacifico", "Pacifico", R.font.pacifico_regular)
    )

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getTypeface(context: Context, fontId: String): Typeface {
        // Return from cache if available
        typefaceCache[fontId]?.let { return it }

        val typeface = when (fontId) {
            "default" -> Typeface.DEFAULT
            "serif" -> Typeface.SERIF
            "sans_serif" -> Typeface.SANS_SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> {
                // Find custom font
                val fontItem = availableFonts.find { it.id == fontId }
                fontItem?.fontResId?.let { resId ->
                    try {
                        ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Typeface.DEFAULT
                    }
                } ?: Typeface.DEFAULT
            }
        }

        // Cache the typeface
        typefaceCache[fontId] = typeface
        return typeface
    }

    fun getFontDisplayName(fontId: String): String {
        return availableFonts.find { it.id == fontId }?.displayName ?: "Default"
    }

    fun getFontIndex(fontId: String): Int {
        return availableFonts.indexOfFirst { it.id == fontId }.takeIf { it >= 0 } ?: 0
    }
}
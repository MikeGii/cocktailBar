package com.example.cocktailbar.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.cocktailbar.R
import com.example.cocktailbar.TemplatePreviewView
import com.example.cocktailbar.util.dp

class DrinksSettingsDialog(
    private val context: Context,
    private val previewView: TemplatePreviewView
) {

    private val colors = listOf(
        "#FFFFFF", "#000000", "#FF6B35", "#FFA366",
        "#CCCCCC", "#888888", "#FFD700", "#90EE90"
    )

    private val fonts = listOf(
        FontOption("default", "Default", Typeface.DEFAULT),
        FontOption("serif", "Serif", Typeface.SERIF),
        FontOption("sans-serif", "Sans", Typeface.SANS_SERIF),
        FontOption("monospace", "Mono", Typeface.MONOSPACE)
    )

    data class FontOption(val id: String, val displayName: String, val typeface: Typeface)

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_drinks_settings)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupFontSizeControls(dialog)
        setupColorControls(dialog)
        setupFontSelection(dialog)

        dialog.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupFontSizeControls(dialog: Dialog) {
        // Name font size
        val seekbarNameFontSize = dialog.findViewById<SeekBar>(R.id.seekbarNameFontSize)
        val tvNameFontSizeValue = dialog.findViewById<TextView>(R.id.tvNameFontSizeValue)

        seekbarNameFontSize.progress = (previewView.drinksNameFontSize - 10).toInt()
        tvNameFontSizeValue.text = "${previewView.drinksNameFontSize.toInt()}sp"

        seekbarNameFontSize.setOnSeekBarChangeListener(createFontSizeListener(tvNameFontSizeValue) { fontSize ->
            previewView.drinksNameFontSize = fontSize
        })

        // Price font size
        val seekbarPriceFontSize = dialog.findViewById<SeekBar>(R.id.seekbarPriceFontSize)
        val tvPriceFontSizeValue = dialog.findViewById<TextView>(R.id.tvPriceFontSizeValue)

        seekbarPriceFontSize.progress = (previewView.drinksPriceFontSize - 10).toInt()
        tvPriceFontSizeValue.text = "${previewView.drinksPriceFontSize.toInt()}sp"

        seekbarPriceFontSize.setOnSeekBarChangeListener(createFontSizeListener(tvPriceFontSizeValue) { fontSize ->
            previewView.drinksPriceFontSize = fontSize
        })

        // Description font size
        val seekbarDescFontSize = dialog.findViewById<SeekBar>(R.id.seekbarDescFontSize)
        val tvDescFontSizeValue = dialog.findViewById<TextView>(R.id.tvDescFontSizeValue)

        seekbarDescFontSize.progress = (previewView.drinksDescriptionFontSize - 10).toInt()
        tvDescFontSizeValue.text = "${previewView.drinksDescriptionFontSize.toInt()}sp"

        seekbarDescFontSize.setOnSeekBarChangeListener(createFontSizeListener(tvDescFontSizeValue) { fontSize ->
            previewView.drinksDescriptionFontSize = fontSize
        })
    }

    private fun createFontSizeListener(
        valueTextView: TextView,
        onChanged: (Float) -> Unit
    ): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress + 10f
                valueTextView.text = "${fontSize.toInt()}sp"
                onChanged(fontSize)
                previewView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    private fun setupColorControls(dialog: Dialog) {
        val nameColorContainer = dialog.findViewById<LinearLayout>(R.id.nameColorContainer)
        val priceColorContainer = dialog.findViewById<LinearLayout>(R.id.priceColorContainer)
        val descColorContainer = dialog.findViewById<LinearLayout>(R.id.descColorContainer)

        setupColorButtons(nameColorContainer, previewView.drinksNameColor) { color ->
            previewView.drinksNameColor = color
            previewView.invalidate()
        }

        setupColorButtons(priceColorContainer, previewView.drinksPriceColor) { color ->
            previewView.drinksPriceColor = color
            previewView.invalidate()
        }

        setupColorButtons(descColorContainer, previewView.drinksDescriptionColor) { color ->
            previewView.drinksDescriptionColor = color
            previewView.invalidate()
        }
    }

    private fun setupColorButtons(
        container: LinearLayout,
        currentColor: Int,
        onColorSelected: (Int) -> Unit
    ) {
        container.removeAllViews()
        colors.forEach { colorHex ->
            val color = Color.parseColor(colorHex)
            val button = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp).apply {
                    marginEnd = 8.dp
                }
                background = createColorButtonBackground(color, color == currentColor)
                setOnClickListener {
                    onColorSelected(color)
                    updateColorSelection(container, color)
                }
            }
            container.addView(button)
        }
    }

    private fun updateColorSelection(container: LinearLayout, selectedColor: Int) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val childColor = Color.parseColor(colors[i])
            child.background = createColorButtonBackground(childColor, childColor == selectedColor)
        }
    }

    private fun createColorButtonBackground(color: Int, isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            setColor(color)
            setStroke(
                if (isSelected) 4 else 2,
                if (isSelected) context.getColor(R.color.orange_primary) else Color.GRAY
            )
        }
    }

    private fun setupFontSelection(dialog: Dialog) {
        val fontContainer = dialog.findViewById<LinearLayout>(R.id.fontContainer)
        fontContainer.removeAllViews()

        fonts.forEach { fontOption ->
            val button = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    40.dp
                ).apply {
                    marginEnd = 8.dp
                }
                text = fontOption.displayName
                typeface = fontOption.typeface
                textSize = 14f
                setPadding(16.dp, 8.dp, 16.dp, 8.dp)
                gravity = android.view.Gravity.CENTER

                val isSelected = previewView.drinksFont == fontOption.id
                background = createFontButtonBackground(isSelected)
                setTextColor(if (isSelected) Color.WHITE else context.getColor(R.color.text_primary))

                setOnClickListener {
                    previewView.drinksFont = fontOption.id
                    previewView.invalidate()
                    updateFontSelection(fontContainer, fontOption.id)
                }
            }
            fontContainer.addView(button)
        }
    }

    private fun updateFontSelection(container: LinearLayout, selectedFontId: String) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) as TextView
            val isSelected = fonts[i].id == selectedFontId
            child.background = createFontButtonBackground(isSelected)
            child.setTextColor(if (isSelected) Color.WHITE else context.getColor(R.color.text_primary))
        }
    }

    private fun createFontButtonBackground(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            if (isSelected) {
                setColor(context.getColor(R.color.orange_primary))
            } else {
                setColor(context.getColor(R.color.background_primary))
                setStroke(2, context.getColor(R.color.border_light))
            }
        }
    }
}
package com.example.cocktailbar.ui.templates

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailbar.TemplatePreviewView
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.databinding.ItemTemplateSelectBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TemplateSelectAdapter(
    private var templates: List<Template>,
    private val onTemplateClick: (Template) -> Unit,
    private val coroutineScope: CoroutineScope,
    private val imageLoader: suspend (String) -> Bitmap?
) : RecyclerView.Adapter<TemplateSelectAdapter.TemplateViewHolder>() {

    inner class TemplateViewHolder(private val binding: ItemTemplateSelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            val displayMetrics = binding.root.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            (binding.previewView.layoutParams as? ConstraintLayout.LayoutParams)?.let {
                it.dimensionRatio = "W,$screenWidth:$screenHeight"
                binding.previewView.layoutParams = it
            }
        }

        fun bind(template: Template) {
            binding.tvTemplateName.text = template.name

            binding.previewView.setBackgroundBitmap(null)
            binding.previewView.setLogoBitmap(null)

            binding.previewView.apply {
                backgroundScale = template.backgroundScale
                backgroundOffsetX = template.backgroundOffsetX
                backgroundOffsetY = template.backgroundOffsetY
                logoX = template.logoX
                logoY = template.logoY
                logoScale = template.logoScale
                drinksX = template.drinksX
                drinksY = template.drinksY
                drinksWidth = template.drinksWidth
                drinksHeight = template.drinksHeight
                drinksNameFontSize = template.drinksNameFontSize
                drinksPriceFontSize = template.drinksPriceFontSize
                drinksDescriptionFontSize = template.drinksDescriptionFontSize
                drinksNameColor = Color.parseColor(template.drinksNameColor)
                drinksPriceColor = Color.parseColor(template.drinksPriceColor)
                drinksDescriptionColor = Color.parseColor(template.drinksDescriptionColor)
                drinksFont = template.drinksFont
                drinks = template.drinks
                editMode = TemplatePreviewView.EditMode.NONE
                onTapInViewMode = null
            }

            template.backgroundUrl?.let { url ->
                coroutineScope.launch {
                    val bitmap = imageLoader(url)
                    binding.previewView.setBackgroundBitmap(bitmap)
                }
            }

            template.logoUrl?.let { url ->
                coroutineScope.launch {
                    val bitmap = imageLoader(url)
                    binding.previewView.setLogoBitmap(bitmap)
                }
            }

            binding.root.setOnClickListener { onTemplateClick(template) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateSelectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(templates[position])
    }

    override fun getItemCount(): Int = templates.size

    fun updateTemplates(newTemplates: List<Template>) {
        templates = newTemplates
        notifyDataSetChanged()
    }
}
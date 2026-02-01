package com.example.cocktailbar

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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

        fun bind(template: Template) {
            binding.tvTemplateName.text = template.name

            // Reset views
            binding.previewView.setBackgroundBitmap(null)
            binding.previewView.setLogoBitmap(null)

            // Apply template settings to preview view
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
                drinksFontSize = template.drinksFontSize
                drinksColumns = template.drinksColumns
                drinks = template.drinks
                editMode = TemplatePreviewView.EditMode.NONE

                // Disable touch in list view
                onTapInViewMode = null
            }

            // Load background
            template.backgroundUrl?.let { url ->
                coroutineScope.launch {
                    val bitmap = imageLoader(url)
                    binding.previewView.setBackgroundBitmap(bitmap)
                }
            }

            // Load logo
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
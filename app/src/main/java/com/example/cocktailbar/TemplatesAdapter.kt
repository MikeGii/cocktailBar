package com.example.cocktailbar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.databinding.ItemTemplateBinding

class TemplatesAdapter(
    private var templates: List<Template>,
    private val onEditClick: (Template) -> Unit,
    private val onDeleteClick: (Template) -> Unit,
    private val onPreviewClick: (Template) -> Unit
) : RecyclerView.Adapter<TemplatesAdapter.TemplateViewHolder>() {

    inner class TemplateViewHolder(private val binding: ItemTemplateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(template: Template) {
            binding.tvTemplateName.text = template.name

            // Load background thumbnail
            if (!template.backgroundUrl.isNullOrEmpty()) {
                binding.ivBackground.load(template.backgroundUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                    transformations(RoundedCornersTransformation(16f))
                }
            } else {
                binding.ivBackground.setImageResource(R.drawable.bg_image_placeholder)
            }

            // Load logo thumbnail
            if (!template.logoUrl.isNullOrEmpty()) {
                binding.ivLogo.load(template.logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                }
            } else {
                binding.ivLogo.setImageResource(R.drawable.bg_image_placeholder)
            }

            binding.root.setOnClickListener { onPreviewClick(template) }
            binding.btnEdit.setOnClickListener { onEditClick(template) }
            binding.btnDelete.setOnClickListener { onDeleteClick(template) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(
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
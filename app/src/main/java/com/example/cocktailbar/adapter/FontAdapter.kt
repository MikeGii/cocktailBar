package com.example.cocktailbar.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailbar.databinding.ItemFontBinding
import com.example.cocktailbar.util.FontManager

class FontAdapter(
    private val context: Context,
    private var selectedFontId: String,
    private val onFontSelected: (String) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private val fonts = FontManager.availableFonts

    inner class FontViewHolder(private val binding: ItemFontBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fontItem: FontManager.FontItem) {
            // Set font name with the actual font applied
            binding.tvFontPreview.text = fontItem.displayName
            binding.tvFontPreview.typeface = FontManager.getTypeface(context, fontItem.id)

            // Show check mark for selected font
            binding.ivSelected.visibility =
                if (fontItem.id == selectedFontId) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                val previousSelected = selectedFontId
                selectedFontId = fontItem.id

                // Update UI
                notifyItemChanged(fonts.indexOfFirst { it.id == previousSelected })
                notifyItemChanged(fonts.indexOfFirst { it.id == selectedFontId })

                onFontSelected(fontItem.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position])
    }

    override fun getItemCount(): Int = fonts.size
}
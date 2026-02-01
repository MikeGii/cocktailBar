package com.example.cocktailbar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.cocktailbar.databinding.ItemImagePickerBinding

class ImagePickerAdapter(
    private val onImageSelected: (String) -> Unit
) : RecyclerView.Adapter<ImagePickerAdapter.ImageViewHolder>() {

    private var images: List<String> = emptyList()

    inner class ImageViewHolder(private val binding: ItemImagePickerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            binding.ivImage.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
                transformations(RoundedCornersTransformation(16f))
            }

            binding.root.setOnClickListener {
                onImageSelected(imageUrl)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun updateImages(newImages: List<String>) {
        images = newImages
        notifyDataSetChanged()
    }
}
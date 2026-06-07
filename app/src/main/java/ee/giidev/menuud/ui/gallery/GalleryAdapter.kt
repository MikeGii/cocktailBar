package ee.giidev.menuud.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import ee.giidev.menuud.R
import ee.giidev.menuud.data.model.GalleryImage
import ee.giidev.menuud.databinding.ItemGalleryImageBinding

class GalleryAdapter(
    private var images: List<GalleryImage>,
    private val onImageClick: (GalleryImage) -> Unit,
    private val onDeleteClick: (GalleryImage) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemGalleryImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(image: GalleryImage) {
            binding.ivImage.load(image.url) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
                transformations(RoundedCornersTransformation(24f))
            }

            binding.tvFileName.text = image.name

            binding.root.setOnClickListener { onImageClick(image) }
            binding.btnDelete.setOnClickListener { onDeleteClick(image) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
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

    fun updateImages(newImages: List<GalleryImage>) {
        images = newImages
        notifyDataSetChanged()
    }
}

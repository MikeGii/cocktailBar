package com.example.cocktailbar.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cocktailbar.ui.common.ImagePickerAdapter
import com.example.cocktailbar.R
import com.example.cocktailbar.SupabaseClient
import com.example.cocktailbar.databinding.DialogImagePickerBinding
import com.example.cocktailbar.ui.gallery.GalleryViewModel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class ImagePickerDialog(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    fun show(bucket: String, onSelected: (String) -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogImagePickerBinding.inflate(
            android.view.LayoutInflater.from(context)
        )
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvTitle.text = context.getString(
            if (bucket == GalleryViewModel.BUCKET_BACKGROUNDS) R.string.select_background
            else R.string.select_logo
        )

        val imageAdapter = ImagePickerAdapter { imageUrl ->
            onSelected(imageUrl)
            dialog.dismiss()
        }

        dialogBinding.rvImages.layoutManager = GridLayoutManager(context, 2)
        dialogBinding.rvImages.adapter = imageAdapter

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        loadImages(bucket, dialogBinding, imageAdapter)

        dialog.show()
        setDialogSize(dialog, 0.90, 0.70)
    }

    private fun loadImages(
        bucket: String,
        binding: DialogImagePickerBinding,
        adapter: ImagePickerAdapter
    ) {
        lifecycleScope.launch {
            try {
                val storageBucket = SupabaseClient.client.storage.from(bucket)
                val files = storageBucket.list()

                val images = files
                    .filter { it.name.isNotEmpty() && !it.name.startsWith(".") }
                    .map { file -> storageBucket.publicUrl(file.name) }

                binding.progressBar.visibility = View.GONE
                if (images.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.updateImages(images)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun setDialogSize(dialog: Dialog, widthRatio: Double, heightRatio: Double) {
        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * widthRatio).toInt()
            val height = (displayMetrics.heightPixels * heightRatio).toInt()
            window.setLayout(width, height)
        }
    }
}
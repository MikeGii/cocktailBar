package com.example.cocktailbar.dialog

import android.app.Dialog
import android.content.Context
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailbar.R
import com.example.cocktailbar.adapter.FontAdapter
import com.example.cocktailbar.databinding.DialogFontPickerBinding

class FontPickerDialog(
    private val context: Context
) {
    fun show(currentFontId: String, onFontSelected: (String) -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogFontPickerBinding.inflate(
            android.view.LayoutInflater.from(context)
        )
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val adapter = FontAdapter(context, currentFontId) { fontId ->
            onFontSelected(fontId)
            dialog.dismiss()
        }

        binding.rvFonts.layoutManager = LinearLayoutManager(context)
        binding.rvFonts.adapter = adapter

        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            val height = (displayMetrics.heightPixels * 0.60).toInt()
            window.setLayout(width, height)
        }
    }
}
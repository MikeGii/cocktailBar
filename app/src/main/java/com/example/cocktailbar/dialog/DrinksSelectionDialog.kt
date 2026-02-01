package com.example.cocktailbar.dialog

import android.app.Dialog
import android.content.Context
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailbar.Drink
import com.example.cocktailbar.R
import com.example.cocktailbar.SelectableDrinksAdapter
import com.example.cocktailbar.databinding.DialogImagePickerBinding

class DrinksSelectionDialog(
    private val context: Context
) {

    fun show(
        drinks: List<Drink>,
        selectedIds: MutableSet<String>,
        onConfirmed: (Set<String>) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogImagePickerBinding.inflate(
            android.view.LayoutInflater.from(context)
        )
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvTitle.text = context.getString(R.string.select_drinks)
        dialogBinding.progressBar.visibility = android.view.View.GONE

        val tempSelectedIds = selectedIds.toMutableSet()

        val drinksAdapter = SelectableDrinksAdapter(
            drinks = drinks,
            selectedIds = tempSelectedIds,
            onSelectionChanged = { drinkId, isSelected ->
                if (isSelected) tempSelectedIds.add(drinkId)
                else tempSelectedIds.remove(drinkId)
            }
        )

        dialogBinding.rvImages.layoutManager = LinearLayoutManager(context)
        dialogBinding.rvImages.adapter = drinksAdapter

        dialogBinding.btnCancel.text = context.getString(R.string.ok)
        dialogBinding.btnCancel.setOnClickListener {
            onConfirmed(tempSelectedIds)
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            val height = (displayMetrics.heightPixels * 0.70).toInt()
            window.setLayout(width, height)
        }
    }
}
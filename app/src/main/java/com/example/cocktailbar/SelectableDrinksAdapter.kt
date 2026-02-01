package com.example.cocktailbar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.databinding.ItemDrinkSelectableBinding

class SelectableDrinksAdapter(
    private var drinks: List<Drink>,
    private var selectedIds: MutableSet<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<SelectableDrinksAdapter.DrinkViewHolder>() {

    inner class DrinkViewHolder(private val binding: ItemDrinkSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(drink: Drink) {
            binding.tvDrinkName.text = drink.name
            binding.tvPrice.text = drink.getDisplayPrice()
            binding.checkbox.isChecked = selectedIds.contains(drink.id)

            binding.root.setOnClickListener {
                binding.checkbox.isChecked = !binding.checkbox.isChecked
            }

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                onSelectionChanged(drink.id, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkViewHolder {
        val binding = ItemDrinkSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DrinkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DrinkViewHolder, position: Int) {
        holder.bind(drinks[position])
    }

    override fun getItemCount(): Int = drinks.size

    fun updateDrinks(newDrinks: List<Drink>, newSelectedIds: MutableSet<String>) {
        drinks = newDrinks
        selectedIds = newSelectedIds
        notifyDataSetChanged()
    }
}
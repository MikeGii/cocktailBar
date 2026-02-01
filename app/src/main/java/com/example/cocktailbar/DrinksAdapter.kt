package com.example.cocktailbar

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailbar.databinding.ItemDrinkBinding
import java.util.Locale

class DrinksAdapter(
    private var drinks: List<Drink>,
    private val onEditClick: (Drink) -> Unit,
    private val onDeleteClick: (Drink) -> Unit
) : RecyclerView.Adapter<DrinksAdapter.DrinkViewHolder>() {

    private var filteredDrinks: List<Drink> = drinks

    inner class DrinkViewHolder(private val binding: ItemDrinkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(drink: Drink) {
            binding.tvDrinkName.text = drink.name
            binding.tvPrice.text = String.format(Locale.getDefault(), "%.2f €", drink.price)
            binding.tvDescription.text = drink.description ?: ""

            // Hide description if empty
            binding.tvDescription.visibility = if (drink.description.isNullOrEmpty()) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }

            binding.btnEdit.setOnClickListener { onEditClick(drink) }
            binding.btnDelete.setOnClickListener { onDeleteClick(drink) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkViewHolder {
        val binding = ItemDrinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DrinkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DrinkViewHolder, position: Int) {
        holder.bind(filteredDrinks[position])
    }

    override fun getItemCount(): Int = filteredDrinks.size

    fun updateDrinks(newDrinks: List<Drink>) {
        drinks = newDrinks
        filteredDrinks = newDrinks
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredDrinks = if (query.isEmpty()) {
            drinks
        } else {
            drinks.filter { drink ->
                drink.name.contains(query, ignoreCase = true) ||
                        (drink.description?.contains(query, ignoreCase = true) == true)
            }
        }
        notifyDataSetChanged()
    }
}
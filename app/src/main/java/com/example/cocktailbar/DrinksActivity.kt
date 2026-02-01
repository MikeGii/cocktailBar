package com.example.cocktailbar

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailbar.databinding.ActivityDrinksBinding
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class DrinksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrinksBinding
    private lateinit var adapter: DrinksAdapter
    private var drinks: MutableList<Drink> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDrinksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSearch()
        setupButtons()
        loadDrinks()
    }

    private fun setupRecyclerView() {
        adapter = DrinksAdapter(
            drinks = drinks,
            onEditClick = { drink -> showDrinkDialog(drink) },
            onDeleteClick = { drink -> showDeleteConfirmation(drink) }
        )
        binding.rvDrinks.layoutManager = LinearLayoutManager(this)
        binding.rvDrinks.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
                updateEmptyState()
            }
        })
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showDrinkDialog(null) }
    }

    private fun loadDrinks() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Load drinks
                val drinksResult = SupabaseClient.client
                    .from("drinks")
                    .select()
                    .decodeList<Drink>()

                // Load all variants
                val variantsResult = SupabaseClient.client
                    .from("drink_variants")
                    .select()
                    .decodeList<DrinkVariant>()

                // Group variants by drink_id
                val variantsByDrink = variantsResult.groupBy { it.drinkId }

                // Assign variants to drinks
                drinks.clear()
                drinks.addAll(drinksResult.map { drink ->
                    drink.copy().apply {
                        variants = variantsByDrink[drink.id]?.sortedBy { it.sortOrder ?: 0 } ?: emptyList()
                    }
                })

                runOnUiThread {
                    adapter.updateDrinks(drinks)
                    showLoading(false)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@DrinksActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDrinkDialog(drink: Drink?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_drink)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = dialog.findViewById<TextInputEditText>(R.id.etDrinkName)
        val etDescription = dialog.findViewById<TextInputEditText>(R.id.etDescription)
        val variantsContainer = dialog.findViewById<LinearLayout>(R.id.variantsContainer)
        val btnAddVariant = dialog.findViewById<TextView>(R.id.btnAddVariant)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        // Track variant views
        val variantViews = mutableListOf<View>()

        fun addVariantRow(variant: DrinkVariant? = null) {
            val variantView = LayoutInflater.from(this)
                .inflate(R.layout.item_variant_edit, variantsContainer, false)

            val etSizeName = variantView.findViewById<EditText>(R.id.etSizeName)
            val etPrice = variantView.findViewById<EditText>(R.id.etPrice)
            val btnRemove = variantView.findViewById<View>(R.id.btnRemove)

            variant?.let {
                etSizeName.setText(it.sizeName)
                etPrice.setText(String.format("%.2f", it.price))
            }

            btnRemove.setOnClickListener {
                variantsContainer.removeView(variantView)
                variantViews.remove(variantView)
            }

            variantsContainer.addView(variantView)
            variantViews.add(variantView)
        }

        // Set title and prefill data if editing
        if (drink != null) {
            tvTitle.text = getString(R.string.edit_drink)
            etName.setText(drink.name)
            etDescription.setText(drink.description ?: "")

            // Add existing variants
            drink.variants.forEach { addVariantRow(it) }
        } else {
            tvTitle.text = getString(R.string.add_drink)
            // Add one empty variant by default
            addVariantRow()
        }

        btnAddVariant.setOnClickListener { addVariantRow() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty()) {
                tvError.text = getString(R.string.error)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Collect variants
            val variants = mutableListOf<DrinkVariant>()
            for ((index, view) in variantViews.withIndex()) {
                val sizeName = view.findViewById<EditText>(R.id.etSizeName).text.toString().trim()
                val priceText = view.findViewById<EditText>(R.id.etPrice).text.toString().trim()

                if (sizeName.isNotEmpty() && priceText.isNotEmpty()) {
                    val price = priceText.replace(",", ".").toDoubleOrNull()
                    if (price != null) {
                        variants.add(DrinkVariant(
                            sizeName = sizeName,
                            price = price,
                            sortOrder = index
                        ))
                    }
                }
            }

            if (variants.isEmpty()) {
                tvError.text = getString(R.string.at_least_one_price)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            tvError.visibility = View.GONE

            if (drink != null) {
                updateDrink(drink.id, name, description, variants, dialog)
            } else {
                addDrink(name, description, variants, dialog)
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        val window = dialog.window
        if (window != null) {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun addDrink(name: String, description: String, variants: List<DrinkVariant>, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                // Insert drink first
                val newDrink = DrinkRequest(
                    name = name,
                    description = description.ifEmpty { null }
                )

                val insertedDrinks = SupabaseClient.client
                    .from("drinks")
                    .insert(newDrink) { select() }
                    .decodeList<Drink>()

                val drinkId = insertedDrinks.firstOrNull()?.id
                    ?: throw Exception("Failed to get drink ID")

                // Insert variants
                val variantsToInsert = variants.map { variant ->
                    DrinkVariantRequest(
                        drinkId = drinkId,
                        sizeName = variant.sizeName,
                        price = variant.price,
                        sortOrder = variant.sortOrder ?: 0
                    )
                }

                SupabaseClient.client
                    .from("drink_variants")
                    .insert(variantsToInsert)

                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this@DrinksActivity, R.string.save, Toast.LENGTH_SHORT).show()
                    loadDrinks()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    dialog.findViewById<Button>(R.id.btnSave).isEnabled = true
                    val tvError = dialog.findViewById<TextView>(R.id.tvError)
                    tvError.visibility = View.VISIBLE
                    tvError.text = e.message ?: getString(R.string.error)
                }
            }
        }
    }

    private fun updateDrink(id: String, name: String, description: String, variants: List<DrinkVariant>, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                // Update drink
                val updatedDrink = DrinkRequest(
                    name = name,
                    description = description.ifEmpty { null }
                )

                SupabaseClient.client
                    .from("drinks")
                    .update(updatedDrink) {
                        filter { eq("id", id) }
                    }

                // Delete old variants
                SupabaseClient.client
                    .from("drink_variants")
                    .delete {
                        filter { eq("drink_id", id) }
                    }

                // Insert new variants
                val variantsToInsert = variants.map { variant ->
                    DrinkVariantRequest(
                        drinkId = id,
                        sizeName = variant.sizeName,
                        price = variant.price,
                        sortOrder = variant.sortOrder ?: 0
                    )
                }

                SupabaseClient.client
                    .from("drink_variants")
                    .insert(variantsToInsert)

                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this@DrinksActivity, R.string.save, Toast.LENGTH_SHORT).show()
                    loadDrinks()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    dialog.findViewById<Button>(R.id.btnSave).isEnabled = true
                    val tvError = dialog.findViewById<TextView>(R.id.tvError)
                    tvError.visibility = View.VISIBLE
                    tvError.text = e.message ?: getString(R.string.error)
                }
            }
        }
    }

    private fun showDeleteConfirmation(drink: Drink) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_drink)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ -> deleteDrink(drink) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun deleteDrink(drink: Drink) {
        lifecycleScope.launch {
            try {
                // Variants will be deleted automatically due to ON DELETE CASCADE
                SupabaseClient.client
                    .from("drinks")
                    .delete {
                        filter { eq("id", drink.id) }
                    }

                runOnUiThread {
                    Toast.makeText(this@DrinksActivity, R.string.delete, Toast.LENGTH_SHORT).show()
                    loadDrinks()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@DrinksActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvDrinks.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }
}
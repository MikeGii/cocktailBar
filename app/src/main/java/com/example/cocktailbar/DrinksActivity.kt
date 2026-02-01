package com.example.cocktailbar.ui.drinks

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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailbar.R
import com.example.cocktailbar.data.model.Drink
import com.example.cocktailbar.data.model.DrinkVariant
import com.example.cocktailbar.databinding.ActivityDrinksBinding
import com.example.cocktailbar.ui.common.UiState
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DrinksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrinksBinding
    private lateinit var adapter: DrinksAdapter

    private val viewModel: DrinksViewModel by viewModels { DrinksViewModel.Factory() }

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
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = DrinksAdapter(
            drinks = emptyList(),
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
                viewModel.setSearchQuery(s.toString())
            }
        })
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showDrinkDialog(null) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> showLoading(true)
                            is UiState.Success -> {
                                showLoading(false)
                                adapter.updateDrinks(state.data)
                                updateEmptyState(state.data.isEmpty())
                            }
                            is UiState.Empty -> {
                                showLoading(false)
                                adapter.updateDrinks(emptyList())
                                updateEmptyState(true)
                            }
                            is UiState.Error -> {
                                showLoading(false)
                                Toast.makeText(this@DrinksActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.toastMessage.collectLatest { messageResId ->
                        Toast.makeText(this@DrinksActivity, messageResId, Toast.LENGTH_SHORT).show()
                    }
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

        if (drink != null) {
            tvTitle.text = getString(R.string.edit_drink)
            etName.setText(drink.name)
            etDescription.setText(drink.description ?: "")
            drink.variants.forEach { addVariantRow(it) }
        } else {
            tvTitle.text = getString(R.string.add_drink)
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

            val variants = mutableListOf<DrinkVariant>()
            for ((index, view) in variantViews.withIndex()) {
                val sizeName = view.findViewById<EditText>(R.id.etSizeName).text.toString().trim()
                val priceText = view.findViewById<EditText>(R.id.etPrice).text.toString().trim()

                if (sizeName.isNotEmpty() && priceText.isNotEmpty()) {
                    val price = priceText.replace(",", ".").toDoubleOrNull()
                    if (price != null) {
                        variants.add(DrinkVariant(sizeName = sizeName, price = price, sortOrder = index))
                    }
                }
            }

            if (variants.isEmpty()) {
                tvError.text = getString(R.string.at_least_one_price)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            dialog.dismiss()

            if (drink != null) {
                viewModel.updateDrink(drink.id, name, description, variants)
            } else {
                viewModel.addDrink(name, description, variants)
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        dialog.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun showDeleteConfirmation(drink: Drink) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_drink)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteDrink(drink) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvDrinks.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }
}
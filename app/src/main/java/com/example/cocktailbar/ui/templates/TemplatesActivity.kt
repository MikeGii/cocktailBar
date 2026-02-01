package com.example.cocktailbar.ui.templates

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.databinding.ActivityTemplatesBinding
import com.example.cocktailbar.ui.common.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TemplatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplatesBinding
    private lateinit var adapter: TemplatesAdapter

    private val viewModel: TemplatesViewModel by viewModels { TemplatesViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTemplatesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTemplates()
    }

    private fun setupRecyclerView() {
        adapter = TemplatesAdapter(
            templates = emptyList(),
            onEditClick = { template -> openTemplateEditor(template) },
            onDeleteClick = { template -> showDeleteConfirmation(template) },
            onPreviewClick = { template -> openTemplatePreview(template) }
        )
        binding.rvTemplates.layoutManager = LinearLayoutManager(this)
        binding.rvTemplates.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { openTemplateEditor(null) }
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
                                adapter.updateTemplates(state.data)
                                updateEmptyState(false)
                            }
                            is UiState.Empty -> {
                                showLoading(false)
                                adapter.updateTemplates(emptyList())
                                updateEmptyState(true)
                            }
                            is UiState.Error -> {
                                showLoading(false)
                                Toast.makeText(this@TemplatesActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.toastMessage.collectLatest { messageResId ->
                        Toast.makeText(this@TemplatesActivity, messageResId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openTemplateEditor(template: Template?) {
        val intent = Intent(this, TemplateVisualEditorActivity::class.java)
        template?.let {
            intent.putExtra(TemplateVisualEditorActivity.EXTRA_TEMPLATE_ID, it.id)
        }
        startActivity(intent)
    }

    private fun openTemplatePreview(template: Template) {
        val intent = Intent(this, TemplateDisplayActivity::class.java)
        intent.putExtra(TemplateDisplayActivity.EXTRA_TEMPLATE_ID, template.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(template: Template) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_template)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteTemplate(template) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvTemplates.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }
}
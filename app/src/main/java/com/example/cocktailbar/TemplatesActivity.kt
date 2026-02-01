package com.example.cocktailbar

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailbar.data.model.Template
import com.example.cocktailbar.databinding.ActivityTemplatesBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class TemplatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplatesBinding
    private lateinit var adapter: TemplatesAdapter
    private var templates: MutableList<Template> = mutableListOf()

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
    }

    override fun onResume() {
        super.onResume()
        loadTemplates()
    }

    private fun setupRecyclerView() {
        adapter = TemplatesAdapter(
            templates = templates,
            onEditClick = { template -> openTemplateEditor(template) },
            onDeleteClick = { template -> showDeleteConfirmation(template) },
            onPreviewClick = { template -> openTemplatePreview(template) }
        )
        binding.rvTemplates.layoutManager = LinearLayoutManager(this)
        binding.rvTemplates.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.fabAdd.setOnClickListener {
            openTemplateEditor(null)
        }
    }

    private fun loadTemplates() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = SupabaseClient.client
                    .from("templates")
                    .select()
                    .decodeList<Template>()

                templates.clear()
                templates.addAll(result)

                runOnUiThread {
                    adapter.updateTemplates(templates)
                    showLoading(false)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    updateEmptyState()
                    Toast.makeText(this@TemplatesActivity, R.string.error, Toast.LENGTH_SHORT).show()
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
        // TODO: Implement preview activity
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(template: Template) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_template)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ -> deleteTemplate(template) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun deleteTemplate(template: Template) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                SupabaseClient.client
                    .from("templates")
                    .delete {
                        filter { eq("id", template.id) }
                    }

                runOnUiThread {
                    Toast.makeText(this@TemplatesActivity, R.string.delete, Toast.LENGTH_SHORT).show()
                    loadTemplates()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@TemplatesActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvTemplates.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmpty.visibility = if (isEmpty && binding.progressBar.visibility != View.VISIBLE)
            View.VISIBLE else View.GONE
    }
}
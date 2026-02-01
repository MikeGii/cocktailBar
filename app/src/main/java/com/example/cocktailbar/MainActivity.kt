package com.example.cocktailbar

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cocktailbar.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAdminLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnSelectTemplate.setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.btnSyncData.setOnClickListener {
            syncData()
        }

        binding.btnAdminLogin.setOnClickListener {
            if (isAdminLoggedIn) {
                openAdminPanel()
            } else {
                showLoginDialog()
            }
        }
    }

    private fun syncData() {
        Toast.makeText(this, R.string.syncing, Toast.LENGTH_SHORT).show()
        // TODO: Implement sync logic
    }

    private fun showLoginDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_login)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etUsername = dialog.findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = dialog.findViewById<TextInputEditText>(R.id.etPassword)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val btnLogin = dialog.findViewById<Button>(R.id.btnLogin)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                tvError.text = getString(R.string.error)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            tvError.visibility = View.GONE

            checkAdminCredentials(username, password) { success ->
                runOnUiThread {
                    if (success) {
                        isAdminLoggedIn = true
                        dialog.dismiss()
                        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                        openAdminPanel()
                    } else {
                        btnLogin.isEnabled = true
                        tvError.text = getString(R.string.login_failed)
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // Set dialog width to 90% of screen width
        val window = dialog.window
        if (window != null) {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun checkAdminCredentials(username: String, password: String, callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.client
                    .from("admin")
                    .select {
                        filter {
                            eq("username", username)
                            eq("password", password)
                        }
                    }
                    .decodeList<Admin>()

                callback(result.isNotEmpty())
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }

    private fun openAdminPanel() {
        Toast.makeText(this, "Admin Panel - ${getString(R.string.coming_soon)}", Toast.LENGTH_SHORT).show()
        // TODO: Open Admin Panel Activity
    }
}
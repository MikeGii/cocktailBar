package com.example.cocktailbar

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAdminLoggedIn = false
    private var isConnected = false

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

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

        setupNetworkMonitoring()
        setupButtons()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkCallback()
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network is available, verify Supabase connection
                checkSupabaseConnection()
            }

            override fun onLost(network: Network) {
                // Network lost
                isConnected = false
                runOnUiThread {
                    updateConnectionIndicator()
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                // Network capabilities changed, verify connection
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    checkSupabaseConnection()
                } else {
                    isConnected = false
                    runOnUiThread {
                        updateConnectionIndicator()
                    }
                }
            }
        }

        // Check initial connection state
        checkInitialConnection()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkInitialConnection() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (hasInternet) {
            checkSupabaseConnection()
        } else {
            isConnected = false
            updateConnectionIndicator()
        }
    }

    private fun checkSupabaseConnection() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("admin").select {
                    limit(1)
                }
                isConnected = true
            } catch (e: Exception) {
                isConnected = false
            }
            runOnUiThread {
                updateConnectionIndicator()
            }
        }
    }

    private fun setupButtons() {
        // Worker buttons
        binding.btnSelectTemplate.setOnClickListener {
            startActivity(Intent(this, TemplateSelectActivity::class.java))
        }

        binding.btnSyncData.setOnClickListener {
            syncData()
        }

        binding.btnAdminLogin.setOnClickListener {
            if (!isAdminLoggedIn) {
                showLoginDialog()
            }
        }

        // Admin buttons
        binding.btnDrinks.setOnClickListener {
            startActivity(android.content.Intent(this, DrinksActivity::class.java))
        }

        binding.btnTemplates.setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }

        binding.btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun updateUI() {
        if (isAdminLoggedIn) {
            binding.workerContainer.visibility = View.GONE
            binding.adminContainer.visibility = View.VISIBLE
            binding.btnAdminLogin.visibility = View.GONE
        } else {
            binding.workerContainer.visibility = View.VISIBLE
            binding.adminContainer.visibility = View.GONE
            binding.btnAdminLogin.visibility = View.VISIBLE
        }
    }

    private fun updateConnectionIndicator() {
        if (isConnected) {
            binding.connectionIndicator.setImageResource(R.drawable.ic_connected)
        } else {
            binding.connectionIndicator.setImageResource(R.drawable.ic_disconnected)
        }
    }

    private fun syncData() {
        if (!isConnected) {
            Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, R.string.syncing, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("admin").select {
                    limit(1)
                }
                isConnected = true

                // TODO: Implement full sync logic

                runOnUiThread {
                    updateConnectionIndicator()
                    Toast.makeText(this@MainActivity, R.string.sync_success, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isConnected = false
                runOnUiThread {
                    updateConnectionIndicator()
                    Toast.makeText(this@MainActivity, R.string.sync_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoginDialog() {
        if (!isConnected) {
            Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show()
            return
        }

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
                        updateUI()
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

    private fun logout() {
        isAdminLoggedIn = false
        updateUI()
        Toast.makeText(this, R.string.logout, Toast.LENGTH_SHORT).show()
    }
}
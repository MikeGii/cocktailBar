package com.example.cocktailbar

import android.app.Dialog
import android.content.Context
import android.content.Intent
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cocktailbar.databinding.ActivityMainBinding
import com.example.cocktailbar.ui.drinks.DrinksActivity
import com.example.cocktailbar.ui.gallery.GalleryActivity
import com.example.cocktailbar.ui.main.MainViewModel
import com.example.cocktailbar.ui.templates.TemplateSelectActivity
import com.example.cocktailbar.ui.templates.TemplatesActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory(this) }

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
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkCallback()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isConnected.collect { connected ->
                        updateConnectionIndicator(connected)
                    }
                }

                launch {
                    viewModel.isAdminLoggedIn.collect { loggedIn ->
                        updateUI(loggedIn)
                    }
                }

                launch {
                    viewModel.toastMessage.collectLatest { messageResId ->
                        Toast.makeText(this@MainActivity, messageResId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModel.checkConnection()
            }

            override fun onLost(network: Network) {
                viewModel.setConnected(false)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    viewModel.checkConnection()
                } else {
                    viewModel.setConnected(false)
                }
            }
        }

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
            viewModel.checkConnection()
        } else {
            viewModel.setConnected(false)
        }
    }

    private fun setupButtons() {
        // Worker buttons
        binding.btnSelectTemplate.setOnClickListener {
            startActivity(Intent(this, TemplateSelectActivity::class.java))
        }

        binding.btnSyncData.setOnClickListener {
            viewModel.syncData()
        }

        binding.btnAdminLogin.setOnClickListener {
            if (!viewModel.isAdminLoggedIn.value) {
                showLoginDialog()
            }
        }

        // Admin buttons
        binding.btnDrinks.setOnClickListener {
            startActivity(Intent(this, DrinksActivity::class.java))
        }

        binding.btnTemplates.setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }

        binding.btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun updateUI(isAdminLoggedIn: Boolean) {
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

    private fun updateConnectionIndicator(isConnected: Boolean) {
        binding.connectionIndicator.setImageResource(
            if (isConnected) R.drawable.ic_connected else R.drawable.ic_disconnected
        )
    }

    private fun showLoginDialog() {
        if (!viewModel.isConnected.value) {
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

        // Observe login result
        lifecycleScope.launch {
            viewModel.loginResult.collect { success ->
                if (success) {
                    dialog.dismiss()
                } else {
                    btnLogin.isEnabled = true
                    tvError.text = getString(R.string.login_failed)
                    tvError.visibility = View.VISIBLE
                }
            }
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                tvError.text = getString(R.string.field_required)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            tvError.visibility = View.GONE
            viewModel.login(username, password)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        dialog.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }
}
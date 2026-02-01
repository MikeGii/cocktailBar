package com.example.cocktailbar.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cocktailbar.MainActivity
import com.example.cocktailbar.R
import com.example.cocktailbar.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initializeApp()
    }

    private fun initializeApp() {
        lifecycleScope.launch {
            val minSplashTime = 1500L
            val startTime = System.currentTimeMillis()

            try {
                SupabaseClient.client.from("admin").select { limit(1) }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < minSplashTime) {
                delay(minSplashTime - elapsed)
            }

            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
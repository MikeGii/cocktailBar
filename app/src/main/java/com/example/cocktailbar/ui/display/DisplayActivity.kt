package com.example.cocktailbar.ui.display

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.cocktailbar.R
import com.example.cocktailbar.databinding.ActivityDisplayBinding

/**
 * Shows a single image full-screen and can "lock" the screen for unattended
 * display at an event.
 *
 * Locking uses Android Lock Task Mode (screen pinning) via [startLockTask], which
 * blocks the Home and Recents buttons and the notification shade. On a normal
 * tablet the user can still leave by holding Back + Recents together; if the app
 * is later provisioned as device owner the exact same call becomes a full kiosk
 * with no escape. We also hide the system bars (immersive) and keep the screen on.
 *
 * Unlocking is intentionally hidden: triple-tap the image to reveal the unlock
 * button for a few seconds, then tap it.
 */
class DisplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayBinding

    private var isLocked = false

    // Triple-tap detection to reveal the unlock button while locked.
    private var tapCount = 0
    private var lastTapTime = 0L
    private val tapTimeoutMs = 600L
    private val handler = Handler(Looper.getMainLooper())
    private val hideUnlockRunnable = Runnable {
        if (isLocked) binding.btnUnlock.visibility = View.GONE
    }

    // Swallows the Back button only while locked.
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Locked: ignore Back. Hint the user how to unlock.
            Toast.makeText(this@DisplayActivity, R.string.screen_locked, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        if (imageUrl.isNullOrEmpty()) {
            finish()
            return
        }

        binding.ivImage.load(imageUrl) {
            crossfade(true)
            error(R.drawable.bg_image_placeholder)
        }

        onBackPressedDispatcher.addCallback(this, backCallback)
        setupViews()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { if (!isLocked) finish() }
        binding.btnLock.setOnClickListener { lockScreen() }
        binding.btnUnlock.setOnClickListener { unlockScreen() }
        binding.ivImage.setOnClickListener { if (isLocked) detectTripleTap() }
    }

    // region Lock / unlock

    private fun lockScreen() {
        isLocked = true
        backCallback.isEnabled = true

        binding.topBar.visibility = View.GONE
        binding.btnLock.visibility = View.GONE
        binding.btnUnlock.visibility = View.GONE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()
        startLockTaskSafely()

        Toast.makeText(this, R.string.screen_locked, Toast.LENGTH_LONG).show()
    }

    private fun unlockScreen() {
        isLocked = false
        backCallback.isEnabled = false

        stopLockTaskSafely()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        exitImmersive()

        binding.topBar.visibility = View.VISIBLE
        binding.btnLock.visibility = View.VISIBLE
        binding.btnUnlock.visibility = View.GONE
        handler.removeCallbacks(hideUnlockRunnable)
    }

    private fun startLockTaskSafely() {
        try {
            if (lockTaskState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
            }
        } catch (e: Exception) {
            // Screen pinning can be disabled in system settings; immersive mode
            // still hides the system UI as a softer fallback.
            e.printStackTrace()
        }
    }

    private fun stopLockTaskSafely() {
        try {
            if (lockTaskState() != ActivityManager.LOCK_TASK_MODE_NONE) {
                stopLockTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun lockTaskState(): Int {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.lockTaskModeState
        } else {
            ActivityManager.LOCK_TASK_MODE_NONE
        }
    }

    // endregion

    // region Triple-tap to reveal unlock

    private fun detectTripleTap() {
        val now = System.currentTimeMillis()
        tapCount = if (now - lastTapTime < tapTimeoutMs) tapCount + 1 else 1
        lastTapTime = now

        if (tapCount >= 3) {
            tapCount = 0
            showUnlockButton()
        }
    }

    private fun showUnlockButton() {
        binding.btnUnlock.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(0.85f).setDuration(200).start()
        }
        handler.removeCallbacks(hideUnlockRunnable)
        handler.postDelayed(hideUnlockRunnable, 3000)
    }

    // endregion

    // region Immersive system UI

    private fun enterImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    private fun exitImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // If the user swipes the system bars in while locked, re-hide them.
        if (hasFocus && isLocked) enterImmersive()
    }

    // endregion

    override fun onDestroy() {
        super.onDestroy()
        if (isLocked) stopLockTaskSafely()
        handler.removeCallbacks(hideUnlockRunnable)
    }

    companion object {
        const val EXTRA_IMAGE_URL = "image_url"
    }
}

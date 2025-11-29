package com.example.travelbuddy

import android.content.SharedPreferences
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

open class BaseActivity : AppCompatActivity() {
    protected lateinit var sharedPreferences: SharedPreferences
    private var darkModeButton: ImageButton? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        sharedPreferences = getSharedPreferences("TravelBuddyPrefs", MODE_PRIVATE)
        applyTheme()
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        window.decorView.post { addDarkModeButton() }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        window.decorView.post { addDarkModeButton() }
    }

    private fun addDarkModeButton() {
        val rootView = window.decorView.rootView as? ViewGroup ?: return
        if (findViewById<ImageButton>(R.id.darkModeButtonGlobal) != null) return
        val contentView = rootView.findViewById<ViewGroup>(android.R.id.content) ?: return

        val iconSize = resources.getDimensionPixelSize(R.dimen.icon_size_medium)
        val margin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val statusBarHeight = getStatusBarHeight()
        val extraPadding = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val calculatedTopMargin = statusBarHeight + extraPadding
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_small)

        darkModeButton = ImageButton(this)
        darkModeButton?.id = R.id.darkModeButtonGlobal
        
        val layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.topMargin = calculatedTopMargin
        layoutParams.marginEnd = margin
        darkModeButton?.layoutParams = layoutParams

        darkModeButton?.setBackgroundResource(android.R.drawable.screen_background_light_transparent)
        darkModeButton?.setPadding(padding, padding, padding, padding)
        darkModeButton?.contentDescription = "Tryb ciemny"
        darkModeButton?.elevation = 8f
        darkModeButton?.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        darkModeButton?.setOnClickListener { toggleDarkMode() }

        contentView.addView(darkModeButton)
        updateDarkModeButtonIcon()
    }

    protected fun applyTheme() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    protected fun toggleDarkMode() {
        val newDarkMode = !sharedPreferences.getBoolean("dark_mode", false)
        sharedPreferences.edit().putBoolean("dark_mode", newDarkMode).apply()
        AppCompatDelegate.setDefaultNightMode(if (newDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        Toast.makeText(this, if (newDarkMode) "Tryb ciemny włączony" else "Tryb jasny włączony", Toast.LENGTH_SHORT).show()
        updateDarkModeButtonIcon()
    }

    protected fun updateDarkModeButtonIcon() {
        val button = darkModeButton ?: findViewById<ImageButton>(R.id.darkModeButtonGlobal)
        button?.setImageResource(if (sharedPreferences.getBoolean("dark_mode", false)) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}

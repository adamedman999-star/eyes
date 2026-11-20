package com.tim.eyecomfort

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var toggle: Switch
    private lateinit var permButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.setOnApplyWindowInsetsListener { v, insets ->
            val pad = (24 * resources.displayMetrics.density).toInt()
            v.setPadding(pad, pad + insets.systemWindowInsetTop, pad, pad + insets.systemWindowInsetBottom)
            insets
        }

        val title = TextView(this).apply { text = "Eye Comfort"; textSize = 28f }
        root.addView(title)

        permButton = Button(this).apply {
            text = "Разрешить показ поверх других окон"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
        root.addView(permButton)

        toggle = Switch(this).apply {
            text = "Фильтр включён"
            textSize = 18f
            setOnCheckedChangeListener { _, on -> setFilter(on) }
        }
        root.addView(toggle, lp(top = 24))

        addSlider(root, "Тёплый оттенок", Prefs.warmth(this)) {
            Prefs.setWarmth(this, it); refresh()
        }
        addSlider(root, "Затемнение", Prefs.dim(this)) {
            Prefs.setDim(this, it); refresh()
        }

        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.canDrawOverlays(this)
        permButton.visibility = if (granted) android.view.View.GONE else android.view.View.VISIBLE
        toggle.isEnabled = granted
        toggle.isChecked = granted && Prefs.enabled(this)
    }

    private fun setFilter(on: Boolean) {
        if (on && !Settings.canDrawOverlays(this)) {
            toggle.isChecked = false
            return
        }
        Prefs.setEnabled(this, on)
        if (on) {
            startForegroundService(Intent(this, OverlayService::class.java))
        } else {
            startService(
                Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_STOP)
            )
        }
    }

    private fun refresh() {
        if (Prefs.enabled(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
    }

    private fun addSlider(root: LinearLayout, label: String, value: Int, onChange: (Int) -> Unit) {
        root.addView(TextView(this).apply { text = label; textSize = 16f }, lp(top = 24))
        root.addView(SeekBar(this).apply {
            max = 100
            progress = value
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onChange(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }, lp(top = 4))
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = (top * resources.displayMetrics.density).toInt() }
}

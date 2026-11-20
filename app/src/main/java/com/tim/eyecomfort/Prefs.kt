package com.tim.eyecomfort

import android.content.Context

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("filter", Context.MODE_PRIVATE)

    fun enabled(c: Context) = p(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("enabled", v).apply()

    fun warmth(c: Context) = p(c).getInt("warmth", 40)
    fun setWarmth(c: Context, v: Int) = p(c).edit().putInt("warmth", v).apply()

    fun dim(c: Context) = p(c).getInt("dim", 20)
    fun setDim(c: Context, v: Int) = p(c).edit().putInt("dim", v).apply()
}

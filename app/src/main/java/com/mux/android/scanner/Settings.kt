package com.mux.android.scanner

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppSettings {
    private var prefs: SharedPreferences? = null

    /** false = cyan HUD, true = amber HUD */
    var amber by mutableStateOf(false)
        private set

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("scanner", Context.MODE_PRIVATE)
        prefs = p
        amber = p.getBoolean("amber", false)
    }

    fun applyAmber(v: Boolean) {
        amber = v
        prefs?.edit()?.putBoolean("amber", v)?.apply()
    }
}
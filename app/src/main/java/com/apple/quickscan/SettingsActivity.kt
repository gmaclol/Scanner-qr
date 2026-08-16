package com.apple.quickscan

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.apple.quickscan.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        const val PREFS_NAME = "quickscan_settings"
        const val KEY_SOUND = "pref_sound"
        const val KEY_VIBRATE = "pref_vibrate"
        const val KEY_AUTO_COPY = "pref_auto_copy"
        const val KEY_AUTO_OPEN = "pref_auto_open"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        binding.switchSound.isChecked = prefs.getBoolean(KEY_SOUND, true)
        binding.switchVibrate.isChecked = prefs.getBoolean(KEY_VIBRATE, true)
        binding.switchAutoCopy.isChecked = prefs.getBoolean(KEY_AUTO_COPY, true)
        binding.switchAutoOpen.isChecked = prefs.getBoolean(KEY_AUTO_OPEN, false)

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply()
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_VIBRATE, isChecked).apply()
        }

        binding.switchAutoCopy.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_COPY, isChecked).apply()
        }

        binding.switchAutoOpen.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTO_OPEN, isChecked).apply()
        }

        // Versione app
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }
        binding.tvCurrentVersion.text = "Versione $versionName • GitHub Releases"

        val updateManager = UpdateManager(this)
        binding.btnCheckUpdates.setOnClickListener {
            binding.btnCheckUpdates.isEnabled = false
            lifecycleScope.launch {
                updateManager.checkForUpdates(
                    onUpdateAvailable = { onlineVersion, downloadUrl, releaseNotes ->
                        binding.btnCheckUpdates.isEnabled = true
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this@SettingsActivity, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                            .setTitle("Aggiornamento Disponibile 🚀")
                            .setMessage("È disponibile la nuova versione $onlineVersion.\n\n$releaseNotes")
                            .setPositiveButton("Scarica & Installa") { _, _ ->
                                updateManager.downloadAndInstall(downloadUrl)
                            }
                            .setNegativeButton("Più tardi", null)
                            .show()
                    },
                    onNoUpdate = {
                        binding.btnCheckUpdates.isEnabled = true
                        android.widget.Toast.makeText(this@SettingsActivity, "Sei già all'ultima versione ($versionName) ✅", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}

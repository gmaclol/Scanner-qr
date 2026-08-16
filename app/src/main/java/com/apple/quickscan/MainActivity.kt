package com.apple.quickscan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.apple.quickscan.databinding.ActivityMainBinding
import com.apple.quickscan.databinding.BottomSheetScanResultBinding
import com.apple.quickscan.history.HistoryRepository
import com.apple.quickscan.history.ScanHistoryItem
import com.apple.quickscan.scanner.QrCodeAnalyzer
import com.apple.quickscan.utils.ClipboardHelper
import com.apple.quickscan.utils.SoundAndHapticFeedback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var soundAndHaptic: SoundAndHapticFeedback
    private lateinit var historyRepository: HistoryRepository

    private var camera: Camera? = null
    private var isTorchOn: Boolean = false
    private var qrCodeAnalyzer: QrCodeAnalyzer? = null
    private var resultBottomSheet: BottomSheetDialog? = null

    private val handler = Handler(Looper.getMainLooper())

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            binding.layoutPermissionRequired.visibility = View.GONE
            startCamera()
        } else {
            binding.layoutPermissionRequired.visibility = View.VISIBLE
        }
    }

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { scanImageFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        soundAndHaptic = SoundAndHapticFeedback(this)
        historyRepository = HistoryRepository(this)

        setupTopBar()
        setupDrawer()
        checkCameraPermissionAndStart()
        checkForUpdatesOnStartup()
    }

    private fun checkForUpdatesOnStartup() {
        lifecycleScope.launch {
            val updateManager = UpdateManager(this@MainActivity)
            updateManager.checkForUpdates(
                onUpdateAvailable = { onlineVersion, downloadUrl, releaseNotes ->
                    if (!isFinishing) {
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Aggiornamento Disponibile 🚀")
                            .setMessage("È disponibile la nuova versione $onlineVersion.\n\n$releaseNotes")
                            .setPositiveButton("Scarica & Installa") { _, _ ->
                                updateManager.downloadAndInstall(downloadUrl)
                            }
                            .setNegativeButton("Più tardi", null)
                            .show()
                    }
                }
            )
        }
    }

    private fun setupTopBar() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnTorch.setOnClickListener {
            toggleTorch()
        }

        binding.btnGallery.setOnClickListener {
            galleryPickerLauncher.launch("image/*")
        }

        binding.btnGrantPermission.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupDrawer() {
        val menu = binding.drawerMenu

        menu.navScanner.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        menu.navCreateQr.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, CreateQrActivity::class.java))
        }

        menu.navHistory.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        menu.navSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.layoutPermissionRequired.visibility = View.GONE
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            qrCodeAnalyzer = QrCodeAnalyzer { barcode ->
                runOnUiThread {
                    onBarcodeScanned(barcode)
                }
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, qrCodeAnalyzer!!)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleTorch() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            isTorchOn = !isTorchOn
            cam.cameraControl.enableTorch(isTorchOn)
            binding.btnTorch.setImageResource(if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
        } else {
            Toast.makeText(this, "Torcia non disponibile su questo dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onBarcodeScanned(barcode: Barcode) {
        val rawValue = barcode.rawValue ?: return
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val soundEnabled = prefs.getBoolean(SettingsActivity.KEY_SOUND, true)
        val hapticEnabled = prefs.getBoolean(SettingsActivity.KEY_VIBRATE, true)
        val autoCopyEnabled = prefs.getBoolean(SettingsActivity.KEY_AUTO_COPY, true)
        val autoOpenEnabled = prefs.getBoolean(SettingsActivity.KEY_AUTO_OPEN, false)

        // 1. Trigger Sound & Haptics immediately
        soundAndHaptic.triggerScanFeedback(soundEnabled, hapticEnabled)

        // 2. Auto-copy to clipboard
        if (autoCopyEnabled) {
            ClipboardHelper.copyToClipboard(this, rawValue)
            showDynamicIslandNotification(rawValue)
        }

        // 3. Save to History
        val formatName = getFormatName(barcode.format)
        historyRepository.addItem(
            ScanHistoryItem(
                content = rawValue,
                formatName = formatName,
                isGenerated = false
            )
        )

        // 4. Auto-open if URL and enabled
        if (autoOpenEnabled && (rawValue.startsWith("http://") || rawValue.startsWith("https://"))) {
            openBrowser(rawValue)
            return
        }

        // 5. Present iOS-Style Result Bottom Sheet
        showResultBottomSheet(barcode, rawValue, formatName)
    }

    private fun showDynamicIslandNotification(content: String) {
        binding.tvIslandSubtitle.text = content
        binding.layoutDynamicIsland.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = -60f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            binding.layoutDynamicIsland.animate()
                .alpha(0f)
                .translationY(-60f)
                .setDuration(250)
                .withEndAction { binding.layoutDynamicIsland.visibility = View.GONE }
                .start()
        }, 2500)
    }

    private fun showResultBottomSheet(barcode: Barcode, content: String, formatName: String) {
        resultBottomSheet?.dismiss()

        val sheetBinding = BottomSheetScanResultBinding.inflate(layoutInflater)
        resultBottomSheet = BottomSheetDialog(this, R.style.Theme_QuickScan_BottomSheetDialog)
        resultBottomSheet?.setContentView(sheetBinding.root)

        sheetBinding.tvFormatBadge.text = formatName
        sheetBinding.tvScanContent.text = content

        // Configure Primary Action
        when {
            content.startsWith("http://") || content.startsWith("https://") -> {
                sheetBinding.btnPrimaryAction.text = getString(R.string.open_link)
                sheetBinding.btnPrimaryAction.setOnClickListener {
                    openBrowser(content)
                }
            }
            content.startsWith("WIFI:") -> {
                sheetBinding.btnPrimaryAction.text = getString(R.string.connect_wifi)
                sheetBinding.btnPrimaryAction.setOnClickListener {
                    ClipboardHelper.copyToClipboard(this, content)
                    Toast.makeText(this, "Dati Wi-Fi copiati negli appunti", Toast.LENGTH_SHORT).show()
                }
            }
            content.startsWith("tel:") -> {
                sheetBinding.btnPrimaryAction.text = getString(R.string.call_number)
                sheetBinding.btnPrimaryAction.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(content)))
                }
            }
            content.startsWith("mailto:") -> {
                sheetBinding.btnPrimaryAction.text = getString(R.string.send_email)
                sheetBinding.btnPrimaryAction.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(content)))
                }
            }
            else -> {
                sheetBinding.btnPrimaryAction.text = getString(R.string.search_web)
                sheetBinding.btnPrimaryAction.setOnClickListener {
                    val searchUrl = "https://www.google.com/search?q=" + Uri.encode(content)
                    openBrowser(searchUrl)
                }
            }
        }

        sheetBinding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
        }

        sheetBinding.btnCopyAgain.setOnClickListener {
            ClipboardHelper.copyToClipboard(this, content)
            Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }

        sheetBinding.btnRescan.setOnClickListener {
            resultBottomSheet?.dismiss()
        }

        resultBottomSheet?.setOnDismissListener {
            qrCodeAnalyzer?.isScanningEnabled = true
        }

        resultBottomSheet?.show()
    }

    private fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Impossibile aprire il link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onBarcodeScanned(barcodes.first())
                    } else {
                        Toast.makeText(this, "Nessun codice trovato nell'immagine", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nella lettura dell'immagine", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore caricamento immagine", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_EAN_13 -> "Codice EAN-13"
            Barcode.FORMAT_EAN_8 -> "Codice EAN-8"
            Barcode.FORMAT_UPC_A -> "Codice UPC-A"
            Barcode.FORMAT_UPC_E -> "Codice UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "Codice a Barre"
        }
    }

    override fun onResume() {
        super.onResume()
        qrCodeAnalyzer?.isScanningEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        soundAndHaptic.release()
    }
}
